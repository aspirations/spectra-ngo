CREATE SEQUENCE spectra_po_seq START 1;

ALTER TABLE tenant_settings
    ADD COLUMN po_approver_role VARCHAR(32) NOT NULL DEFAULT 'BRANCH_ADMIN',
    ADD COLUMN po_approver_user_id BIGINT REFERENCES users (id),
    ADD COLUMN po_qty_tolerance_pct NUMERIC(8, 4) NOT NULL DEFAULT 0,
    ADD COLUMN po_cost_tolerance_pct NUMERIC(8, 4) NOT NULL DEFAULT 0,
    ADD COLUMN po_notify_on_submit BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN po_notify_on_approve BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN po_notify_on_variance BOOLEAN NOT NULL DEFAULT TRUE;

CREATE TABLE purchase_orders (
    id                  BIGSERIAL PRIMARY KEY,
    tenant_id           BIGINT         NOT NULL REFERENCES tenants (id),
    branch_id           BIGINT         NOT NULL REFERENCES branches (id),
    supplier_id         BIGINT         NOT NULL REFERENCES suppliers (id),
    po_number           VARCHAR(64)    NOT NULL,
    status              VARCHAR(32)    NOT NULL,
    requested_by_user_id BIGINT        NOT NULL REFERENCES users (id),
    submitted_at        TIMESTAMP,
    approver_role       VARCHAR(32),
    approver_user_id    BIGINT REFERENCES users (id),
    approved_by_user_id BIGINT REFERENCES users (id),
    approved_at         TIMESTAMP,
    reject_reason       TEXT,
    notes               TEXT,
    expected_total      NUMERIC(14, 2) NOT NULL DEFAULT 0,
    received_total      NUMERIC(14, 2) NOT NULL DEFAULT 0,
    created_date        TIMESTAMP,
    last_modified_date  TIMESTAMP,
    created_by          BIGINT,
    last_modified_by    BIGINT,
    UNIQUE (tenant_id, po_number)
);
CREATE INDEX idx_po_tenant_branch_status ON purchase_orders (tenant_id, branch_id, status);
CREATE INDEX idx_po_approver ON purchase_orders (tenant_id, branch_id, approver_user_id, status);

CREATE TABLE purchase_order_items (
    id                  BIGSERIAL PRIMARY KEY,
    tenant_id           BIGINT         NOT NULL REFERENCES tenants (id),
    purchase_order_id   BIGINT         NOT NULL REFERENCES purchase_orders (id) ON DELETE CASCADE,
    shelter_product_id  BIGINT         NOT NULL REFERENCES shelter_products (id),
    qty_ordered         NUMERIC(14, 3) NOT NULL,
    unit_cost           NUMERIC(14, 4) NOT NULL,
    qty_received        NUMERIC(14, 3) NOT NULL DEFAULT 0,
    damaged_qty         NUMERIC(14, 3) NOT NULL DEFAULT 0,
    notes               TEXT,
    created_date        TIMESTAMP,
    last_modified_date  TIMESTAMP,
    created_by          BIGINT,
    last_modified_by    BIGINT
);
CREATE INDEX idx_po_items_po ON purchase_order_items (purchase_order_id);

ALTER TABLE purchase_grns
    ADD COLUMN purchase_order_id BIGINT REFERENCES purchase_orders (id);
CREATE INDEX idx_grn_po ON purchase_grns (purchase_order_id);

ALTER TABLE purchase_grn_items
    ADD COLUMN purchase_order_item_id BIGINT REFERENCES purchase_order_items (id),
    ADD COLUMN damaged_qty NUMERIC(14, 3) NOT NULL DEFAULT 0;

CREATE TABLE notifications (
    id                  BIGSERIAL PRIMARY KEY,
    tenant_id           BIGINT         NOT NULL REFERENCES tenants (id),
    branch_id           BIGINT         NOT NULL REFERENCES branches (id),
    user_id             BIGINT         NOT NULL REFERENCES users (id),
    type                VARCHAR(32)    NOT NULL,
    title               VARCHAR(255)   NOT NULL,
    body                TEXT           NOT NULL,
    entity_type         VARCHAR(32),
    entity_id           BIGINT,
    read_at             TIMESTAMP,
    email_status        VARCHAR(16)    NOT NULL DEFAULT 'PENDING',
    created_date        TIMESTAMP,
    last_modified_date  TIMESTAMP,
    created_by          BIGINT,
    last_modified_by    BIGINT
);
CREATE INDEX idx_notifications_user ON notifications (tenant_id, user_id, read_at, created_date DESC);
CREATE INDEX idx_notifications_branch ON notifications (tenant_id, branch_id, created_date DESC);
