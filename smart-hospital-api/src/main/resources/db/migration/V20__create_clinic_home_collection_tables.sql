-- V20__create_clinic_home_collection_tables.sql
CREATE TABLE IF NOT EXISTS clinic_home_collections (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    patient_id      UUID        NOT NULL,
    patient_name    VARCHAR(200) NOT NULL,
    patient_phone   VARCHAR(20),
    address         TEXT        NOT NULL,
    scheduled_at    TIMESTAMPTZ NOT NULL,
    collected_at    TIMESTAMPTZ,
    technician_id   UUID,
    technician_name VARCHAR(200),
    status          VARCHAR(30) NOT NULL DEFAULT 'SCHEDULED'
                        CHECK (status IN ('SCHEDULED','EN_ROUTE','COLLECTED','FAILED','CANCELLED')),
    failure_reason  TEXT,
    notes           TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by      VARCHAR(100),
    updated_by      VARCHAR(100)
);

CREATE INDEX IF NOT EXISTS idx_clinic_home_collections_patient
    ON clinic_home_collections(patient_id);

CREATE INDEX IF NOT EXISTS idx_clinic_home_collections_scheduled_at
    ON clinic_home_collections(scheduled_at);

CREATE INDEX IF NOT EXISTS idx_clinic_home_collections_technician
    ON clinic_home_collections(technician_id);

CREATE INDEX IF NOT EXISTS idx_clinic_home_collections_status
    ON clinic_home_collections(status);
