ALTER TABLE internal_consumptions
    ADD COLUMN taken_by_user_id BIGINT REFERENCES users (id);

UPDATE internal_consumptions
SET taken_by_user_id = created_by
WHERE taken_by_user_id IS NULL
  AND created_by IS NOT NULL;

CREATE INDEX idx_consumptions_taken_by ON internal_consumptions (tenant_id, taken_by_user_id);

ALTER TABLE internal_consumption_items
    ADD COLUMN taken_by_user_id BIGINT REFERENCES users (id);

UPDATE internal_consumption_items i
SET taken_by_user_id = c.taken_by_user_id
FROM internal_consumptions c
WHERE i.consumption_id = c.id
  AND i.taken_by_user_id IS NULL;
