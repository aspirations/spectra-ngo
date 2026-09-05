CREATE SEQUENCE spectra_staff_purchase_seq START 1;

ALTER TABLE staff_store_products
    ADD COLUMN unit_cost NUMERIC(12, 2) NOT NULL DEFAULT 0;

ALTER TABLE staff_pos_order_items
    ADD COLUMN unit_cost NUMERIC(12, 2) NOT NULL DEFAULT 0,
    ADD COLUMN line_cost NUMERIC(12, 2) NOT NULL DEFAULT 0;

UPDATE staff_store_products
SET unit_cost = ROUND(unit_price * 0.80, 2)
WHERE unit_cost = 0 AND qty_on_hand > 0;

UPDATE staff_pos_order_items i
SET unit_cost = p.unit_cost,
    line_cost = ROUND(i.qty * p.unit_cost, 2)
FROM staff_store_products p
WHERE i.product_id = p.id;

CREATE TABLE staff_store_purchases (
    id                 BIGSERIAL PRIMARY KEY,
    tenant_id          BIGINT         NOT NULL REFERENCES tenants (id),
    branch_id          BIGINT         NOT NULL REFERENCES branches (id),
    warehouse_id       BIGINT         NOT NULL REFERENCES warehouses (id),
    purchase_number    VARCHAR(64)    NOT NULL,
    supplier_name      VARCHAR(255),
    total_amount       NUMERIC(14, 2) NOT NULL,
    purchased_at       TIMESTAMP      NOT NULL,
    notes              TEXT,
    created_date       TIMESTAMP,
    last_modified_date TIMESTAMP,
    created_by         BIGINT,
    last_modified_by   BIGINT,
    UNIQUE (tenant_id, purchase_number)
);
CREATE INDEX idx_staff_purchases_branch ON staff_store_purchases (tenant_id, branch_id, purchased_at);

CREATE TABLE staff_store_purchase_items (
    id                 BIGSERIAL PRIMARY KEY,
    tenant_id          BIGINT         NOT NULL REFERENCES tenants (id),
    purchase_id        BIGINT         NOT NULL REFERENCES staff_store_purchases (id) ON DELETE CASCADE,
    product_id         BIGINT         NOT NULL REFERENCES staff_store_products (id),
    qty                NUMERIC(12, 3) NOT NULL,
    unit_cost          NUMERIC(12, 2) NOT NULL,
    line_total         NUMERIC(12, 2) NOT NULL,
    created_date       TIMESTAMP,
    last_modified_date TIMESTAMP,
    created_by         BIGINT,
    last_modified_by   BIGINT
);
CREATE INDEX idx_staff_purchase_items_purchase ON staff_store_purchase_items (purchase_id);
