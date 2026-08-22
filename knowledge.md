# Project Knowledge

## What this is

Full-stack library management system (gestion de bibliothèque):
- **`bibliotheque-backend/`** — Spring Boot 2.4.5 REST API on port 8080. Package root: `com.ibizabroker.bibliotheque`.
- **`bibliotheque-frontend/`** — Angular 14 app on port 4200 (15 components, Bootstrap 5).

Two roles: **Admin** (CRUD books/users) and **User** (borrow/return books). JWT auth (`jjwt` 0.9.1), BCrypt password hashing. Root `README.md` (in French) is the canonical doc — architecture, API reference, and a known-issues table live there.

## Key layout

Backend (`bibliotheque-backend/src/main/java/com/ibizabroker/bibliotheque/`):
- `entity/` — JPA entities (Books, Users, Role, Borrow, Reservation, ReservationStatut); `JsonDataSerializer` formats dates as `dd-MM-yyyy`
- `dao/` — Spring Data JPA repositories (BooksRepository, UsersRepository, BorrowRepository, ReservationRepository)
- `controller/` — HTTP endpoints: `BooksController` (`/admin/books`), `AdminController` (`/admin/users`), `BorrowController` (`/borrow`), `JwtController` (`/authenticate`), `ReservationController` (`/api/reservations`)
- `service/` — `JwtService` (authentication), `ReservationService` (business rules RG-01 to RG-06)
- `dto/` — `ReservationRequestDTO`, `ReservationResponseDTO`, `ErrorResponseDTO`
- `exceptions/` — `NotFoundException`, `ConflictException`, `BadRequestException`
- `configuration/` — `WebSecurityConfiguration` (route rules), `JwtRequestFilter`, `CorsConfiguration`, `JwtAuthenticationEntryPoint`
- `util/JwtUtil.java` — token creation/validation
- `src/main/resources/application.properties` — DB connection, JPA settings

Frontend (`bibliotheque-frontend/src/app/`):
- One folder per screen (e.g. `books-list/`, `create-book/`, `borrow-book/`), each with `.html` / `.css` / `.ts` / `.spec.ts`
- `_service/` — HTTP calls to the backend (`books.service.ts`, `users.service.ts`, `borrow.service.ts`, `user-auth.service.ts`)
- `_auth/` — `auth.guard.ts` (route protection by role) and `auth.interceptor.ts` (adds `Authorization: Bearer <token>` to every request; redirects to `/login` on 401, `/forbidden` on 403)
- `app-routing.module.ts` — URL → component + allowed roles

## Reservation module (RG-01 to RG-06)

The Reservation module implements book reservation rules:
- **RG-01**: Only books with `noOfCopies == 0` can be reserved
- **RG-02**: One active reservation per member per book
- **RG-03**: Max 3 active reservations per member
- **RG-04**: Reservations expire after 7 days
- **RG-05**: Can only cancel if status is `EN_ATTENTE` or `DISPONIBLE`
- **RG-06**: Terminal statuses (`ANNULEE`, `EXPIREE`, `HONOREE`) cannot be changed

Status flow: `EN_ATTENTE` → `DISPONIBLE` → `HONOREE` | `ANNULEE` | `EXPIREE`

Endpoint: `POST/GET/PATCH/DELETE /api/reservations`

## Commands

Backend:
- Run: `cd bibliotheque-backend && ./mvnw spring-boot:run` (Windows: `mvnw.cmd spring-boot:run`)
- Test/build: `./mvnw test` / `./mvnw package`

Frontend:
- Install: `cd bibliotheque-frontend && npm install`
- Dev server: `npm start` (= `ng serve`, serves on 4200)
- Build: `npm run build`
- Test: `npm test` (`ng test`, Karma/Jasmine — requires Chrome)

Docker:
- Start DB: `docker compose up -d` (PostgreSQL 16, port 5432, auto-creates schema via `docker/init/01-create-db.sql`)

## Conventions & gotchas

- **Database is PostgreSQL**: `application.properties` points at `jdbc:postgresql://localhost:5432/KAFOKAMLybrery48` (user/pass `postgres`/`postgres`). The README still describes the original MySQL setup — README is stale on DB points.
- `spring.jpa.hibernate.ddl-auto=update` creates tables on startup but **not** the database/schema; the schema must exist beforehand (or use `docker compose up`).
- **No bootstrap account exists** — `POST /admin/users` requires a token, so the first admin must be inserted via SQL with a BCrypt hash (see README §4, login `admin`/`admin123`).
- Backend compiles on JDK 17+ thanks to Lombok pinned to `1.18.32` in `pom.xml` (was the historical build failure); `java.version` is still `1.8` and Spring Boot is 2.4.5 — old but working.
- Frontend is Angular 14; Node 22 is officially unsupported (`ng version` warns) but builds fine.
- Backend API base URL `http://localhost:8080` is hardcoded in the Angular services, not in `environment.ts`.
- CORS is configured server-side to allow the frontend origin; both servers must run together (8080 + 4200).
- Borrowing logic: `POST /borrow` decrements `noOfCopies` (7-day due date), `PUT /borrow` increments it on return — the controller touches **two** tables.
- Reservation endpoint (`/api/reservations`) is **not** protected by JWT — no `@PreAuthorize` or role check. This is by design for the current scope.
- Dates are serialized as `dd-MM-yyyy` (no time component) via `JsonDataSerializer`.
- Git: one branch per feature, never push to `main` directly; `node_modules/`, `target/`, `dist/` are gitignored.
