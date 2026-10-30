# Account-Service API

Account-Service owns account records and their balances. Its REST, GraphQL and MCP adapters call the same `AccountService`. PostgreSQL is the runtime store; Flyway applies the schema migration on startup.

## Runtime configuration

| Variable | Default | Description |
| --- | --- | --- |
| `SERVER_PORT` | `8081` | HTTP port |
| `DB_URL` | `jdbc:postgresql://localhost:5432/banking` | PostgreSQL JDBC URL |
| `DB_USERNAME` | Required | PostgreSQL login |
| `DB_PASSWORD` | Required | PostgreSQL password |
| `KEYCLOAK_ISSUER_URI` | `http://localhost:8080/realms/banking` | OIDC issuer used to validate JWTs |
| `SECURITY_TRANSACTION_SERVICE_CLIENT_ID` | `transaction-service` | OAuth2 client ID allowed to adjust balances |

For a local run, provide PostgreSQL and the required environment variables, then run from the project root:

```powershell
$env:DB_USERNAME = "banking"
$env:DB_PASSWORD = "<local-secret>"
$env:KEYCLOAK_ISSUER_URI = "http://localhost:8080/realms/banking"
mvn -pl account-service spring-boot:run
```

Do not use the example values as production credentials. The migration is in `account-service/src/main/resources/db/migration/V1__create_account_tables.sql`.

## OAuth2 scopes

- `SCOPE_accounts.read`: read an account when the JWT has a matching `customer_reference` claim; the `transaction-service` client may read accounts for server-side operations.
- `SCOPE_accounts.admin.read`: read across customer references, in addition to `SCOPE_accounts.read`.
- `SCOPE_accounts.write`: create an account only for the JWT's matching `customer_reference`; cross-customer creation requires this scope plus `accounts.admin.write` or the transaction-service client identity.
- `SCOPE_accounts.admin.write`: change an account's `customerReference` ownership field.
- `SCOPE_accounts.status.write`: block or unblock accounts.
- `SCOPE_accounts.balance.write`: required to request a balance adjustment; it is not sufficient by itself. The JWT `azp` or `client_id` must equal `TRANSACTION_SERVICE_CLIENT_ID`.

The MCP Streamable HTTP endpoint is behind the same authenticated security chain. MCP exposes read-only tools; it does not expose a balance mutation tool. The token must also satisfy the method-level account read policy.

## REST

Requests use JSON and require `Authorization: Bearer <access-token>`, except `/actuator/health`.

| Method and path | Required access | Purpose |
| --- | --- | --- |
| `POST /api/accounts` | `accounts.write` and matching customer claim, or `accounts.write` plus `accounts.admin.write`/transaction-service identity | Create an ACTIVE account with a zero balance. Body: `customerReference`, `currency`. |
| `GET /api/accounts/{id}` | `accounts.read` and account/customer access | Read account details. |
| `GET /api/accounts?customerReference=...` | `accounts.read` and matching customer, admin-read, or transaction-service access | List one customer's accounts. The reference is required. |
| `PATCH /api/accounts/{id}` | `accounts.admin.write` | Change the permitted `customerReference` ownership field only. |
| `GET /api/accounts/{id}/balance` | `accounts.read` and account/customer access | Read balance and currency. |
| `PATCH /api/accounts/{id}/status` | `accounts.status.write` | Set status to `ACTIVE` or `BLOCKED`. |
| `PATCH /api/accounts/{id}/balance` | `accounts.balance.write` plus transaction-service client identity | Apply an idempotent credit or debit. |

Balance adjustment example:

```json
{
  "operationReference": "e17db6ae-17ac-4890-97e4-7a8a9e3eb3e2",
  "type": "CREDIT",
  "amount": 125.50,
  "currency": "EUR"
}
```

The `operationReference` is an idempotency key supplied by Transaction-Service. Replaying the same reference and payload returns the original result; reusing it for different details returns `409 Conflict`. Debits that would produce a negative balance, blocked accounts and currency mismatches are rejected. Amounts use `BigDecimal`, four fractional digits maximum, and the currency must be a three-letter code.

REST errors use `application/problem+json`. Missing accounts return `404`; validation/command errors return `400`; blocked accounts, insufficient funds and operation conflicts return `409`; unauthenticated/unauthorized requests return `401`/`403`.

## GraphQL

Endpoint: `POST /graphql`. Authentication and service-layer authorization are the same as REST. GraphQL exposes `account`, `accounts`, `accountBalance` queries and `createAccount`, `updateAccount`, `updateAccountStatus`, `adjustAccountBalance` mutations.

UUIDs use GraphQL `ID`. Monetary values are represented as decimal strings to avoid binary floating-point conversion, for example:

```graphql
query AccountBalance($id: ID!) {
  accountBalance(id: $id) { id balance currency }
}
```

```graphql
mutation CreateAccount($input: CreateAccountInput!) {
  createAccount(input: $input) { id status currency balance }
}
```

`adjustAccountBalance` accepts `operationReference`, `type`, `amount` as a decimal string, and `currency`; the same transaction-service client authorization and idempotency rules apply as REST.

## MCP tools

Spring AI MCP Server WebMVC uses Streamable HTTP (`spring.ai.mcp.server.protocol=STREAMABLE`). Registered read-only tools:

- `get_account(accountId)`: read account details.
- `check_account_status(accountId)`: read current status.
- `get_account_balance(accountId)`: read balance and currency.

The endpoint requires a valid JWT and the same account-read policy. It is intentionally not exposed without authentication and has no balance-write tool.

## Tests

Run all module tests and compile its reactor prerequisites from the workspace root:

```powershell
mvn -pl account-service -am clean verify
```

Tests use the H2 in-memory database in PostgreSQL compatibility mode because Docker was unavailable during implementation. The runtime schema and driver target PostgreSQL; validate the migration and locking behavior against a real PostgreSQL instance before deployment.