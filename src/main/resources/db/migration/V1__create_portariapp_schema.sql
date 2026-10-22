CREATE TABLE condominiums (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(150) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE units (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    condominium_id UUID NOT NULL
        REFERENCES condominiums(id),
    identifier VARCHAR(30) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT uq_units_condominium_identifier
        UNIQUE (condominium_id, identifier),

    CONSTRAINT uq_units_id_condominium
        UNIQUE (id, condominium_id)
);

CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(254) NOT NULL UNIQUE,
    password_hash TEXT,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE residents (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    condominium_id UUID NOT NULL
        REFERENCES condominiums(id),
    unit_id UUID NOT NULL,
    user_id UUID UNIQUE REFERENCES users(id),
    full_name VARCHAR(150) NOT NULL,
    phone VARCHAR(30),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT fk_residents_unit
        FOREIGN KEY (unit_id, condominium_id)
        REFERENCES units (id, condominium_id),

    CONSTRAINT uq_residents_id_condominium
        UNIQUE (id, condominium_id)
);

CREATE TABLE staff_members (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    condominium_id UUID NOT NULL
        REFERENCES condominiums(id),
    user_id UUID UNIQUE REFERENCES users(id),
    full_name VARCHAR(150) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT uq_staff_id_condominium
        UNIQUE (id, condominium_id)
);

CREATE TABLE deliveries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    condominium_id UUID NOT NULL
        REFERENCES condominiums(id),

    recipient_resident_id UUID NOT NULL,

    received_by_staff_id UUID NOT NULL,

    status VARCHAR(20) NOT NULL DEFAULT 'RECEIVED',

    description TEXT,
  
    received_at TIMESTAMPTZ NOT NULL DEFAULT now(),

    picked_up_at TIMESTAMPTZ,
    picked_up_by_staff_id UUID,
    picked_up_by_name VARCHAR(150),

    notes TEXT,

    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT ck_deliveries_status
        CHECK (status IN (
            'RECEIVED',
            'PICKED_UP',
            'RETURNED',
            'LOST',
            'CANCELLED'
        )),

    CONSTRAINT fk_deliveries_recipient
        FOREIGN KEY (recipient_resident_id, condominium_id)
        REFERENCES residents (id, condominium_id),

    CONSTRAINT fk_deliveries_received_by
        FOREIGN KEY (received_by_staff_id, condominium_id)
        REFERENCES staff_members (id, condominium_id),

    CONSTRAINT fk_deliveries_picked_up_by
        FOREIGN KEY (picked_up_by_staff_id, condominium_id)
        REFERENCES staff_members (id, condominium_id),

    CONSTRAINT ck_deliveries_pickup
        CHECK (
            (
                status <> 'PICKED_UP'
                OR (
                    picked_up_at IS NOT NULL
                    AND picked_up_by_staff_id IS NOT NULL
                    AND picked_up_by_name IS NOT NULL
                )
            )
        ),

    CONSTRAINT uq_deliveries_id_condominium
        UNIQUE (id, condominium_id)
);

CREATE INDEX idx_deliveries_condominium_received_at
    ON deliveries (condominium_id, received_at DESC);

CREATE TABLE delivery_evidences (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    condominium_id UUID NOT NULL,
    delivery_id UUID NOT NULL,

    uploaded_by_staff_id UUID NOT NULL,

    storage_file_key VARCHAR(500) NOT NULL,
    original_filename VARCHAR(255),
    content_type VARCHAR(100) NOT NULL,
    size_bytes BIGINT NOT NULL,

    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT fk_delivery_evidences_delivery
        FOREIGN KEY (delivery_id, condominium_id)
        REFERENCES deliveries (id, condominium_id),

    CONSTRAINT fk_delivery_evidences_staff
        FOREIGN KEY (uploaded_by_staff_id, condominium_id)
        REFERENCES staff_members (id, condominium_id),

    CONSTRAINT ck_delivery_evidences_size
        CHECK (size_bytes > 0),

    CONSTRAINT ck_delivery_evidences_content_type
        CHECK (content_type IN (
            'image/jpeg',
            'image/png',
            'image/webp',
            'image/heic'
        ))
);

CREATE INDEX idx_delivery_evidences_delivery
    ON delivery_evidences (
        condominium_id,
        delivery_id,
        created_at
);
