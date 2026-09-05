ALTER TABLE residents
    ADD COLUMN collar_no VARCHAR(64),
    ADD COLUMN description TEXT,
    ADD COLUMN sex VARCHAR(16),
    ADD COLUMN color VARCHAR(64),
    ADD COLUMN approx_age VARCHAR(64),
    ADD COLUMN intake_source VARCHAR(32);
