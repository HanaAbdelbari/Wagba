# users-service

Accounts, authentication, sessions and user administration for Wagba.

- Port: 5001
- Database: `users_db` (PostgreSQL)
- Swagger UI: http://localhost:5001/swagger-ui.html

## Requirements
Java 21, PostgreSQL, RabbitMQ.

## Setup
1. Create the database (run as the postgres user):
   ```sql
   CREATE ROLE users_user LOGIN PASSWORD 'dev_password';
   CREATE DATABASE users_db OWNER users_user;
   ```
2. Optional, create the first SuperAdmin on first start by setting these environment variables:
   `WAGBA_ADMIN_EMAIL` and `WAGBA_ADMIN_PASSWORD` (12 characters or more).
3. Run `UsersServiceApplication`.
   Flyway creates the schema and JWT keys are generated in `keys/` (never commit this folder).

## Tests
Create the test database once:
```sql
CREATE DATABASE users_test_db OWNER users_user;
```
Then run `.\mvnw test`.

## Events published (RabbitMQ exchange `wagba.events`, topic)
| Event | Routing key |
|---|---|
| UserRegistered | `user.registered` |
| UserStatusChanged | `user.status.changed` |
| UserRoleChanged | `user.role.changed` |
| PasswordResetRequested | `password.reset.requested` |
| EmailVerificationRequested | `email.verification.requested` |
| EmailVerified | `email.verified` |

## Configuration
| Property | Meaning |
|---|---|
| `wagba.jwt.*` | issuer, key paths, token lifetimes |
| `wagba.verification.*` | code lifetime, max attempts, cooldown, pepper (set `WAGBA_VERIFICATION_PEPPER` outside development) |
| `wagba.outbox.*` | poll interval, batch size, publisher on/off |