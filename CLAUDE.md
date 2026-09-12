# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project overview

`booking_app_be` is the Spring Boot 3.5 / Java 21 backend for a hotel booking platform (graduation
thesis project, repo `VanMo2003/booking_app`). It exposes a REST API under `/booking-app/api/v1`,
backed by MySQL, with JWT authentication and VNPay payment integration.

## Commands

Build/run (Windows: use `mvnw.cmd`, Unix: `./mvnw`):

```bash
./mvnw clean package -DskipTests   # build jar
./mvnw spring-boot:run             # run locally (needs local MySQL, see below)
./mvnw test                        # run all tests
./mvnw test -Dtest=BookingAppBeApplicationTests   # run a single test class
```

Spotless (Palantir Java formatter, tabs=4 spaces-equivalent) runs automatically on `mvn compile`
via the `apply` goal bound to the `compile` phase — there is no separate lint step to remember, but
if formatting drifts, `./mvnw spotless:apply` fixes it directly.

Local run needs a MySQL instance matching `application.yaml` defaults (`localhost:3306/booking_app_db`,
override via `SPRING_DATASOURCE_*` env vars). Full stack (app + MySQL) via Docker:

```bash
docker compose up -d --build      # or ./deploy.sh
docker compose logs -f booking-app
```

Copy `.env.example` to `.env` to override compose defaults (DB creds, JWT signer key, VNPay sandbox
keys). See [DEPLOY_EC2.md](DEPLOY_EC2.md) for the EC2 deployment sequence.

## Architecture

Standard layered structure, one package per concern under `com.vanmo.booking_app_be`:

- `controller` — thin `@RestController`s; every response is wrapped in `ApiResponse<T>`
  (`success`/`message`/`code`/`data`, see `dto/common/ApiResponse.java`). `ApiResponse.ok(...)` on
  success; failures normally reach `GlobalExceptionHandler` rather than being built by hand.
- `service` (interface) + `service/impl` (implementation) — one interface/impl pair per domain
  entity (e.g. `HotelService`/`HotelServiceImpl`). Controllers depend on the interface.
- `repository` — Spring Data JPA repositories, one per entity.
- `entity` — JPA entities; enums for status/role fields live in `entity/enums` (`Role`,
  `BookingStatus`, `PaymentStatus`, `RoomStatus`, `HotelStatus`, `PayrollStatus`, `PaymentMethod`).
- `dto/request`, `dto/response` — request/response DTOs, one class per operation (`XCreateRequest`,
  `XUpdateRequest`, `XResponse`, `ListXResponse`) rather than shared/generic DTOs.
- `mapper` — MapStruct interfaces (`@Mapper(componentModel = "spring")`) converting between
  entities and DTOs; injected into services like any other Spring bean. Associations that need
  extra query/service logic are marked `@Mapping(target = "...", ignore = true)` and populated
  manually in the service layer.
- `config` — security (`SecurityConfig`, `CustomJwtDecoder`, `JwtAuthenticationEntryPoint`),
  CORS/web (`CorsConfig`, `WebConfig`), VNPay (`VNPayConfig`), and `ApplicationInitConfig`, which
  seeds a default `admin123`/`admin123` ADMIN account on startup if none exists.
- `security` — `AccessGuard`, the one place hierarchy/ownership authorization checks live (see
  below); called from the service layer, not controllers.
- `exception` — `AppException` + `ErrorCode` enum (numeric app-level codes, distinct from HTTP
  status) are the standard way to fail a request; `GlobalExceptionHandler` maps these (and
  validation/upload-size/access-denied errors) to `ApiResponse.fail(...)`.
- `scheduler` — `PaymentExpireScheduler` polls every 60s to expire stale pending-payment bookings.

### Multi-tenant hierarchy: chain → branch → staff

Three levels, each tied to an `Account` by role:

1. **`HotelChain`** ("khách sạn") — one `HOTEL_OWNER` account owns exactly one chain
   (`HotelChain.account`, `@OneToOne`).
2. **`Hotel`** ("cơ sở"/branch — the entity keeps its original name; it is NOT the chain) —
   `@ManyToOne` to `HotelChain`, plus `Hotel.account` is the branch's `HOTEL_MANAGER`
   (`@ManyToOne`, not unique — one manager can run several `Hotel` rows). `Room`, `Amenity`,
   `Service`, `HotelImage`, `Employee`, and `Booking` all hang off `Hotel` exactly as before this
   hierarchy was added — none of those FK names or the `/hotels`, `/rooms?hotelId=` etc. REST
   contracts changed.
3. **`Employee`** (`STAFF`) — already scoped to exactly one `Hotel` via `Employee.hotel`; created
   exclusively through `POST /employees` (which internally creates the backing `STAFF` `Account`),
   never directly via `POST /accounts`.

`ADMIN` is unrestricted. `CUSTOMER` is unaffected by this hierarchy (global accounts, can book at
any branch).

### Auth & authorization

JWT-based, HMAC-SHA512, via `spring-boot-starter-oauth2-resource-server` configured as a resource
server with a custom `NimbusJwtDecoder` (`SecurityConfig.jwtDecoder`). Roles are the five
`Role` enum values (`ADMIN`, `HOTEL_OWNER`, `HOTEL_MANAGER`, `STAFF`, `CUSTOMER`), carried as JWT
authorities with no prefix (`ROLE_HOTEL_MANAGER`, etc.) and JWT `sub` = username. Only a small
allowlist in `SecurityConfig` (`PUBLIC_ENDPOINT_GET`, `PUBLIC_ENDPOINT_POST` — login/introspect/
refresh/account-create/file endpoints) is open; every other endpoint requires authentication.
`/uploads/**` is excluded from the security filter chain entirely (served as static files, see
`file.upload-dir` in `application.yaml`).

Coarse "who is even authenticated" gating stops at `SecurityConfig`; everything ownership-shaped
(does this manager actually run this branch, does this staff member belong to this branch, does
this owner's chain contain this branch) is enforced by `security/AccessGuard.java`, called at the
top of the relevant `service/impl/*` methods — `requireChainOwner`, `requireBranchManagerOrAbove`,
`requireBranchStaffOrAbove`, `requireSelfOrAdmin`, `requireAdmin`. It resolves the caller via
`SecurityContextHolder` + `AccountRepository.findByUsername`, and every rejection throws
`AppException(ErrorCode.UN_AUTHORIZED)` (403). A few `@PreAuthorize("!hasAuthority('ROLE_STAFF')")`
annotations on `@DeleteMapping` methods (Room/Amenity/Service/Booking) exist as defense-in-depth
alongside the service-layer check, not instead of it — `GlobalExceptionHandler` maps the resulting
`AccessDeniedException` to 403 too. `POST /accounts` stays public for `role=CUSTOMER` self-signup;
every other role is gated inside `AccountServiceImpl.create()`'s role-creation matrix (e.g. only
`ADMIN` can mint `HOTEL_OWNER` accounts, only `ADMIN`/`HOTEL_OWNER` can mint `HOTEL_MANAGER`
accounts, `STAFF` is rejected there outright and must go through `/employees`).

`BookingServiceImpl` is the one place `AccessGuard` isn't the whole story: bookings are created
and read by both staff (scoped to their branch) and customers (self-service, any branch), so its
mutation/read methods check `Role.CUSTOMER` first (must be booking for their own `Customer` record)
and fall back to `accessGuard.requireBranchStaffOrAbove(...)` otherwise — see
`BookingServiceImpl.requireBookingAccess`.

### Payments

VNPay sandbox integration (`VNPayConfig`, `VNPayUtil`, `PaymentServiceImpl`,
`PaymentController#vn-pay-callback`). Bookings carry a `PaymentStatus`; the scheduler above expires
bookings left in a pending-payment state past their window.

### Multipart / file uploads

Endpoints that accept images (e.g. `HotelController#create`, `#uploadImages`) use
`consumes = MULTIPART_FORM_DATA_VALUE` with the JSON payload as a `@RequestPart("data")` alongside
a `@RequestPart("files") List<MultipartFile>`. `FileStorageService` writes to `file.upload-dir`
(`uploads/`, mounted as a Docker volume in `docker-compose.yml`).

### Database

MySQL 8.4; `spring.jpa.hibernate.ddl-auto=update` by default (no migration tool — schema evolves
via entity changes on boot). `src/main/resources/static/booking_db.sql` is a reference dump of the
schema/seed data, not an executed migration.

### A recurring bug shape to watch for

Several `*ServiceImpl.create()` methods historically called `mapper.toEntity(req)` and saved
directly, trusting MapStruct to turn a `*Id` field (e.g. `hotelId`) on the request DTO into the
matching `@ManyToOne` field (e.g. `hotel`) on the entity — MapStruct doesn't do that automatically
(different property names, `Long` → entity isn't an auto-conversion), so the association silently
stayed `null` and saving failed a NOT NULL/FK constraint (or, for nullable FKs, just silently lost
the association). Fixed instances: `Hotel.account`/`.hotelChain`, `Payroll.employee`,
`HotelImage.hotel`, `RoomImage.room`, `Feedback.booking`/`.customer`, `Report.hotel` — each now
resolves the referenced entity from its repository and sets it manually before save. If you add a
new `create()` that takes a `*Id` field, check whether the mapper actually wires it up (look for an
explicit `@Mapping(source=..., target=...)` or manual `entity.setX(...)` in the service) rather than
assuming it does.

### Walk-in guests

`Customer.account` is **optional**. A guest who books at the front desk gets a profile with just
name/phone and no login account (`POST /customers` with no `accountId`, staff-or-above only via
`AccessGuard.requireStaffOrAbove()`); staff then books for them with the returned `customerId`
through the normal `POST /bookings`. `Booking.handledBy` records the staff/manager account that took
the booking (null for customer self-service) and is what the staff-performance report groups by.
If that guest later self-registers, `POST /customers/link-by-phone` attaches their new account to
the *same* `Customer` row (matched on phone, unlinked rows only) instead of creating a duplicate;
`GET /customers/search?phoneNumber=` is the front-desk lookup that stops duplicates in the first
place.

### Reports

Two families, both in `ReportController`/`ReportServiceImpl`, each with a branch-level
(`requireBranchManagerOrAbove`) and a chain-level `/reports/chain/...` (`requireChainOwner`) variant:
revenue (by date/month/year/week, pre-existing) and the detailed set — `occupancy`,
`occupancy/daily`, `revenue-by-room-type`, `revenue-by-service`, `top-customers`,
`staff-performance`, `cancellation-rate`, plus `export` returning a multi-sheet `.xlsx`
(`ExcelReportServiceImpl`, Apache POI). Two things worth knowing about the numbers:

- **Room revenue is derived, not stored.** `BookingRoom` has no price column and `Booking.totalAmount`
  is a single client-supplied figure, so revenue-by-room-type computes `room.price × nights` per
  booked room. Service revenue *is* stored (`BookingService.total`, set from
  `service.unitPrice × quantity` at booking time).
- **Occupancy and room-night maths happen in Java**, not SQL (`BookedRoomNightRow` +
  `ReportServiceImpl.nightsWithin`), to avoid DB-specific date arithmetic. `occupancy/daily` is
  capped at 92 days (`MAX_DAILY_RANGE`).

### Public browsing

`SecurityConfig.PUBLIC_ENDPOINT_GET` allowlists the anonymous browse surface (`/hotels`,
`/hotels/detail/{id}`, `/hotels/category`, `/hotels/search`, `/rooms`, `/rooms/{id}`,
`/rooms/available`, `/amenities/hotel/{hotelId}`, `/amenities/room`, `/services`) so a visitor can
browse without logging in; everything that writes or is account-scoped still needs a token. The
matching service methods intentionally carry no `AccessGuard` call — if you add a new browse
endpoint, it needs both halves (no guard *and* an allowlist entry) or it will 401 anonymously.

### Testing the whole API

`postman/booking_app.postman_collection.json` is the authoritative end-to-end suite (~166 requests,
10 folders) and runs top-to-bottom in Postman's Collection Runner, chaining tokens/ids through
collection variables and finishing with a cleanup folder so it can be re-run back-to-back. It covers
every controller plus the authorization-negative cases (staff can't delete, cross-tenant reads
rejected, customer can't book for someone else). Keep it in sync when adding endpoints.
