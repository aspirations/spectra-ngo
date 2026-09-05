-- Email uniquely identifies a user (and therefore their NGO). Login no longer needs a tenant code.
ALTER TABLE users DROP CONSTRAINT IF EXISTS users_tenant_id_email_key;
ALTER TABLE users DROP CONSTRAINT IF EXISTS users_email_key;
CREATE UNIQUE INDEX IF NOT EXISTS users_email_lower_key ON users (lower(email));

ALTER TABLE dog_profiles RENAME TO residents;
ALTER TABLE residents RENAME COLUMN tag_or_microchip_id TO reference_id;
ALTER TABLE residents ADD COLUMN kind VARCHAR(32) NOT NULL DEFAULT 'DOG';
UPDATE residents SET category = 'ADULT' WHERE category = 'ADULT_KENNEL';
UPDATE residents SET category = 'JUVENILE' WHERE category = 'PUPPY_NURSERY';
UPDATE residents SET category = 'POST_OP' WHERE category = 'POST_OP_WARD';
UPDATE residents SET category = 'CRITICAL' WHERE category = 'CRITICAL_ISOLATION';
ALTER INDEX IF EXISTS idx_dogs_tenant_branch RENAME TO idx_residents_tenant_branch;
ALTER INDEX IF EXISTS idx_dogs_status RENAME TO idx_residents_status;

ALTER TABLE dog_case_notes RENAME TO resident_notes;
ALTER TABLE resident_notes RENAME COLUMN dog_id TO resident_id;
ALTER INDEX IF EXISTS idx_dog_notes_dog RENAME TO idx_resident_notes_resident;

ALTER TABLE dog_vaccinations RENAME TO resident_treatments;
ALTER TABLE resident_treatments RENAME COLUMN dog_id TO resident_id;
ALTER INDEX IF EXISTS idx_vaccinations_due RENAME TO idx_treatments_due;

ALTER TABLE tenant_settings RENAME COLUMN puppy_feed_grams_per_day TO juvenile_feed_grams_per_day;
