ALTER TABLE staff_members ADD COLUMN role VARCHAR(20);

UPDATE staff_members SET role = 'DOORMAN' WHERE role IS NULL;

ALTER TABLE staff_members
    ALTER COLUMN role SET NOT NULL,
    ADD CONSTRAINT ck_staff_members_role CHECK (role IN ('DOORMAN', 'ADMIN'));
