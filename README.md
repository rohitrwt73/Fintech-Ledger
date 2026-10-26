# FinTech Ledger

An educational simulated wallet and double-entry ledger built as a Java 21 / Spring Boot modular monolith with a React + TypeScript dashboard. All amounts are INR simulation units. This project has no real payment rails and must not be used to hold or transfer actual funds.

## Architecture and accounting

The backend is split into `auth`, `user`, `account`, `transfer`, `ledger`, `refund`, `reconciliation`, `risk`, `audit`, `config`, and `common` areas. Requests flow through controller, service, repository, and PostgreSQL. Flyway owns schema changes; Hibernate runs with `ddl-auto=validate`.

The wallet convention is **credit increases a wallet balance; debit decreases it**. Every transfer writes a debit to the source and an equal credit to the destination in one database transaction. The balance column is a read projection, updated under pessimistic row locks. Transfer locks are acquired in ascending UUID order, which prevents concurrent source debits from overspending and reduces deadlocks. Idempotency keys are stored with a SHA-256 request fingerprint in PostgreSQL. Posted ledger rows are not exposed through mutation endpoints.

The simulated opening pool is represented by balanced opening entries between a treasury contra account (negative balance) and the clearing account (positive balance). Authenticated users can use `POST /api/v1/transfers/simulated-funding` with `{ "accountId": "…", "amount": "2500.00" }`; it debits clearing and credits their wallet. This development-only endpoint is capped at ₹100,000 per call and is not a real deposit.

## Requirements

- Java 21 and Maven 3.9+
- Node.js 22+ and npm
- Docker Desktop for the local PostgreSQL option

## Run locally with Docker PostgreSQL

From the repository root:

```powershell
Copy-Item .env.example .env
docker compose up -d postgres
```

In terminal 1, run the backend:

```powershell
$env:DB_URL = 'jdbc:postgresql://localhost:5432/fintech_ledger'
$env:DB_USERNAME = 'ledger'
$env:DB_PASSWORD = 'ledger'
$env:JWT_SECRET = 'replace-this-with-a-long-random-local-secret-value'
mvn -f .\pom.xml org.springframework.boot:spring-boot-maven-plugin:3.4.5:run
```

In terminal 2, run the frontend:

```powershell
cd frontend
npm install
npm run dev
```

Open <http://localhost:5173>. Register a user, create a wallet under **Accounts**, and use its **Add simulated funds** button before testing a transfer. The transfer form accepts the recipient's visible account number; the sender selects one of their own wallets. Or start all containers with `docker compose up --build`; configure the environment first with `Copy-Item .env.example .env`.

## Neon PostgreSQL

Copy `.env.example` to `.env` and set `DB_URL` to a Neon JDBC URL, for example `jdbc:postgresql://YOUR_NEON_HOST/YOUR_DATABASE?sslmode=require`, plus `DB_USERNAME` and `DB_PASSWORD`. Keep credentials out of source control. Set `COOKIE_SECURE=true` and a cryptographically random `JWT_SECRET` for HTTPS deployments. If connection setup fails, verify the host/database spelling, Neon role password, outbound network policy, and `sslmode=require`; check that the Neon project is awake and that Flyway can create tables.

## Build and tests

```powershell
mvn verify
cd frontend
npm install
npm test
npm run build
```

The backend currently has unit tests for monetary validation and balance equality. Testcontainers PostgreSQL dependencies are configured, but database integration tests are still needed. The frontend has a Vitest dashboard empty-state test.

## API

Base path: `/api/v1`. OpenAPI UI: `/swagger-ui.html`; JSON spec: `/v3/api-docs`.

```http
POST /api/v1/auth/register
Content-Type: application/json
{"email":"demo@example.com","password":"change-me-please","fullName":"Demo User"}

POST /api/v1/accounts
Content-Type: application/json
{"currency":"INR"}

POST /api/v1/transfers/simulated-funding
Content-Type: application/json
{"accountId":"ACCOUNT_UUID","amount":"10000.00"}

POST /api/v1/transfers
Idempotency-Key: 9be3c8e5-6a93-4e88-a714-a332379b96c2
Content-Type: application/json
{"sourceAccountId":"ACCOUNT_UUID","destinationAccountNumber":"IN0123456789ABCDEF","amount":"500.00","description":"Shared lunch"}
```

Authentication uses an HttpOnly, SameSite=Strict JWT cookie and a double-submit CSRF cookie (`GET /api/v1/auth/csrf`); the browser client sends `X-XSRF-TOKEN`. Set `COOKIE_SECURE=true` behind HTTPS. New registrations always receive `USER`; roles are not accepted from clients. Admin endpoints require `ADMIN`. For a local demo admin, promote a registered user using a trusted database operator: `UPDATE app_users SET role='ADMIN' WHERE email='admin@example.com';` (never expose this operation to the app).

## Deployment notes

Build the backend with `mvn -DskipTests package` and serve `target/fintech-ledger-1.0.0.jar`. Build the frontend with `npm run build` and serve `frontend/dist` from a static host or the included Nginx image. Configure a managed PostgreSQL database such as Neon, HTTPS, secure cookies, `FRONTEND_ORIGIN`, `DB_*`, and a strong `JWT_SECRET`. `/actuator/health` is the health probe. CI runs Maven verification and the frontend production build.

This is a portfolio starter, not banking software. Before any production use, complete independent security review, reconciliation and reversal workflows, transaction/risk monitoring, rate limiting, stronger operational controls, privacy review, backups, key rotation, and jurisdiction-specific legal/compliance analysis. Do not use actual customer funds.
