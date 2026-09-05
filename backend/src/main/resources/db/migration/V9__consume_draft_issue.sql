ALTER TABLE internal_consumptions
    ADD COLUMN status VARCHAR(32) NOT NULL DEFAULT 'ISSUED',
    ADD COLUMN requested_by_user_id BIGINT REFERENCES users (id),
    ADD COLUMN issued_by_user_id BIGINT REFERENCES users (id),
    ADD COLUMN issued_at TIMESTAMP;

UPDATE internal_consumptions
SET requested_by_user_id = COALESCE(created_by, taken_by_user_id),
    issued_by_user_id = COALESCE(created_by, taken_by_user_id),
    issued_at = created_date
WHERE issued_at IS NULL;

CREATE INDEX idx_consumptions_branch_status ON internal_consumptions (branch_id, status);

ALTER TABLE internal_consumption_items
    ADD COLUMN resident_id BIGINT REFERENCES residents (id);

ALTER TABLE resident_treatments
    ADD COLUMN status VARCHAR(32) NOT NULL DEFAULT 'ISSUED',
    ADD COLUMN consumption_id BIGINT REFERENCES internal_consumptions (id);

ALTER TABLE resident_treatments
    ALTER COLUMN administered_at DROP NOT NULL;
