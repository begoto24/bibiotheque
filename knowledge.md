# Project Knowledge

## What this is

Full-stack library management system (gestion de bibliothèque):
- **`bibliotheque-backend/`** — Spring Boot 2.4.5 REST API on port 8080. Package root: `com.ibizabroker.bibliotheque`.
- **`bibliotheque-frontend/`** — Angular 14 app on port 4200 (15 components, Bootstrap 5).

Two roles: **Admin** (CRUD books/users) and **User** (borrow/return books). JWT auth (`jjwt` 0.9.1), BCrypt password hashing. Root `README.md` (in French) is the canonical doc — architecture, API reference, and a known-issues table live there. `SEANCE-1.md` / `EPREUVE-SEANCE-1.md` are course/exam materials.

## Key layout

Backend (`bibliotheque-backend/src/main/java/com/ibizabroker/bibliotheque/`):
- `entity/` — JPA entities (Books, Users, Role, Borrow); `JsonDataSerializer` formats dates as `dd-MM-yyyy`
- `dao/` — Spring Data JPA repositories
- `controller/` — HTTP endpoints: `BooksController` (`/admin/books`), `AdminController` (`/admin/users`), `BorrowController` (`/borrow`), `JwtController` (`/authenticate`)
- `configuration/` — `WebSecurityConfiguration` (route rules), `JwtRequestFilter`, `CorsConfiguration`, `JwtAuthenticationEntryPoint`
- `util/JwtUtil.java` — token creation/validation
- `src/main/resources/application.properties` — DB connection, JPA settings

Frontend (`bibliotheque-frontend/src/app/`):
- One folder per screen (e.g. `books-list/`, `create-book/`, `borrow-book/`), each with `.html` / `.css` / `.ts` / `.spec.ts`
- `_service/` — HTTP calls to the backend (`books.service.ts`, `users.service.ts`, `borrow.service.ts`, `user-auth.service.ts`)
- `_auth/` — `auth.guard.ts` (route protection by role) and `auth.interceptor.ts` (adds `Authorization: Bearer <token>` to every request; redirects to `/login` on 401, `/forbidden` on 403)
- `app-routing.module.ts` — URL → component + allowed roles

## Commands

Backend:
- Run: `cd bibliotheque-backend && ./mvnw spring-boot:run` (Windows: `mvnw.cmd spring-boot:run`)
- Test/build: `./mvnw test` / `./mvnw package`

Frontend:
- Install: `cd bibliotheque-frontend && npm install`
- Dev server: `npm start` (= `ng serve`, serves on 4200)
- Build: `npm run build`
- Test: `npm test` (`ng test`, Karma/Jasmine — requires Chrome)

## Conventions & gotchas

- **Database is PostgreSQL** in the current working state: `application.properties` points at `jdbc:postgresql://localhost:5432/KAFOKAMLybrery48` (user/pass `postgres`/`postgres`). Note: the README still describes the original MySQL setup (`bibliotheque` db, root/mysql) and Docker files do not exist — README is stale on those points.
- `spring.jpa.hibernate.ddl-auto=update` creates tables on startup but **not** the database/schema; the schema must exist beforehand.
- **No bootstrap account exists** — `POST /admin/users` requires a token, so the first admin must be inserted via SQL with a BCrypt hash (see README §5, login `admin`/`admin123`).
- Backend compiles on JDK 17+ thanks to Lombok pinned to `1.18.32` in `pom.xml` (was the historical build failure); `java.version` is still `1.8` and Spring Boot is 2.4.5 — old but working.
- Frontend is Angular 14; Node 22 is officially unsupported (`ng version` warns) but builds fine.
- Backend API base URL `http://localhost:8080` is hardcoded in the Angular services, not in `environment.ts`.
- CORS is configured server-side to allow the frontend origin; both servers must run together (8080 + 4200).
- Borrowing logic: `POST /borrow` decrements `noOfCopies` (7-day due date), `PUT /borrow` increments it on return — the controller touches **two** tables.
- Dates are serialized as `dd-MM-yyyy` (no time component) via `JsonDataSerializer`.
- Git: one branch per feature, never push to `main` directly; `node_modules/`, `target/`, `dist/` are gitignored.
