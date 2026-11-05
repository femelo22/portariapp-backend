ALTER TABLE condominiums
    ADD CONSTRAINT uq_condominiums_name UNIQUE (name);
