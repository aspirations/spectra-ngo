# Spectra NGO

Multi-tenant care-centre operations: resident records (dogs, cats, cows, elderly people, or other), shelter inventory (FEFO), staff credit POS, attendance/leave, and month-end payroll.

Monorepo: Spring Boot 4.1 backend (`backend/`) + Next.js 15 frontend (`frontend/`). Same git repository.

## Prerequisites

- Java 25+
- PostgreSQL 16+ with database `spectra`
- Node.js 20+
- Maven Wrapper is included (`backend/mvnw`)

Create the database:

```sql
CREATE DATABASE spectra;
```

Default JDBC: `jdbc:postgresql://localhost:5432/spectra` user `postgres` password `root`. Override with `SPRING_DATASOURCE_*`.

## Run backend

```bash
cd backend
./mvnw spring-boot:run
```

Windows: `.\mvnw.cmd spring-boot:run`

API base: `http://localhost:8080/api`

Flyway applies `V1__spectra_schema.sql` then `V2__residents_and_global_email.sql` on startup. Hibernate `ddl-auto` is `validate`.

## Run frontend

```bash
cd frontend
npm install
npm run dev
```

App: `http://localhost:3000`. Next.js rewrites `/api/*` to the Spring Boot server.

## Demo login

Email uniquely identifies the organisation — no tenant code. Password for all users: `Spectra@123`

| Email | Role |
|---|---|
| admin@spectra.org | SUPER_ADMIN (pick branch **Main Centre** in the top bar) |
| branch@spectra.org | BRANCH_ADMIN |
| inventory@spectra.org | INVENTORY_MANAGER |
| vet@spectra.org | VET_TECH_EMPLOYEE |
| employee@spectra.org | EMPLOYEE |

Disable seeding with `APPLICATION_SEED_ENABLED=false`.

## JWT / tenancy

Bearer token claims: `tenant_id`, `branch_id`, `user_id`, `role`. Hibernate `@TenantId` filters tenant rows. Super Admin sends `X-Branch-Id` to act on a branch.

## Month-end payroll

Quartz cron `0 0 22 L * ?` opens a `DRAFT` run for the previous month. Admins can also POST `/api/payroll/runs/generate`.
