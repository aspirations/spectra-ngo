-- Salary advances, payroll lines for advances/reimbursements, OpEx recovery link

CREATE TABLE salary_advances (
    id                 BIGSERIAL PRIMARY KEY,
    tenant_id          BIGINT NOT NULL REFERENCES tenants (id),
    branch_id          BIGINT NOT NULL REFERENCES branches (id),
    user_id            BIGINT NOT NULL REFERENCES users (id),
    amount             NUMERIC(12, 2) NOT NULL,
    advanced_at        DATE NOT NULL,
    notes              VARCHAR(500),
    status             VARCHAR(40) NOT NULL,
    payroll_run_id     BIGINT REFERENCES payroll_runs (id),
    created_date       TIMESTAMP,
    last_modified_date TIMESTAMP,
    created_by         BIGINT,
    last_modified_by   BIGINT
);
CREATE INDEX idx_salary_advances_user ON salary_advances (tenant_id, user_id, status);

ALTER TABLE payroll_items
    ADD COLUMN advance_deduction NUMERIC(12, 2) NOT NULL DEFAULT 0,
    ADD COLUMN reimbursement_credit NUMERIC(12, 2) NOT NULL DEFAULT 0;

ALTER TABLE operating_expenses
    ADD COLUMN reimbursed_in_payroll_item_id BIGINT REFERENCES payroll_items (id);
