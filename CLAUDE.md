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
  validation/upload-size/access-denied errors) to `ApiResponse.fail(...)`. It has a catch-all
  `Exception` handler returning 500, so a caller's own mistake needs its own handler to come back as
  400: wrong parameter type (`MethodArgumentTypeMismatchException`), a missing required query
  parameter (`MissingServletRequestParameterException`) and an unreadable body
  (`HttpMessageNotReadableException`) are mapped explicitly - add one when a new endpoint can fail that
  way (`AiControllerTest` pins the AI endpoints' HTTP behaviour).
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
   contracts changed. `Dish` (the branch's food menu, see *Food menu*) and `Tour` (see *Tours*)
   hang off `Hotel` the same way.
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

### Tours

`Tour` (`tours` table: name, description, `price` per guest, free-text `duration`/`departure`/
`includes`, nullable `maxGuests` = no limit, `pathImage`, `available`) follows the *Food menu* pattern
exactly: `Hotel.tours` cascade + orphanRemoval, `TourController` (`/tours`) public reads in
`PUBLIC_ENDPOINT_GET`, writes through `AccessGuard.requireBranchManagerEditor` + `@PreAuthorize`
(owner/manager only), partial `PUT`, single-part `file` image upload. `TourUpdateRequest` has two
"clear" conventions the mapper can't express with IGNORE: `maxGuests: 0` → null (no limit) and a
blank string clears a text field or the photo (`TourServiceImpl.blankToNull`). The list sorts
available tours first, then price, then name.

Tours also carry what guests browse by: `destination` (free text, what the public search matches), `themes`
(`TravelTheme`, element collection `tour_themes`), an ordered `itinerary` (`ItineraryStep` embeddables in
`tour_itinerary`), `childPrice` (null = adult price), `cancellationPolicy`, `featured`, and a promotion
(`discountPercent` 1-90 until `promotionEndsAt`; 0 switches it off). `TourPricing` is the one place a day's
price is worked out (Vietnam date); `TourResponse` carries `onSale`/`salePrice`/`saleChildPrice`. Public
`GET /tours/search` (q over name/destination/branch address - MySQL's collation ignores accents -, theme,
maxPrice, paged), `/tours/destinations` and `/tours/home?interests=` (`TourDiscovery`: featured, deals,
forYou) only list available tours of active branches.

A tour is sold as a **tour + stay package**: `TourRoom` (`tour_rooms`, unique tour/room, both FKs
`ON DELETE CASCADE`) lists the branch rooms the package uses and `Tour.stayNights` (column default 1) the
nights from the tour date. `TourServiceImpl.setRooms` writes only the difference (re-inserting a pair
removed in the same flush would hit the unique key - Hibernate inserts before deletes). Public
`GET /tours/{id}/rooms/available?date=` (`TourBookingService.availableRooms`) applies the hotel-page rule:
not MAINTENANCE and not in an overlapping PENDING/CONFIRMED booking.

**Orders, vouchers, interests, guest pushes.** An order has `guests` (total) and `children` (≥ 1 adult);
`unitPrice`/`childUnitPrice` are `TourPricing`'s on the booking day. `voucherCode` goes through
`VoucherService.reserve` (`VoucherRules`: dates, chain/branch, usage limit, once per guest by default, min
order) and its discount comes off `tourAmount`, so revenue reports stay net; cancelling releases the use.
`Voucher` (`vouchers`, unique code, chain-wide or one branch) is written by the chain owner or that branch's
manager; `GET /vouchers/available` is public, `POST /vouchers/check` previews a discount. A new running voucher
pushes `PROMOTION` to guests who ordered at the chain; a tour going on sale pushes it to guests whose
`Customer.interests` (`/customers/me/interests`) share a theme. `TripReminderScheduler` (18:00 Vietnam) sends
`TRIP_REMINDER` for tomorrow's tours and check-ins; `BookingServiceImpl` sends `BOOKING_UPDATED` when the
branch confirms or cancels a guest's room booking. Deep links: `AppLinks.tour`, `AppLinks.booking`.

`TourBooking` (`tour_bookings`) is a guest's tour order, paid at the hotel: it copies `tourName` and
`unitPrice` at booking time and keeps `tour_id` with `ON DELETE SET NULL` (also `conversation_id`,
`handled_by_account_id`, `booking_id`); hotel and customer cascade. For a tour with rooms the guest must
pick package rooms that are free for the whole stay and sleep everyone; `place` then creates a normal
`Booking` (`bookRooms`: CASH/UNPAID, note "Phòng kèm đơn tour #id") so availability, the desk and reports
see it, stores `tourAmount`/`roomAmount`/`stayCheckout`, and confirm/cancel of the tour order push the
same status to that booking. `BookingServiceImpl.cancelBooking` refuses a booking that belongs to an open
tour order (1020) - cancel the tour order instead. `TourBookingServiceImpl.place` is the one place
an order is created - `POST /tour-bookings` (customer for themselves) and the assistant's
`placeFromChat` both go through it: tour available, date today..+365 days (Vietnam time), 1-50 guests,
`Tour.maxGuests` is seats **per departure** summed over open (PENDING/CONFIRMED) orders that day, and the
same guest/tour/day returns the open order instead of a duplicate (different head count → error). Errors
are `TOUR_BOOKING_INVALID` (1020) with a Vietnamese message, because the same text is fed back to the
model. Confirm/complete/cancel: branch team via `requireBranchEditor` (admin read-only); the guest may
cancel their own open order; a hotel-side cancel needs a reason. Notifications go through
`NotificationService.notifyAccounts(accessGuard.branchTeam(hotel), …)` - owner, manager and staff,
the same list the chat fan-out uses - or to the guest's account, with deep link
`AppLinks.tourBooking(id)`.

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
and fall back to a branch check otherwise: `requireBookingAccess` (`requireBranchStaffOrAbove`) for
reads, `requireBookingEditor` (`requireBranchEditor`, admin refused) for every write - create,
update, confirm/cancel/complete, payment status/method, the VNPay callback's writes - and
`requireBranchManagerEditor` for delete. Use the editor variant for any new booking write.

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
`ADMIN` with `ADMIN_READ_ONLY` (1012). Bookings and their `/bookingRooms` / `/bookingServices` lines
are the same: admins read them but can't create, edit, confirm, cancel, complete, pay or delete
them. In `AccountServiceImpl`, an admin can only lock/unlock an
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
  reports as unregistered are deleted. Every message carries a `notification` block (the OS shows it
  while the app is in the background or killed - no Dart background handler needed), `android.priority:
  high` and `android.notification.channel_id = booking_app_alerts`, the high-importance channel the
  Flutter app creates in `MainActivity.kt` - keep `FcmPushNotificationSender.ANDROID_CHANNEL_ID` and the
  app in sync (`FcmPushNotificationSenderTest` pins the payload shape). Chat pushes skip accounts with an
  open socket (`ChatSessionRegistry.isOnline`); `WebSocketConfig` sets a 70 s idle timeout (the app pings
  every 25 s) so a phone that dropped off without a close frame stops counting as online.
  `POST /notifications/devices/test` (`NotificationService.sendTestPush`) pushes a test notification to
  the caller's own tokens after a delay via `CompletableFuture.delayedExecutor` straight into
  `PushNotificationListener` (no inbox row), so the app's setup screen can check delivery with the app
  swiped away (Xiaomi & co. drop it unless Autostart is on - that part is device settings, not code).
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
  (room types/prices, amenities, services, menu, tours as `[mã id]` lines with paused ones marked
  "TẠM NGỪNG" and each package's rooms/nights - public browse data only; the user prompt adds the guest's
  last 5 tour orders at the branch) and the rules make tour ordering two turns: look up free package rooms
  with `find_tour_rooms`, send a summary (rooms, stay, tour + room amounts) and ask for "Đồng ý", then
  call `create_tour_booking` with the room numbers. `ChatAssistant.bookTour` refuses the call unless the newest message is the
  guest's and `looksLikeConsent` matches it (accent-insensitive word list), so a model that skips the
  summary books nothing; the tool runs in both suggest and auto-reply mode. It also books **rooms** the same
  way: `create_room_booking` (room numbers from `find_available_rooms`, check-in and check-out) goes through
  `BookingService.placeRoomsFromChat`, which takes the branch and guest from the conversation (auto-reply has no
  signed-in caller), re-checks the stay and every room (exists at the branch, not MAINTENANCE, free for the whole
  range), sums `price x nights`, saves it CASH/UNPAID/PENDING with the note "Đặt qua trợ lý AI trong tin nhắn",
  returns the open booking instead of a second one for the same stay, and sends `BOOKING_CREATED` to the branch
  team (the only place that notification is used). `ChatAssistant.bookRooms` applies the same consent gate as
  tours. `looksLikeConsent` also refuses a message that is really a question ("đổi sang ngày 20 được không") -
  `QUESTION` - because a false yes would place an order the guest never agreed to (`ChatAssistantConsentTest`).
  Besides the two booking tools the model gets `find_available_rooms` (a real availability lookup, same rule as the
  hotel detail page) and `handoff_to_staff` (sets `Conversation.needsStaff` + `handoffReason`, shown
  only on the hotel side, cleared when a person on the team replies). Guest text goes inside
  `<conversation>` tags and the rules tell the model it is data, not instructions. Two entry points:
  `POST /ai/conversations/{id}/suggest-reply` (team member gets a draft and nothing is sent to the
  guest, but if the guest already agreed to a tour summary the order is placed and the draft says so) and
  auto-reply - when `Hotel.aiAutoReply` is on, `ChatEventListener` calls
  `ChatAssistant.onCustomerMessage` for every guest message; after `ai.auto-reply-delay-seconds` it
  answers only if that message is still the newest (staff didn't answer, guest didn't write again),
  via `ChatService.postAssistantReply` (sender = "Trợ lý AI", `aiGenerated`, no account, hotel
  unread left as is). Delays run on a private scheduled executor, model calls on virtual threads.
- **Natural-language search** (`AiSearchService`, public `POST /ai/search`): the fast model only turns
  the sentence into filters through a forced tool call (`set_search_filters`); matching branches and
  free rooms is plain Java. Unusable filters are dropped with a note instead of failing.

- **Natural-language tour search** (`AiTourSearch`, public `POST /ai/tours/search`): the same shape for tours.
  The fast model fills `set_tour_filters` (destinations plus the province it knows, `TravelTheme`s, price per adult,
  adults/children, tour day, nights, "kèm phòng", keywords); `TourService.publicTours()` supplies the rows and
  `AiTourSearch.score` ranks them. Place, price, head count, rooms and nights are hard conditions; themes, keywords,
  a child price, a sale and `featured` only move a tour up the list, so a wish nobody matches exactly still returns
  the closest tours. With a day named, seats are checked for real against open orders
  (`TourBookingRepository.sumGuests`, `TourBookingServiceImpl.OPEN`) and full tours are reported in `notes`.
  `filters` and `score` are pure and unit-tested (`AiTourSearchTest`).

- **Trip planner** (`AiTripPlanner`, public `POST /ai/trip-plan`): "Hạ Long 3 ngày 2 đêm, 2 người lớn 1 trẻ em,
  6 triệu, thích biển" → a day-by-day plan. Two model calls and the model decides nothing that costs money: the fast
  model fills `set_trip_request` (destination, nights, start date, head count, budget, themes), the backend finds the
  real candidates (`candidates`: active branches matching the place with a room free for the whole stay through
  `HotelKnowledge.availableRooms`, and running tours that fit - package tours are left out and counted in `notes`),
  and the assistant model only arranges them and writes the day titles (`set_trip_plan`, at most one tour a day).
  `build` then drops any hotel or tour that was not offered, cuts days past `nights + 1`, dates each day, picks the
  voucher that takes most off the tour part (`bestVoucher` + `VoucherRules.discountOf`) and sums rooms/tours/total/
  per person from the rows. `request`, `build`, `bestVoucher`, `tourAmount` and `time` are pure
  (`AiTripPlannerTest`).

- **Review digest** (`ReviewDigest`, public `GET /ai/hotels/{hotelId}/review-digest`): "Khách thường khen… /
  thường phàn nàn…" read out of that branch's own reviews (`FeedbackRepository.findByHotel`, newest 40 comments
  inside `<reviews>` tags) through a forced `set_review_digest` call. The text is cached in `HotelReviewDigest`
  (one row per branch, `hotel_id` unique, FK `ON DELETE CASCADE`) and rewritten only when the review count changes,
  so a hotel page costs no model call; the star breakdown and average are computed in `ReviewDigest.stats`, never
  asked of the model. Fewer than `MIN_REVIEWS` (4) reviews, or no API key with nothing cached, answers
  `available = false` and the app hides the card; a model failure falls back to the stored text instead of failing
  the page. `stats`, `prompt` and `split` are unit-tested (`ReviewDigestTest`).

- **Tour draft writer** (`TourDraftWriter`, `POST /ai/tours/draft`, owner/manager via
  `AccessGuard.requireBranchManagerEditor`): the only AI feature that writes rather than reads the database. From
  the few words a manager typed (name, destination, duration, departure, themes, notes) the assistant model fills
  `set_tour_draft` with the tour's long fields - description, `includes`, cancellation terms and an itinerary that
  matches the duration - and the branch's own name/address/category go in so the text fits the real place. The
  rules forbid prices and invented partners; nothing is written to a tour, the app only fills the form. `prompt`
  and `response` are pure (`TourDraftWriterTest`).

- **Report analyst** (`AiReportAnalyst`, `POST /ai/reports/ask` with exactly one of `hotelId` / `hotelChainId`):
  "Doanh thu tháng này so với tháng trước thế nào?" answered from the branch's or chain's own figures. Two model
  calls and no model arithmetic: the fast model only reads the period out of the question
  (`set_report_question`), the backend then calls the ordinary `ReportService` (`getRevenueSummary`,
  `getRevenueByTour`, `getBranchPerformanceForChain`, `getRevenueOutlook`) - so the report guards apply and the
  numbers match the report screens - and the assistant model writes the answer from `brief`, a plain-text block of
  those figures, into `set_report_answer` (answer, highlights, suggestions). The KPIs travel back with the answer so
  every claim can be checked. `period`, `brief`, `money` and `number` are pure (`AiReportAnalystTest`); the period
  defaults to this month to date and is clamped to today and to `MAX_DAYS`.

- **Pricing advisor** (`AiPricingAdvisor`, `GET /ai/branches/{hotelId}/pricing-advice`, branch manager or above
  through the report service's guards): per room type, whether to raise, hold or lower the price. The brief is the
  branch's last `HISTORY_DAYS` (30): the revenue summary, `getRevenueByRoomType` (list price vs real ADR and
  occupancy), the weekday pattern and what is already booked for the next `OUTLOOK_DAYS`. The model answers
  `set_pricing_advice` with a direction and a percentage; `build` drops room types that do not exist, clamps the
  percentage to `MAX_PERCENT`, lists every untouched type as HOLD and works the suggested price out from the list
  price itself (nearest thousand đồng), so no figure is the model's arithmetic. Nothing changes a room's price.
  `brief`, `build`, `percent`, `action` and `suggestedPrice` are pure (`AiPricingAdvisorTest`).

- **The guest's own assistant** (`AiGuestAssistant`, `POST /ai/assistant/ask`, CUSTOMER only): "đơn sắp tới của
  tôi thế nào?", "tôi còn mã giảm giá nào?". The brief is the signed-in guest's own rows - their last 8 room
  bookings and tour orders (status, dates, rooms, amounts, voucher used), the usable vouchers and their interests -
  and the model may mention nothing else. `set_guest_answer` may offer deep links, but `build` keeps only the
  targets in `allowedTargets` (`bookings/<id>`, `tour-bookings/<id>`, `tours/<id>` of those rows), stripping a
  `bookingapp://` prefix and dropping duplicates, so a link always opens something the guest really has. It cannot
  book, cancel or pay - the rules point at the app's own screens. `brief`, `allowedTargets` and `build` are pure
  (`AiGuestAssistantTest`).

- **Chat summary** (`ChatSummarizer`, `GET /ai/conversations/{id}/summary`, branch team via
  `AccessGuard.requireBranchTeam`): a long thread in one note - what the guest came for, `openItems`, `nextSteps`
  and `urgency` (LOW/NORMAL/HIGH, anything else read as NORMAL). The prompt is only the newest `MAX_MESSAGES` (40)
  through `ChatAssistant.transcript` (extracted so every chat prompt shows the thread the same way, guest words
  inside `<conversation>` tags), with no prices or policies to invent from. `waitingOnHotel` is worked out from who
  wrote last, and `needsStaff`/`handoffReason` come from the conversation row - never from the model. `transcript`,
  `build` and `urgency` are pure (`ChatSummarizerTest`).

- **Review themes of a chain** (`AiReviewThemes`, `GET /ai/chains/{hotelChainId}/review-themes?from=&to=`, chain
  owner): the owner's version of the review digest - what guests keep talking about across the chain (cleanliness,
  staff, location, noise…) with a GOOD/MIXED/BAD reading and which branches raised it. The prompt groups the
  period's comments under their branch (`MAX_COMMENTS` 80 in total, 20 per branch so one busy branch cannot crowd
  the others out) and lists the chain's real branch names; `build` keeps only names that match one of them, drops a
  repeated topic and reads an unknown sentiment as MIXED. Reviews and averages per branch are computed in
  `byBranch`. Fewer than `MIN_COMMENTS` (6) commented reviews answers with no themes and a note. `byBranch`,
  `prompt`, `build` and `sentiment` are pure (`AiReviewThemesTest`).

- **Content audit** (`AiContentAudit`, `GET /ai/branches/{hotelId}/content-audit`, branch manager or above): how
  ready a branch's page is to sell. The eleven checks (description long enough, photos, amenities, services, rooms
  with a photo and a price, tours with a destination / itinerary / photo, dishes priced) and the `score` (share of
  checks passed) are counted here from the branch's own rows, so the endpoint still answers without an API key -
  only the advice is missing then. The model is told the results and writes `set_content_audit`; `build` drops
  advice pointing at a check that passed, does not exist, or repeats one. A section with no rows (a branch selling
  no tours) passes rather than nagging. `checks`, `score`, `prompt`, `build` and `priority` are pure
  (`AiContentAuditTest`).

- **Daily brief** (`AiDailyBrief`, `GET /ai/branches/{hotelId}/daily-brief`, branch manager or above): "việc hôm
  nay" - arrivals and departures of the day, orders still waiting to be confirmed (URGENT once one has waited over
  `STALE_PENDING_HOURS`), tours leaving today and tomorrow with their head count, unread chat and handed-off threads,
  reviews of `LOW_RATING` stars or fewer in the last `REVIEW_DAYS`, money uncollected (URGENT past
  `RECEIVABLE_OVERDUE_DAYS`), rooms in maintenance, and the month's target measured against the calendar rather than
  against 100 (`TARGET_GAP_POINTS`). Every signal and its URGENT/WATCH/QUIET level is counted in `signals`, so the
  endpoint still answers without an API key; the model only orders them and writes a line each, and `build` drops a
  task pointing at a QUIET signal, a repeated one or a key nobody counted. `getTargetProgress` and `getReceivables`
  come from `ReportService`, so the brief can never disagree with the report screens. `signals`, `targetSignal`,
  `prompt` and `build` are pure (`AiDailyBriefTest`).

`AiClient` is the single outbound call. Services always speak the Anthropic Messages shape
(system blocks, `tool_use`/`tool_result`); for `AI_PROVIDER=gemini` (auto-selected when only
`GEMINI_API_KEY` is set - the free default) or `openai` (Groq, OpenRouter, Ollama via `AI_BASE_URL`),
`OpenAiCompat` translates to/from Chat Completions. It carries the model's tool-calling assistant turn
through verbatim in a private `_openai_raw` block because Gemini 3 rejects a follow-up turn without
its thought signatures. A provider 5xx is retried twice on the same model; a model still overloaded
(`AI_OVERLOADED`, 1017) or out of quota (`AI_QUOTA_EXCEEDED`, 1018 - quotas are per model) makes
`AiClient.withModelFallback` rerun the *whole* generation on the next model of the chain
(`gemini-3.8-flash > gemini-3.5-flash > gemini-2.5-flash`, search `gemini-3.5-flash-lite >
gemini-2.5-flash-lite`; override with `AI_*_FALLBACK_MODELS`). Never switch models mid tool loop:
signatures don't carry over. No key → `isEnabled()` false, `GET /ai/status` says so, endpoints throw
`AI_UNAVAILABLE` (1015, 503). `AiRateLimiter` caps calls per account/IP in memory (`AI_RATE_LIMITED`,
1016, 429). To test without a key, point `AI_BASE_URL` at a local stub that serves `/chat/completions`
(or `/v1/messages` for anthropic); there is no real-model test in the repo.

### Payments

VNPay sandbox integration (`VNPayConfig`, `VNPayUtil`, `PaymentServiceImpl`,
`PaymentController#vn-pay-callback`). Bookings carry a `PaymentStatus`; the scheduler above expires
bookings left in a pending-payment state past their window.

The payment state is tied to the method by `BookingServiceImpl.applyPaymentMethod`: `VN_PAY` →
`PENDING` with a 15-minute `paymentExpireAt`, `CASH`/`BANK_TRANSFER` → `UNPAID` with no deadline.
Create, `PUT /bookings/{id}/payment-method` (the booking's customer or the branch team, admin refused;
`PAYMENT_METHOD_LOCKED` 1019 once paid, completed or cancelled; re-sending the current method is a
no-op so a running VNPay window is not restarted) and a method change through `PUT /bookings/{id}`
all go through it - never set `paymentMethod` alone. A successful VNPay callback uses
`markPaidByVnPay`, which also stamps the method back to `VN_PAY`, because a guest can switch to cash
after opening the VNPay page and still finish paying there.

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
  is a single client-supplied figure, so revenue by room type / by room takes each completed
  booking's room part (`totalAmount` − its `BookingService.total` lines) and shares it among the
  booking's rooms by today's `room.price × nights` (`RevenueFigures.roomRevenue`) - a later price
  change only shifts the split, never the amount. Service revenue *is* stored (`BookingService.total`,
  set from `service.unitPrice × quantity` at booking time).
- **Occupancy and room-night maths happen in Java**, not SQL (`BookedRoomNightRow` +
  `ReportServiceImpl.nightsWithin`), to avoid DB-specific date arithmetic. `occupancy/daily` is
  capped at 92 days (`MAX_DAILY_RANGE`).
- **`/reports/summary` is the only revenue figure that includes tours.** It fetches plain rows
  (`CompletedBookingRow`, `TourSaleRow`, `BookedRoomNightRow`) for the period and the period
  `RevenueFigures.previousPeriod` picks (calendar-aligned: month-to-date/whole month vs the previous
  month, multi-month ranges from 1 January vs last year; otherwise as many days just before), and
  `RevenueFigures` (pure, unit-tested in `RevenueFiguresTest`) does the sums:
  room revenue = booking totals − service lines, tours add `tourAmount` only (the package rooms are
  their own `Booking` and earn at checkout), ADR/RevPAR in whole đồng, growth `null` when the
  previous period was 0. Its `from`/`to` are inclusive (single day allowed), unlike `occupancy`.
  The older time-series endpoints stay booking-only so existing clients' numbers don't move.
  `/reports/revenue-by-tour` reuses the same `TourSaleRow`s via `RevenueFigures.byTour` (grouped by
  tour id, or by the kept name once the tour was deleted).
  `/reports/chain/branches` runs the same per-period rows once per branch (current + previous), then
  `RevenueFigures.rankBranches` fills each branch's share of the chain total and sorts by revenue.
- **`/reports/outlook` looks forward from `LocalDate.now()`** (`days` 1–90): open = PENDING/CONFIRMED
  bookings (`findOpen`, check-out ≤ window end — earlier check-outs are reported as overdue) and tour
  orders (`findUpcoming`), on the same days the revenue reports will count them once completed.
  `outstanding*` is every COMPLETED booking not marked PAID (`completeBooking` never touches the
  payment status, so this is the front desk's to-collect list).
- The `.xlsx` exports reuse the same service methods (so the same guards): "KPI so với kỳ trước",
  "Thanh toán & kênh bán", "Doanh thu theo tour", and for chains "So sánh cơ sở" right after the KPIs.
  `ExcelReportServiceImplTest` mocks `ReportService` and reads the workbook back.
- **Summary extras.** Each `RevenuePeriodDTO` also carries average stay / lead time (from
  `CompletedBookingRow` dates and `Booking.onCreate`), cancelled revenue, and profit after payroll:
  `PayrollRepository.findCosts` (APPROVED/PAID payslips of branch employees - managers have none) spread
  per day of each payslip's month by `RevenueFigures.payrollCost`. `byCustomerType` (NEW/RETURNING) is
  set by `revenueSummary` after `RevenueFigures.summary`, from `findCustomerIdsBefore(from)`.
- **Per-room, weekday, staff, top customers** all merge plain rows in `RevenueFigures` (`rooms`,
  `weekdays`, `staff`, `topCustomers`). The JPQL projections that grew extra fields
  (`StaffPerformanceDTO`, `TopCustomerDTO`, `ReceivableDTO`) keep an explicit constructor matching the
  query's arguments - adding a field to them must not break that constructor.
- **Outlook occupancy.** `revenueOutlook` adds the rooms booked per night and the window's on-the-books
  occupancy with `RevenueFigures.occupancyOnTheBooks`, fed by the occupancy report's own query
  (`findBookedRoomNights`: any booking not cancelled), and the same nights a year earlier for `lastYearOccupancy`
  (null when nothing was booked then). `targetProgress` reuses `RevenueFigures.outlook`
  without it.
- **Cancellation lead time.** `Booking.canceledAt`/`canceledBy` are set by `cancelBooking` and by a tour
  cancel that frees its rooms (copying the tour's side). `RevenueFigures.cancellationLeadTimes` buckets
  `findCanceled` rows by days before check-in and falls back to `onUpdate` for rows cancelled before the
  columns existed (those have no side). Any new place that cancels a booking must set both.
- **Room types** come from `RevenueFigures.roomTypes`: `RoomRepository.countByRoomType` (rooms + average list
  price) so unsold types are listed, completed stays for revenue/ADR, `findBookedRoomNights` for occupancy.
  `PeriodRows` gained `bookingsWithServices` (service attach rate); its old 12-argument constructor stays for
  the tests and passes 0.
- **Guest reports** (`/reports/guest-origins`, `/reports/ratings`) are computed by `GuestFigures`
  (`GuestFiguresTest`); hometown is free text, grouped after trimming and lower-casing. Ratings use
  `Feedback.reviewDate`, and the branch comparison adds each branch's reviews and average.
- **More report pieces** (all pure helpers in `RevenueFigures` unless noted): trend buckets carry
  `payrollCost`/`grossProfit` (`withPayroll`); the summary adds `byPaymentTiming` from `paidAt`
  (`paymentTiming`) and `uniqueGuests`/`revenuePerGuest`; `/reports/collections` (`collections`, cash basis by
  `paidAt`, max 92 days); `/reports/booking-hours` (`OrderHours`, weekday x hour of `onCreate`); tours get
  departures/seats/fill rate from `Tour.maxGuests`. `RevenueByTourDTO` and `TrendBucketDTO` keep their old
  constructors for the callers that build them positionally.
- **Low reviews** (`FeedbackServiceImpl.alertIfLow`): a review created at, or lowered to, 2 stars or fewer sends
  `LOW_RATING_REVIEW` to the chain owner and branch manager - once, not on later edits of a low review.
- **AI assistant report** (`/reports/ai-assistant`) is computed by `ChatFigures` (unit-tested in
  `ChatFiguresTest`) from `ChatMessageRepository.findForReport` - messages written in the period, thread by
  thread. A reply's wait runs from the first guest message still unanswered; the assistant's messages are
  `aiGenerated` HOTEL messages. Tour orders "placed by AI" are counted by `onCreate`, while AI tour revenue
  stays on the revenue reports' basis (completed, by tour date). `waitingForStaff` is the live `needsStaff`
  count, not period-bound.
- **Monthly targets** (`RevenueTarget`, unique per branch+month, `month` = first day). Progress is
  computed on read (`targetProgress`: revenue so far + `RevenueFigures.outlook` for the rest of the
  month). `RevenueTargetScheduler` (08:00) calls `sendRevenueTargetAlerts`, which has **no access
  check** - it is only reachable from the scheduler and from `runRevenueTargetAlertsNow` (admin);
  don't map it to an endpoint directly. The `reachedNotified`/`atRiskNotified` flags reset when the
  amount changes. `WeeklyReportScheduler` (Monday 08:00) → `sendWeeklyDigests` follows the same
  pattern (admin twin: `POST /reports/weekly-digest`); it compares Monday-Sunday with the previous
  week explicitly instead of `previousPeriod`, whose month rule would kick in for a week starting on
  the 1st. Both jobs notify every approved chain in the DB - the shared local DB included, so don't
  call the admin endpoints from automated runs against it.

### Public browsing

`SecurityConfig.PUBLIC_ENDPOINT_GET` allowlists the anonymous browse surface (`/hotels`,
`/hotels/detail/{id}`, `/hotels/category`, `/hotels/search`, `/rooms`, `/rooms/{id}`,
`/rooms/available`, `/amenities/hotel/{hotelId}`, `/amenities/room`, `/services`, `/dishes`,
`/dishes/{id}`, `/tours`, `/tours/{id}`) so a visitor can
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
