LOCK TABLE deliveries, delivery_evidences IN SHARE ROW EXCLUSIVE MODE;

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM deliveries) OR EXISTS (SELECT 1 FROM delivery_evidences) THEN
        RAISE EXCEPTION 'Cannot create delivery history: deliveries and delivery_evidences must both be empty. Clear existing records manually before retrying migration V3.';
    END IF;
END;
$$;

CREATE TABLE delivery_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    condominium_id UUID NOT NULL,
    delivery_id UUID NOT NULL,
    sequence_number BIGINT NOT NULL,
    previous_status VARCHAR(20),
    status VARCHAR(20) NOT NULL,
    staff_id UUID NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL,
    notes TEXT,

    CONSTRAINT fk_delivery_events_delivery
        FOREIGN KEY (delivery_id, condominium_id)
        REFERENCES deliveries (id, condominium_id),
    CONSTRAINT fk_delivery_events_staff
        FOREIGN KEY (staff_id, condominium_id)
        REFERENCES staff_members (id, condominium_id),
    CONSTRAINT uq_delivery_events_sequence
        UNIQUE (delivery_id, sequence_number),
    CONSTRAINT ck_delivery_events_sequence
        CHECK (sequence_number > 0),
    CONSTRAINT ck_delivery_events_status
        CHECK (status IN ('RECEIVED', 'PICKED_UP', 'RETURNED', 'LOST', 'CANCELLED')),
    CONSTRAINT ck_delivery_events_previous_status
        CHECK (previous_status IS NULL OR previous_status IN
            ('RECEIVED', 'PICKED_UP', 'RETURNED', 'LOST', 'CANCELLED')),
    CONSTRAINT ck_delivery_events_initial
        CHECK (
            (sequence_number = 1 AND previous_status IS NULL AND status = 'RECEIVED')
            OR (sequence_number > 1 AND previous_status IS NOT NULL)
        )
);

CREATE INDEX idx_delivery_events_condominium_delivery_sequence
    ON delivery_events (condominium_id, delivery_id, sequence_number);
