# API examples

The browser flow begins with `GET /api/v1/auth/csrf`, then register/login, then `GET /api/v1/auth/me`. The server returns the session in an HttpOnly cookie.

Create and fund a wallet:

```http
POST /api/v1/accounts
{"currency":"INR"}

POST /api/v1/transfers/simulated-funding
{"accountId":"<wallet uuid>","amount":"2500.00"}
```

Transfer (repeat the same key and body to receive the original receipt):

```http
POST /api/v1/transfers
Idempotency-Key: 87a92db4-46f8-4533-b6ef-a1bd35de2220
{"sourceAccountId":"<source uuid>","destinationAccountNumber":"IN0123456789ABCDEF","amount":"500.00","description":"Invoice 42"}
```

The same key with a different request returns `409 Conflict`. Account balances and ledger history are owner-scoped. Administrative routes require an operator-promoted `ADMIN` role.
