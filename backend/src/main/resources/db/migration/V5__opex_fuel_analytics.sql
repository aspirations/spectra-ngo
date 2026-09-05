-- OpEx, per-dog expense links, fuel logs, analytics alerts (modular monolith domains)

CREATE TABLE operating_expenses (
    id                 BIGSERIAL PRIMARY KEY,
    tenant_id          BIGINT NOT NULL REFERENCES tenants (id),
    branch_id          BIGINT NOT NULL REFERENCES branches (id),
    category           VARCHAR(40) NOT NULL,
    description        VARCHAR(500) NOT NULL,
    amount             NUMERIC(14, 2) NOT NULL,
    payment_source     VARCHAR(40) NOT NULL,
    expense_date       DATE NOT NULL,
    receipt_url        VARCHAR(500),
    paid_by_user_id    BIGINT REFERENCES users (id),
    vendor_name        VARCHAR(200),
    created_date       TIMESTAMP,
    last_modified_date TIMESTAMP,
    created_by         BIGINT,
    last_modified_by   BIGINT
);
CREATE INDEX idx_opex_branch_date ON operating_expenses (tenant_id, branch_id, expense_date);

CREATE TABLE dog_expense_links (
    id                 BIGSERIAL PRIMARY KEY,
    tenant_id          BIGINT NOT NULL REFERENCES tenants (id),
    branch_id          BIGINT NOT NULL REFERENCES branches (id),
    resident_id        BIGINT NOT NULL REFERENCES residents (id),
    operating_expense_id BIGINT REFERENCES operating_expenses (id),
    shelter_product_id BIGINT REFERENCES shelter_products (id),
    consumption_id     BIGINT REFERENCES internal_consumptions (id),
    label              VARCHAR(200) NOT NULL,
    amount             NUMERIC(14, 2) NOT NULL,
    expense_date       DATE NOT NULL,
    created_date       TIMESTAMP,
    last_modified_date TIMESTAMP,
    created_by         BIGINT,
    last_modified_by   BIGINT
);
CREATE INDEX idx_dog_expense_resident ON dog_expense_links (tenant_id, resident_id);

CREATE TABLE fuel_logs (
    id                 BIGSERIAL PRIMARY KEY,
    tenant_id          BIGINT NOT NULL REFERENCES tenants (id),
    branch_id          BIGINT NOT NULL REFERENCES branches (id),
    vehicle_label      VARCHAR(120) NOT NULL,
    odometer_km        NUMERIC(12, 1) NOT NULL,
    litres             NUMERIC(12, 2) NOT NULL,
    amount             NUMERIC(14, 2) NOT NULL,
    filled_at          DATE NOT NULL,
    operating_expense_id BIGINT REFERENCES operating_expenses (id),
    created_date       TIMESTAMP,
    last_modified_date TIMESTAMP,
    created_by         BIGINT,
    last_modified_by   BIGINT
);
CREATE INDEX idx_fuel_branch ON fuel_logs (tenant_id, branch_id, filled_at);

CREATE TABLE analytics_alerts (
    id                 BIGSERIAL PRIMARY KEY,
    tenant_id          BIGINT NOT NULL REFERENCES tenants (id),
    branch_id          BIGINT NOT NULL REFERENCES branches (id),
    alert_type         VARCHAR(60) NOT NULL,
    severity           VARCHAR(20) NOT NULL,
    title              VARCHAR(200) NOT NULL,
    detail             VARCHAR(1000) NOT NULL,
    detected_at        TIMESTAMP NOT NULL,
    acknowledged       BOOLEAN NOT NULL DEFAULT FALSE,
    created_date       TIMESTAMP,
    last_modified_date TIMESTAMP,
    created_by         BIGINT,
    last_modified_by   BIGINT
);
CREATE INDEX idx_analytics_alerts_branch ON analytics_alerts (tenant_id, branch_id, detected_at DESC);
