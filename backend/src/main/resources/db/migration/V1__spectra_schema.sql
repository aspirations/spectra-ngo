-- Spectra NGO v1 schema. Tenant-scoped tables carry tenant_id for Hibernate @TenantId.

CREATE SEQUENCE spectra_grn_seq START 1;
CREATE SEQUENCE spectra_pos_seq START 1;

CREATE TABLE tenants (
    id                  BIGSERIAL PRIMARY KEY,
    name                VARCHAR(255) NOT NULL,
    code                VARCHAR(50)  NOT NULL UNIQUE,
    timezone            VARCHAR(64)  NOT NULL DEFAULT 'Asia/Kolkata',
    currency            VARCHAR(8)   NOT NULL DEFAULT 'INR',
    status              VARCHAR(32)  NOT NULL DEFAULT 'ACTIVE',
    created_date        TIMESTAMP,
    last_modified_date  TIMESTAMP,
    created_by          BIGINT,
    last_modified_by    BIGINT
);

CREATE TABLE branches (
    id                  BIGSERIAL PRIMARY KEY,
    tenant_id           BIGINT       NOT NULL REFERENCES tenants (id),
    name                VARCHAR(255) NOT NULL,
    code                VARCHAR(50)  NOT NULL,
    address             VARCHAR(500),
    city                VARCHAR(100),
    status              VARCHAR(32)  NOT NULL DEFAULT 'ACTIVE',
    created_date        TIMESTAMP,
    last_modified_date  TIMESTAMP,
    created_by          BIGINT,
    last_modified_by    BIGINT,
    UNIQUE (tenant_id, code)
);
CREATE INDEX idx_branches_tenant ON branches (tenant_id);

CREATE TABLE warehouses (
    id                  BIGSERIAL PRIMARY KEY,
    tenant_id           BIGINT       NOT NULL REFERENCES tenants (id),
    branch_id           BIGINT       NOT NULL REFERENCES branches (id),
    type                VARCHAR(32)  NOT NULL,
    name                VARCHAR(255) NOT NULL,
    created_date        TIMESTAMP,
    last_modified_date  TIMESTAMP,
    created_by          BIGINT,
    last_modified_by    BIGINT,
    UNIQUE (branch_id, type)
);
CREATE INDEX idx_warehouses_tenant_branch ON warehouses (tenant_id, branch_id);

CREATE TABLE users (
    id                      BIGSERIAL PRIMARY KEY,
    tenant_id               BIGINT         NOT NULL REFERENCES tenants (id),
    branch_id               BIGINT         REFERENCES branches (id),
    email                   VARCHAR(255)   NOT NULL,
    password_hash           VARCHAR(255)   NOT NULL,
    full_name               VARCHAR(255)   NOT NULL,
    role                    VARCHAR(32)    NOT NULL,
    credit_limit            NUMERIC(12, 2) NOT NULL DEFAULT 0,
    base_monthly_salary     NUMERIC(12, 2) NOT NULL DEFAULT 0,
    other_fixed_deductions  NUMERIC(12, 2) NOT NULL DEFAULT 0,
    rolled_over_store_debt  NUMERIC(12, 2) NOT NULL DEFAULT 0,
    status                  VARCHAR(32)    NOT NULL DEFAULT 'ACTIVE',
    created_date            TIMESTAMP,
    last_modified_date      TIMESTAMP,
    created_by              BIGINT,
    last_modified_by        BIGINT,
    UNIQUE (tenant_id, email)
);
CREATE INDEX idx_users_tenant_branch ON users (tenant_id, branch_id);

CREATE TABLE tenant_settings (
    id                              BIGSERIAL PRIMARY KEY,
    tenant_id                       BIGINT      NOT NULL UNIQUE REFERENCES tenants (id),
    adult_feed_grams_per_day        INT         NOT NULL DEFAULT 400,
    puppy_feed_grams_per_day        INT         NOT NULL DEFAULT 200,
    post_op_feed_grams_per_day      INT         NOT NULL DEFAULT 400,
    isolation_feed_grams_per_day    INT         NOT NULL DEFAULT 400,
    working_days_mode               VARCHAR(32) NOT NULL DEFAULT 'EXCLUDE_SUNDAYS',
    default_vaccine_interval_days   INT         NOT NULL DEFAULT 365,
    created_date                    TIMESTAMP,
    last_modified_date              TIMESTAMP,
    created_by                      BIGINT,
    last_modified_by                BIGINT
);

CREATE TABLE dog_profiles (
    id                  BIGSERIAL PRIMARY KEY,
    tenant_id           BIGINT       NOT NULL REFERENCES tenants (id),
    branch_id           BIGINT       NOT NULL REFERENCES branches (id),
    tag_or_microchip_id VARCHAR(100) NOT NULL,
    name                VARCHAR(255) NOT NULL,
    photo_url           VARCHAR(1000),
    category            VARCHAR(32)  NOT NULL,
    intake_date         DATE         NOT NULL,
    status              VARCHAR(32)  NOT NULL DEFAULT 'ACTIVE',
    created_date        TIMESTAMP,
    last_modified_date  TIMESTAMP,
    created_by          BIGINT,
    last_modified_by    BIGINT,
    UNIQUE (tenant_id, tag_or_microchip_id)
);
CREATE INDEX idx_dogs_tenant_branch ON dog_profiles (tenant_id, branch_id);
CREATE INDEX idx_dogs_status ON dog_profiles (tenant_id, branch_id, status);

CREATE TABLE dog_case_notes (
    id                  BIGSERIAL PRIMARY KEY,
    tenant_id           BIGINT      NOT NULL REFERENCES tenants (id),
    dog_id              BIGINT      NOT NULL REFERENCES dog_profiles (id) ON DELETE CASCADE,
    note_text           TEXT        NOT NULL,
    note_type           VARCHAR(32) NOT NULL,
    created_date        TIMESTAMP,
    last_modified_date  TIMESTAMP,
    created_by          BIGINT,
    last_modified_by    BIGINT
);
CREATE INDEX idx_dog_notes_dog ON dog_case_notes (dog_id, created_date DESC);

CREATE TABLE shelter_products (
    id                      BIGSERIAL PRIMARY KEY,
    tenant_id               BIGINT         NOT NULL REFERENCES tenants (id),
    sku                     VARCHAR(64)    NOT NULL,
    name                    VARCHAR(255)   NOT NULL,
    unit                    VARCHAR(32)    NOT NULL,
    category                VARCHAR(32)    NOT NULL,
    barcode                 VARCHAR(128),
    vaccine_interval_days   INT,
    reorder_level           NUMERIC(14, 3) NOT NULL DEFAULT 0,
    active                  BOOLEAN        NOT NULL DEFAULT TRUE,
    created_date            TIMESTAMP,
    last_modified_date      TIMESTAMP,
    created_by              BIGINT,
    last_modified_by        BIGINT,
    UNIQUE (tenant_id, sku)
);
CREATE INDEX idx_shelter_products_barcode ON shelter_products (tenant_id, barcode);

CREATE TABLE suppliers (
    id                  BIGSERIAL PRIMARY KEY,
    tenant_id           BIGINT         NOT NULL REFERENCES tenants (id),
    branch_id           BIGINT         NOT NULL REFERENCES branches (id),
    name                VARCHAR(255)   NOT NULL,
    contact_phone       VARCHAR(50),
    contact_email       VARCHAR(255),
    payable_balance     NUMERIC(14, 2) NOT NULL DEFAULT 0,
    created_date        TIMESTAMP,
    last_modified_date  TIMESTAMP,
    created_by          BIGINT,
    last_modified_by    BIGINT
);
CREATE INDEX idx_suppliers_tenant_branch ON suppliers (tenant_id, branch_id);

CREATE TABLE purchase_grns (
    id                  BIGSERIAL PRIMARY KEY,
    tenant_id           BIGINT         NOT NULL REFERENCES tenants (id),
    branch_id           BIGINT         NOT NULL REFERENCES branches (id),
    warehouse_id        BIGINT         NOT NULL REFERENCES warehouses (id),
    supplier_id         BIGINT         NOT NULL REFERENCES suppliers (id),
    grn_number          VARCHAR(64)    NOT NULL,
    received_at         TIMESTAMP      NOT NULL,
    status              VARCHAR(32)    NOT NULL DEFAULT 'RECEIVED',
    total_amount        NUMERIC(14, 2) NOT NULL DEFAULT 0,
    notes               TEXT,
    created_date        TIMESTAMP,
    last_modified_date  TIMESTAMP,
    created_by          BIGINT,
    last_modified_by    BIGINT,
    UNIQUE (tenant_id, grn_number)
);
CREATE INDEX idx_grn_tenant_branch ON purchase_grns (tenant_id, branch_id);

CREATE TABLE purchase_grn_items (
    id                  BIGSERIAL PRIMARY KEY,
    tenant_id           BIGINT         NOT NULL REFERENCES tenants (id),
    grn_id              BIGINT         NOT NULL REFERENCES purchase_grns (id) ON DELETE CASCADE,
    shelter_product_id  BIGINT         NOT NULL REFERENCES shelter_products (id),
    batch_number        VARCHAR(64)    NOT NULL,
    expiry_date         DATE           NOT NULL,
    quantity            NUMERIC(14, 3) NOT NULL,
    unit_landed_cost    NUMERIC(14, 4) NOT NULL,
    line_total          NUMERIC(14, 2) NOT NULL,
    created_date        TIMESTAMP,
    last_modified_date  TIMESTAMP,
    created_by          BIGINT,
    last_modified_by    BIGINT
);

CREATE TABLE stock_batches (
    id                  BIGSERIAL PRIMARY KEY,
    tenant_id           BIGINT         NOT NULL REFERENCES tenants (id),
    warehouse_id        BIGINT         NOT NULL REFERENCES warehouses (id),
    shelter_product_id  BIGINT         NOT NULL REFERENCES shelter_products (id),
    grn_item_id         BIGINT         REFERENCES purchase_grn_items (id),
    batch_number        VARCHAR(64)    NOT NULL,
    expiry_date         DATE           NOT NULL,
    qty_on_hand         NUMERIC(14, 3) NOT NULL,
    unit_landed_cost    NUMERIC(14, 4) NOT NULL,
    received_at         TIMESTAMP      NOT NULL,
    created_date        TIMESTAMP,
    last_modified_date  TIMESTAMP,
    created_by          BIGINT,
    last_modified_by    BIGINT
);
CREATE INDEX idx_batches_fefo ON stock_batches (warehouse_id, shelter_product_id, expiry_date, received_at);

CREATE TABLE internal_consumptions (
    id                  BIGSERIAL PRIMARY KEY,
    tenant_id           BIGINT         NOT NULL REFERENCES tenants (id),
    branch_id           BIGINT         NOT NULL REFERENCES branches (id),
    warehouse_id        BIGINT         NOT NULL REFERENCES warehouses (id),
    consumption_date    DATE           NOT NULL,
    type                VARCHAR(32)    NOT NULL,
    theoretical_qty     NUMERIC(14, 3),
    actual_qty          NUMERIC(14, 3),
    variance_pct        NUMERIC(10, 4),
    variance_alert      BOOLEAN        NOT NULL DEFAULT FALSE,
    notes               TEXT,
    created_date        TIMESTAMP,
    last_modified_date  TIMESTAMP,
    created_by          BIGINT,
    last_modified_by    BIGINT
);
CREATE INDEX idx_consumptions_tenant_branch ON internal_consumptions (tenant_id, branch_id, consumption_date);

CREATE TABLE internal_consumption_items (
    id                  BIGSERIAL PRIMARY KEY,
    tenant_id           BIGINT         NOT NULL REFERENCES tenants (id),
    consumption_id      BIGINT         NOT NULL REFERENCES internal_consumptions (id) ON DELETE CASCADE,
    shelter_product_id  BIGINT         NOT NULL REFERENCES shelter_products (id),
    batch_id            BIGINT         NOT NULL REFERENCES stock_batches (id),
    qty                 NUMERIC(14, 3) NOT NULL,
    unit_cost           NUMERIC(14, 4) NOT NULL,
    created_date        TIMESTAMP,
    last_modified_date  TIMESTAMP,
    created_by          BIGINT,
    last_modified_by    BIGINT
);

CREATE TABLE dog_vaccinations (
    id                      BIGSERIAL PRIMARY KEY,
    tenant_id               BIGINT         NOT NULL REFERENCES tenants (id),
    dog_id                  BIGINT         NOT NULL REFERENCES dog_profiles (id) ON DELETE CASCADE,
    shelter_product_id      BIGINT         NOT NULL REFERENCES shelter_products (id),
    consumption_item_id     BIGINT         REFERENCES internal_consumption_items (id),
    administered_at         TIMESTAMP      NOT NULL,
    next_due_date           DATE,
    quantity                NUMERIC(12, 3) NOT NULL,
    notes                   TEXT,
    created_date            TIMESTAMP,
    last_modified_date      TIMESTAMP,
    created_by              BIGINT,
    last_modified_by        BIGINT
);
CREATE INDEX idx_vaccinations_due ON dog_vaccinations (tenant_id, next_due_date);

CREATE TABLE inventory_audit_logs (
    id                  BIGSERIAL PRIMARY KEY,
    tenant_id           BIGINT         NOT NULL REFERENCES tenants (id),
    branch_id           BIGINT         NOT NULL REFERENCES branches (id),
    warehouse_id        BIGINT         NOT NULL REFERENCES warehouses (id),
    shelter_product_id  BIGINT         NOT NULL REFERENCES shelter_products (id),
    batch_id            BIGINT         REFERENCES stock_batches (id),
    system_qty          NUMERIC(14, 3) NOT NULL,
    physical_qty        NUMERIC(14, 3) NOT NULL,
    reason              VARCHAR(32)    NOT NULL,
    status              VARCHAR(32)    NOT NULL DEFAULT 'FLAGGED_UNCERTAIN',
    notes               TEXT,
    reviewed_by         BIGINT,
    reviewed_at         TIMESTAMP,
    created_date        TIMESTAMP,
    last_modified_date  TIMESTAMP,
    created_by          BIGINT,
    last_modified_by    BIGINT
);
CREATE INDEX idx_audits_tenant_branch ON inventory_audit_logs (tenant_id, branch_id, status);

CREATE TABLE supplier_payments (
    id                  BIGSERIAL PRIMARY KEY,
    tenant_id           BIGINT         NOT NULL REFERENCES tenants (id),
    branch_id           BIGINT         NOT NULL REFERENCES branches (id),
    supplier_id         BIGINT         NOT NULL REFERENCES suppliers (id),
    amount              NUMERIC(14, 2) NOT NULL,
    paid_at             TIMESTAMP      NOT NULL,
    notes               TEXT,
    created_date        TIMESTAMP,
    last_modified_date  TIMESTAMP,
    created_by          BIGINT,
    last_modified_by    BIGINT
);

CREATE TABLE staff_store_products (
    id                  BIGSERIAL PRIMARY KEY,
    tenant_id           BIGINT         NOT NULL REFERENCES tenants (id),
    branch_id           BIGINT         NOT NULL REFERENCES branches (id),
    warehouse_id        BIGINT         NOT NULL REFERENCES warehouses (id),
    sku                 VARCHAR(64)    NOT NULL,
    name                VARCHAR(255)   NOT NULL,
    unit                VARCHAR(32)    NOT NULL DEFAULT 'PIECE',
    unit_price          NUMERIC(12, 2) NOT NULL,
    barcode             VARCHAR(128),
    qty_on_hand         NUMERIC(14, 3) NOT NULL DEFAULT 0,
    active              BOOLEAN        NOT NULL DEFAULT TRUE,
    created_date        TIMESTAMP,
    last_modified_date  TIMESTAMP,
    created_by          BIGINT,
    last_modified_by    BIGINT,
    UNIQUE (tenant_id, branch_id, sku)
);
CREATE INDEX idx_staff_products_barcode ON staff_store_products (tenant_id, branch_id, barcode);

CREATE TABLE staff_pos_orders (
    id                  BIGSERIAL PRIMARY KEY,
    tenant_id           BIGINT         NOT NULL REFERENCES tenants (id),
    branch_id           BIGINT         NOT NULL REFERENCES branches (id),
    employee_id         BIGINT         NOT NULL REFERENCES users (id),
    cashier_id          BIGINT         NOT NULL REFERENCES users (id),
    order_number        VARCHAR(64)    NOT NULL,
    tender              VARCHAR(32)    NOT NULL,
    status              VARCHAR(32)    NOT NULL,
    total               NUMERIC(12, 2) NOT NULL,
    payroll_run_id      BIGINT,
    order_at            TIMESTAMP      NOT NULL,
    created_date        TIMESTAMP,
    last_modified_date  TIMESTAMP,
    created_by          BIGINT,
    last_modified_by    BIGINT,
    UNIQUE (tenant_id, order_number)
);
CREATE INDEX idx_pos_employee_month ON staff_pos_orders (employee_id, tender, status, order_at);

CREATE TABLE staff_pos_order_items (
    id                  BIGSERIAL PRIMARY KEY,
    tenant_id           BIGINT         NOT NULL REFERENCES tenants (id),
    order_id            BIGINT         NOT NULL REFERENCES staff_pos_orders (id) ON DELETE CASCADE,
    product_id          BIGINT         NOT NULL REFERENCES staff_store_products (id),
    qty                 NUMERIC(12, 3) NOT NULL,
    unit_price          NUMERIC(12, 2) NOT NULL,
    line_total          NUMERIC(12, 2) NOT NULL,
    created_date        TIMESTAMP,
    last_modified_date  TIMESTAMP,
    created_by          BIGINT,
    last_modified_by    BIGINT
);

CREATE TABLE attendance_records (
    id                  BIGSERIAL PRIMARY KEY,
    tenant_id           BIGINT      NOT NULL REFERENCES tenants (id),
    branch_id           BIGINT      NOT NULL REFERENCES branches (id),
    user_id             BIGINT      NOT NULL REFERENCES users (id),
    work_date           DATE        NOT NULL,
    status              VARCHAR(32) NOT NULL,
    marked_by           BIGINT,
    created_date        TIMESTAMP,
    last_modified_date  TIMESTAMP,
    created_by          BIGINT,
    last_modified_by    BIGINT,
    UNIQUE (user_id, work_date)
);
CREATE INDEX idx_attendance_branch_date ON attendance_records (tenant_id, branch_id, work_date);

CREATE TABLE leave_requests (
    id                  BIGSERIAL PRIMARY KEY,
    tenant_id           BIGINT        NOT NULL REFERENCES tenants (id),
    branch_id           BIGINT        NOT NULL REFERENCES branches (id),
    user_id             BIGINT        NOT NULL REFERENCES users (id),
    type                VARCHAR(32)   NOT NULL,
    from_date           DATE          NOT NULL,
    to_date             DATE          NOT NULL,
    days                NUMERIC(6, 1) NOT NULL,
    status              VARCHAR(32)   NOT NULL DEFAULT 'PENDING',
    reason              TEXT,
    reviewed_by         BIGINT,
    created_date        TIMESTAMP,
    last_modified_date  TIMESTAMP,
    created_by          BIGINT,
    last_modified_by    BIGINT
);
CREATE INDEX idx_leave_tenant_branch ON leave_requests (tenant_id, branch_id, status);

CREATE TABLE payroll_runs (
    id                  BIGSERIAL PRIMARY KEY,
    tenant_id           BIGINT      NOT NULL REFERENCES tenants (id),
    branch_id           BIGINT      NOT NULL REFERENCES branches (id),
    period_year         INT         NOT NULL,
    period_month        INT         NOT NULL,
    status              VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
    approved_by         BIGINT,
    disbursed_by        BIGINT,
    disbursed_at        TIMESTAMP,
    created_date        TIMESTAMP,
    last_modified_date  TIMESTAMP,
    created_by          BIGINT,
    last_modified_by    BIGINT,
    UNIQUE (tenant_id, branch_id, period_year, period_month)
);

CREATE TABLE payroll_items (
    id                      BIGSERIAL PRIMARY KEY,
    tenant_id               BIGINT         NOT NULL REFERENCES tenants (id),
    payroll_run_id          BIGINT         NOT NULL REFERENCES payroll_runs (id) ON DELETE CASCADE,
    user_id                 BIGINT         NOT NULL REFERENCES users (id),
    base_salary             NUMERIC(12, 2) NOT NULL,
    working_days            INT            NOT NULL,
    unpaid_leave_days       NUMERIC(6, 1)  NOT NULL,
    lop_deduction           NUMERIC(12, 2) NOT NULL,
    store_dues              NUMERIC(12, 2) NOT NULL,
    other_deductions        NUMERIC(12, 2) NOT NULL,
    net_payout              NUMERIC(12, 2) NOT NULL,
    rolled_over_store_debt  NUMERIC(12, 2) NOT NULL DEFAULT 0,
    breakdown_json          TEXT,
    created_date            TIMESTAMP,
    last_modified_date      TIMESTAMP,
    created_by              BIGINT,
    last_modified_by        BIGINT
);
CREATE INDEX idx_payroll_items_run ON payroll_items (payroll_run_id);

ALTER TABLE staff_pos_orders
    ADD CONSTRAINT fk_pos_payroll_run FOREIGN KEY (payroll_run_id) REFERENCES payroll_runs (id);
