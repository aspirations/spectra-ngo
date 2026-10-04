-- Per-branch quantity for simple (non lot-tracked) products. Lot-tracked stock already lives in stock_batches per warehouse.
CREATE TABLE branch_stock (
    id                  BIGSERIAL PRIMARY KEY,
    tenant_id           BIGINT         NOT NULL REFERENCES tenants (id),
    warehouse_id        BIGINT         NOT NULL REFERENCES warehouses (id),
    shelter_product_id  BIGINT         NOT NULL REFERENCES shelter_products (id),
    qty_on_hand         NUMERIC(14, 3) NOT NULL DEFAULT 0,
    created_date        TIMESTAMP,
    last_modified_date  TIMESTAMP,
    created_by          BIGINT,
    last_modified_by    BIGINT,
    UNIQUE (warehouse_id, shelter_product_id)
);
CREATE INDEX idx_branch_stock_tenant ON branch_stock (tenant_id);

-- Existing shared quantities move to each tenant's oldest branch so current numbers are preserved.
INSERT INTO branch_stock (tenant_id, warehouse_id, shelter_product_id, qty_on_hand, created_date)
SELECT p.tenant_id, w.id, p.id, p.qty_on_hand, now()
FROM shelter_products p
JOIN LATERAL (
    SELECT wh.id
    FROM warehouses wh
    WHERE wh.tenant_id = p.tenant_id AND wh.type = 'SHELTER_STORE'
    ORDER BY wh.branch_id
    LIMIT 1
) w ON TRUE
WHERE p.lot_tracked IS NOT TRUE
  AND p.qty_on_hand <> 0;
