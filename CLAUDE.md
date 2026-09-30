# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project overview

`booking_app_be` is the Spring Boot 3.5 / Java 21 backend for a hotel booking platform (graduation
thesis project, repo `VanMo2003/booking_app`). It exposes a REST API under `/booking-app/api/v1`,
backed by MySQL, with JWT authentication, VNPay payment integration, email (Spring Mail) and push
notifications (Firebase Cloud Messaging).

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
keys, SMTP, Firebase). See [DEPLOY_EC2.md](DEPLOY_EC2.md) for the EC2 deployment sequence.

## Architecture

Standard layered structure, one package per concern under `com.vanmo.booking_app_be`:

- `controller` — thin `@RestController`s; every response is wrapped in `ApiResponse<T>`
  (`success`/`message`/`code`/`data`, see `dto/common/ApiResponse.java`). `ApiResponse.ok(...)` on
  success; failures normally reach `GlobalExceptionHandler` rather than being built by hand. The one
  exception is `AppLinkController`, which returns a plain HTML page.
- `service` (interface) + `service/impl` (implementation) — one interface/impl pair per domain
  entity (e.g. `HotelService`/`HotelServiceImpl`). Controllers depend on the interface.
- `repository` — Spring Data JPA repositories, one per entity.
- `entity` — JPA entities; enums for status/role fields live in `entity/enums` (`Role`,
  `BookingStatus`, `PaymentStatus`, `RoomStatus`, `HotelStatus`, `PayrollStatus`, `PaymentMethod`,
  `ApprovalStatus`, `NotificationType`, `DishCategory`, `ChatSide`).
- `dto/request`, `dto/response` — request/response DTOs, one class per operation (`XCreateRequest`,
  `XUpdateRequest`, `XResponse`, `ListXResponse`) rather than shared/generic DTOs.
- `mapper` — MapStruct interfaces (`@Mapper(componentModel = "spring")`) converting between
  entities and DTOs; injected into services like any other Spring bean. Associations that need
  extra query/service logic are marked `@Mapping(target = "...", ignore = true)` and populated
  manually in the service layer.
- `config` — security (`SecurityConfig`, `CustomJwtDecoder`, `JwtAuthenticationEntryPoint`),
  CORS/web (`CorsConfig`, `WebConfig`), VNPay (`VNPayConfig`), and `ApplicationInitConfig`, which
  seeds a default `admin123`/`admin123` ADMIN account on startup if none exists and marks
  pre-approval hotel chains as `APPROVED`.
- `security` — `AccessGuard`, the one place hierarchy/ownership authorization checks live (see
  below); called from the service layer, not controllers.
- `event` — Spring application events and their after-commit listeners (push, review email, chat
  fan-out); see *Notifications, push and email* and *Guest ↔ hotel chat*.
- `websocket` — the chat socket (`ChatWebSocketHandler`, `ChatHandshakeInterceptor`,
  `ChatSessionRegistry`), registered by `config/WebSocketConfig`.
- `ai` — model calls (Gemini by default, Claude or any OpenAI-compatible server): chat receptionist and
  natural-language search; see *AI features*.
- `exception` — `AppException` + `ErrorCode` enum (numeric app-level codes, distinct from HTTP
  status) are the standard way to fail a request; `GlobalExceptionHandler` maps these (and
  validation/upload-size/access-denied errors) to `ApiResponse.fail(...)`.
- `scheduler` — `PaymentExpireScheduler` polls every 60s to expire stale pending-payment bookings.
- `utils` — `VNPayUtil`, `AppLinks` (app deep links), `NotificationMessages` (notification copy).

### Multi-tenant hierarchy: chain → branch → staff

Three levels, each tied to an `Account` by role:

1. **`HotelChain`** ("khách sạn") — one `HOTEL_OWNER` account owns exactly one chain
   (`HotelChain.account`, `@OneToOne`). The chain doubles as the owner's registration: it carries the
   contact details and the admin's review state (see *Owner registration and admin review*).
2. **`Hotel`** ("cơ sở"/branch — the entity keeps its original name; it is NOT the chain) —
   `@ManyToOne` to `HotelChain`, plus `Hotel.account` is the branch's `HOTEL_MANAGER`
   (`@ManyToOne`, not unique — one manager can run several `Hotel` rows). `Room`, `Amenity`,
   `Service`, `HotelImage`, `Employee`, and `Booking` all hang off `Hotel` exactly as before this
   hierarchy was added — none of those FK names or the `/hotels`, `/rooms?hotelId=` etc. REST
   contracts changed. `Dish` (the branch's food menu, see *Food menu*) hangs off `Hotel` the same
   way.
3. **`Employee`** (`STAFF`) — already scoped to exactly one `Hotel` via `Employee.hotel`; created
   exclusively through `POST /employees` (which internally creates the backing `STAFF` `Account`),
   never directly via `POST /accounts`.

`ADMIN` can read everything but is review-only (see *Admin is review-only*).

### Food menu

`Dish` (`dishes` table: name, description, price, `DishCategory` enum, `pathImage`, `available`)
belongs to one `Hotel` branch and is removed with it (`Hotel.dishes`, cascade + orphanRemoval).
`DishController` (`/dishes`) reads are public (`GET /dishes?hotelId=`, `GET /dishes/{id}` — both
in `PUBLIC_ENDPOINT_GET`); create/update/delete/image-upload go through
`AccessGuard.requireBranchManagerEditor`, so only the branch's `HOTEL_OWNER`/`HOTEL_MANAGER` write
it — `STAFF` and `ADMIN` are read-only, unlike rooms/amenities/services where staff may edit
(`@PreAuthorize` on the write methods repeats that). `PUT /dishes/{id}` is a partial update
(`DishUpdateRequest` fields are all optional, mapper uses `NullValuePropertyMappingStrategy.IGNORE`),
so `{"available": false}` alone toggles "sold out"; a blank `pathImage` clears the photo.
`POST /dishes/{id}/image` takes a single multipart part named `file`. `DishServiceImpl` sorts the
list by `DishCategory` declaration order (the menu's reading order), then name — keep new enum
values in that order. Dishes are menu-only: nothing links them to bookings or billing. `CUSTOMER` is
unaffected by this hierarchy (global accounts, can book at any branch).

### Auth & authorization

JWT-based, HMAC-SHA512, via `spring-boot-starter-oauth2-resource-server` configured as a resource
server with a custom `NimbusJwtDecoder` (`SecurityConfig.jwtDecoder`). Roles are the five
`Role` enum values (`ADMIN`, `HOTEL_OWNER`, `HOTEL_MANAGER`, `STAFF`, `CUSTOMER`), carried as JWT
authorities with no prefix (`ROLE_HOTEL_MANAGER`, etc.) and JWT `sub` = username. Only a small
allowlist in `SecurityConfig` (`PUBLIC_ENDPOINT_GET`, `PUBLIC_ENDPOINT_POST` — login/introspect/
refresh/account-create/owner-registration/file endpoints, the browse surface, `/app-links/{target}`,
plus `/ws/chat`, which authenticates its own upgrade, and `GET /ai/status` / `POST /ai/search`) is open;
every other endpoint requires authentication. `/uploads/**` is excluded from the security
filter chain entirely (served as static files, see `file.upload-dir` in `application.yaml`).

Coarse "who is even authenticated" gating stops at `SecurityConfig`; everything ownership-shaped
(does this manager actually run this branch, does this staff member belong to this branch, does
this owner's chain contain this branch, has this owner been approved) is enforced by
`security/AccessGuard.java`, called at the top of the relevant `service/impl/*` methods —
`requireChainOwner`, `requireOwnChain`, `requireApprovedOwner`, `requireBranchManagerOrAbove`,
`requireBranchStaffOrAbove`, `requireBranchEditor`, `requireBranchManagerEditor`,
`requireBranchTeam` (chat: owner/manager/staff of the branch, never admin), `requireSelfOrAdmin`,
`requireAdmin`. It resolves the caller via `SecurityContextHolder` +
`AccountRepository.findByUsername`, and rejections throw `AppException(ErrorCode.UN_AUTHORIZED)`
(403) — or the more specific `OWNER_NOT_APPROVED` / `ADMIN_READ_ONLY` (also 403). A few
`@PreAuthorize("!hasAuthority('ROLE_STAFF')")` annotations on `@DeleteMapping` methods
(Room/Amenity/Service/Booking) exist as defense-in-depth alongside the service-layer check, not
instead of it — `GlobalExceptionHandler` maps the resulting `AccessDeniedException` to 403 too.
`POST /accounts` stays public for `role=CUSTOMER` self-signup; every other role is gated inside
`AccountServiceImpl.create()`'s role-creation matrix: `HOTEL_OWNER` is rejected there (owners
self-register through `POST /owner-registrations`), `ADMIN` can't be created at all (only the seeded
one exists), `HOTEL_MANAGER` needs an *approved* owner, and `STAFF` must go through `/employees`.

`BookingServiceImpl` is the one place `AccessGuard` isn't the whole story: bookings are created
and read by both staff (scoped to their branch) and customers (self-service, any branch), so its
mutation/read methods check `Role.CUSTOMER` first (must be booking for their own `Customer` record)
and fall back to `accessGuard.requireBranchStaffOrAbove(...)` otherwise — see
`BookingServiceImpl.requireBookingAccess`.

### Owner registration and admin review

Hotel owners sign themselves up: public `POST /owner-registrations` creates the `HOTEL_OWNER`
account and its `HotelChain` in one transaction, with the contact details (`ownerName`, `email`,
`phone`, `address`) and `approvalStatus = PENDING`. The review state lives on `HotelChain`
(`approvalStatus`, `rejectionReason`, `submittedAt`, `reviewedAt`) because the chain *is* the hotel
being reviewed, and the login response already carries `hotelChain`, so the app knows the state
without an extra call.

- A pending or rejected owner can log in, read `GET /owner-registrations/me` and resubmit with
  `PUT /owner-registrations/me` (both guarded by `AccessGuard.requireOwnChain()`), nothing else:
  `requireChainOwner` and `requireApprovedOwner` throw `OWNER_NOT_APPROVED` (1011) until an admin
  approves, which blocks branches, managers, reports and chain edits.
- Admins work the queue with `GET /owner-registrations?status=PENDING` (oldest first),
  `GET /owner-registrations/{id}`, `PUT /owner-registrations/{id}/approve` and
  `PUT /owner-registrations/{id}/reject` (`reason` required). Only `PENDING` rows can be decided
  (`REGISTRATION_NOT_PENDING`, 1013). A resubmission keeps the previous `rejectionReason` as
  history; editing a still-pending submission doesn't notify admins again.
- `POST /hotel-chains` (an owner without a chain, e.g. after deleting it) also creates a `PENDING`
  chain, and only the owner account itself may call it.
- Chains that existed before this flow are set to `APPROVED` on startup
  (`HotelChainRepository.approveLegacyChains`, called from `ApplicationInitConfig`);
  `HotelChain.isApproved()` treats a null status as approved as well.

### Admin is review-only

`ADMIN` still reads everything, but no longer creates or edits branch content or accounts: rooms,
room images, amenities (and room-amenity links), services and staff (`POST /employees` mints a
login) go through `AccessGuard.requireBranchEditor` / `requireBranchManagerEditor`, which reject
`ADMIN` with `ADMIN_READ_ONLY` (1012). In `AccountServiceImpl`, an admin can only lock/unlock an
account (`status`); role changes are rejected for everyone, password changes are allowed only for
the account holder, and `GET /accounts` / `DELETE /accounts/{id}` are admin-only (delete also removes
the account's notifications and device tokens). The catalog (`/roomTypes`, `/positions`) is still
managed by admins.

### Notifications, push and email

- `Notification` rows are the in-app inbox (`GET /notifications`, `GET /notifications/unread-count`,
  `PUT /notifications/{id}/read`, `PUT /notifications/read-all`). `NotificationService.notify` /
  `notifyAdmins` store them and publish a `PushRequestedEvent`; the wording lives in
  `utils/NotificationMessages` and the deep links in `utils/AppLinks`
  (`bookingapp://owner-status`, `bookingapp://owner-registrations/{id}`).
- Push goes out through the FCM HTTP v1 API (`FcmPushNotificationSender`, service-account auth via
  `google-auth-library-oauth2-http`) to the account's `DeviceToken`s, which the app registers with
  `POST /notifications/devices` and removes with `DELETE /notifications/devices?token=` before
  logout. Without `FIREBASE_CREDENTIALS` the sender is disabled and only the inbox works; tokens FCM
  reports as unregistered are deleted.
- Approve/reject publishes `OwnerReviewedEvent`; `OwnerReviewMailListener` sends the HTML result
  email through `MailServiceImpl` (Spring Mail, `MAIL_*` variables). Without `MAIL_USERNAME` the
  mail is only logged.
- Both listeners are `@Async @TransactionalEventListener(AFTER_COMMIT)` (`@EnableAsync` sits on the
  application class), so a rolled-back review never notifies anyone and SMTP/FCM latency never slows
  the request down.
- Mail clients strip custom-scheme links, so the email button points at the public
  `GET /app-links/{target}` page (built from `APP_PUBLIC_URL`), which forwards the phone to
  `bookingapp://{target}` (via `intent://` on Android Chrome). Only allowlisted targets are served.

To test mail locally, point `MAIL_HOST`/`MAIL_PORT` at a throwaway SMTP sink with
`MAIL_SMTP_AUTH=false`, `MAIL_SMTP_STARTTLS=false` and any `MAIL_USERNAME`. The first message after
startup can take ~10 s to leave (JavaMail resolves the local host name once).

### Guest ↔ hotel chat

`Conversation` (one per customer × branch, unique) and `ChatMessage`. The branch side is a shared
inbox: the chain owner, the branch manager and every `Employee` of that branch read and answer the
same thread, so unread state is two counters on the conversation (`customerUnread`,
`hotelUnread`), not per person. Access lives in `ChatServiceImpl.sideOf` — a `CUSTOMER` must own
the conversation's `Customer`; everyone else goes through `AccessGuard.requireBranchTeam`, which
refuses `ADMIN` too (chats are private; admins aren't "read everything" here). Only customers with a
`Customer` profile can open a conversation (`CUSTOMER_PROFILE_REQUIRED`, 1014).

- Sending, history and inboxes are REST (`ChatController`, `/conversations`); messages come back newest
  first and page with `beforeId`. The WebSocket at `/ws/chat` is server → client only: plain
  `TextWebSocketHandler`, no STOMP. `ChatHandshakeInterceptor` authenticates the upgrade with the same
  access token (`?token=` since browsers can't set headers, or `Authorization`), via
  `CustomJwtDecoder` so logged-out tokens fail; `/ws/chat` is `permitAll` in `SecurityConfig` for that
  reason.
- Every change publishes `ChatUpdatedEvent` (built inside the transaction with per-side views of the
  conversation, so the listener never touches lazy entities). `ChatEventListener` runs after commit,
  synchronously so messages hit sockets in order, sends the event to every open socket of both sides
  and pushes (FCM, `bookingapp://chat/{id}`, Android tag per conversation) only to the other side's
  accounts with no open socket. It calls `PushNotificationListener.onPushRequested` directly: an event
  published from an after-commit callback would never fire.
- `ChatSessionRegistry` keeps sockets in memory (wrapped in `ConcurrentWebSocketSessionDecorator`) —
  single-instance only.
- Deleting a branch or a customer removes their conversations and messages through DB-level
  `@OnDelete(CASCADE)` foreign keys. Message senders are a snapshot (id/name/role, no FK) so deleting
  a staff account never blocks on chat history.

### AI features (`ai` package)

Two features, both grounded in database data rather than model knowledge:

- **Chat receptionist** (`ChatAssistant`): the system prompt carries `HotelKnowledge.factSheet(hotelId)`
  (room types/prices, amenities, services, menu - public browse data only, never bookings or customers)
  and the model gets two tools: `find_available_rooms` (a real availability lookup, same rule as the
  hotel detail page) and `handoff_to_staff` (sets `Conversation.needsStaff` + `handoffReason`, shown
  only on the hotel side, cleared when a person on the team replies). Guest text goes inside
  `<conversation>` tags and the rules tell the model it is data, not instructions. Two entry points:
  `POST /ai/conversations/{id}/suggest-reply` (team member gets a draft, nothing is sent) and
  auto-reply - when `Hotel.aiAutoReply` is on, `ChatEventListener` calls
  `ChatAssistant.onCustomerMessage` for every guest message; after `ai.auto-reply-delay-seconds` it
  answers only if that message is still the newest (staff didn't answer, guest didn't write again),
  via `ChatService.postAssistantReply` (sender = "Trợ lý AI", `aiGenerated`, no account, hotel
  unread left as is). Delays run on a private scheduled executor, model calls on virtual threads.
- **Natural-language search** (`AiSearchService`, public `POST /ai/search`): the fast model only turns
  the sentence into filters through a forced tool call (`set_search_filters`); matching branches and
  free rooms is plain Java. Unusable filters are dropped with a note instead of failing.

`AiClient` is the single outbound call. Services always speak the Anthropic Messages shape
(system blocks, `tool_use`/`tool_result`); for `AI_PROVIDER=gemini` (auto-selected when only
`GEMINI_API_KEY` is set - the free default) or `openai` (Groq, OpenRouter, Ollama via `AI_BASE_URL`),
`OpenAiCompat` translates to/from Chat Completions. It carries the model's tool-calling assistant turn
through verbatim in a private `_openai_raw` block because Gemini 3 rejects a follow-up turn without
its thought signatures. No key → `isEnabled()` false, `GET /ai/status` says so, endpoints throw
`AI_UNAVAILABLE` (1015, 503). `AiRateLimiter` caps calls per account/IP in memory (`AI_RATE_LIMITED`,
1016, 429). To test without a key, point `AI_BASE_URL` at a local stub that serves `/chat/completions`
(or `/v1/messages` for anthropic); there is no real-model test in the repo.

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
`/rooms/available`, `/amenities/hotel/{hotelId}`, `/amenities/room`, `/services`, `/dishes`,
`/dishes/{id}`) so a visitor can
browse without logging in; everything that writes or is account-scoped still needs a token. The
matching service methods intentionally carry no `AccessGuard` call — if you add a new browse
endpoint, it needs both halves (no guard *and* an allowlist entry) or it will 401 anonymously.

### Testing the whole API

`postman/booking_app.postman_collection.json` is the authoritative end-to-end suite (10 folders)
and runs top-to-bottom in Postman's Collection Runner, chaining tokens/ids through collection
variables and finishing with a cleanup folder so it can be re-run back-to-back. It covers every
controller plus the authorization-negative cases (staff can't delete, cross-tenant reads rejected,
customer can't book for someone else, pending owners locked out, admin is review-only) and the
owner registration → reject → resubmit → approve flow, and the chat REST flow (customer ↔ staff,
cross-tenant and admin refusals). The `/ws/chat` socket can't run in the Collection Runner — test it
with a Postman WebSocket request or a script. Keep it in sync when adding endpoints.
