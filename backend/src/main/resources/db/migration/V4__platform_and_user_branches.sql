UPDATE users SET role = 'NGO_ADMIN' WHERE role = 'SUPER_ADMIN';

CREATE TABLE user_branches (
    id                 BIGSERIAL PRIMARY KEY,
    tenant_id          BIGINT NOT NULL REFERENCES tenants (id),
    user_id            BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    branch_id          BIGINT NOT NULL REFERENCES branches (id) ON DELETE CASCADE,
    created_date       TIMESTAMP,
    last_modified_date TIMESTAMP,
    created_by         BIGINT,
    last_modified_by   BIGINT,
    UNIQUE (user_id, branch_id)
);
CREATE INDEX idx_user_branches_branch ON user_branches (tenant_id, branch_id);

INSERT INTO user_branches (tenant_id, user_id, branch_id)
SELECT u.tenant_id, u.id, u.branch_id
FROM users u
WHERE u.branch_id IS NOT NULL
ON CONFLICT (user_id, branch_id) DO NOTHING;

INSERT INTO user_branches (tenant_id, user_id, branch_id)
SELECT u.tenant_id, u.id, b.id
FROM users u
JOIN branches b ON b.tenant_id = u.tenant_id
WHERE u.role = 'NGO_ADMIN'
ON CONFLICT (user_id, branch_id) DO NOTHING;
