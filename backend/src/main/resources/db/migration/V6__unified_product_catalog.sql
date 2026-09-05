-- One product master: lot-tracked SKUs keep qty in stock_batches; simple SKUs keep qty on the product.

ALTER TABLE shelter_products
    ADD COLUMN IF NOT EXISTS lot_tracked BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS qty_on_hand NUMERIC(14, 3) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS unit_price NUMERIC(12, 2) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS unit_cost NUMERIC(12, 2) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS staff_sale BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS clinical_use BOOLEAN NOT NULL DEFAULT TRUE;

UPDATE shelter_products
SET lot_tracked = TRUE,
    clinical_use = TRUE,
    staff_sale = FALSE
WHERE staff_sale IS FALSE;

ALTER TABLE purchase_grn_items
    ALTER COLUMN batch_number DROP NOT NULL,
    ALTER COLUMN expiry_date DROP NOT NULL;

ALTER TABLE internal_consumption_items
    ALTER COLUMN batch_id DROP NOT NULL,
    ADD COLUMN IF NOT EXISTS cost_center VARCHAR(32);

CREATE TABLE IF NOT EXISTS staff_product_unify_map (
    old_staff_id BIGINT PRIMARY KEY,
    new_product_id BIGINT NOT NULL
);

INSERT INTO shelter_products (
    tenant_id, sku, name, unit, category, barcode, vaccine_interval_days, reorder_level, active,
    lot_tracked, qty_on_hand, unit_price, unit_cost, staff_sale, clinical_use,
    created_date, last_modified_date, created_by, last_modified_by
)
SELECT ssp.tenant_id,
       CASE
           WHEN EXISTS (
               SELECT 1 FROM shelter_products sp
               WHERE sp.tenant_id = ssp.tenant_id AND lower(sp.sku) = lower(ssp.sku)
           ) THEN ssp.sku || '-STAFF-' || ssp.id
           ELSE ssp.sku
       END,
       ssp.name,
       ssp.unit,
       'STAFF_RETAIL',
       ssp.barcode,
       NULL,
       0,
       ssp.active,
       FALSE,
       ssp.qty_on_hand,
       ssp.unit_price,
       COALESCE(ssp.unit_cost, 0),
       TRUE,
       FALSE,
       ssp.created_date,
       ssp.last_modified_date,
       ssp.created_by,
       ssp.last_modified_by
FROM staff_store_products ssp;

INSERT INTO staff_product_unify_map (old_staff_id, new_product_id)
SELECT ssp.id, sp.id
FROM staff_store_products ssp
JOIN shelter_products sp
  ON sp.tenant_id = ssp.tenant_id
 AND sp.staff_sale = TRUE
 AND (
        sp.sku = ssp.sku
     OR sp.sku = ssp.sku || '-STAFF-' || ssp.id
 );

ALTER TABLE staff_pos_order_items DROP CONSTRAINT IF EXISTS staff_pos_order_items_product_id_fkey;
ALTER TABLE staff_store_purchase_items DROP CONSTRAINT IF EXISTS staff_store_purchase_items_product_id_fkey;

UPDATE staff_pos_order_items i
SET product_id = m.new_product_id
FROM staff_product_unify_map m
WHERE i.product_id = m.old_staff_id;

UPDATE staff_store_purchase_items i
SET product_id = m.new_product_id
FROM staff_product_unify_map m
WHERE i.product_id = m.old_staff_id;

ALTER TABLE staff_pos_order_items
    ADD CONSTRAINT staff_pos_order_items_product_id_fkey
        FOREIGN KEY (product_id) REFERENCES shelter_products (id);

ALTER TABLE staff_store_purchase_items
    ADD CONSTRAINT staff_store_purchase_items_product_id_fkey
        FOREIGN KEY (product_id) REFERENCES shelter_products (id);

DROP TABLE IF EXISTS staff_product_unify_map;
