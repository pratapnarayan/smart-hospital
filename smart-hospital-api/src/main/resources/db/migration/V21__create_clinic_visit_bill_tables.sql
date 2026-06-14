-- V21__create_clinic_visit_bill_tables.sql
CREATE TABLE IF NOT EXISTS clinic_visit_bills (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    bill_number     VARCHAR(30) NOT NULL UNIQUE,
    opd_visit_id    UUID        NOT NULL, -- UUID ref to opd_visits; no FK across modules by design
    patient_id      UUID        NOT NULL,
    patient_name    VARCHAR(200) NOT NULL,
    visit_date      DATE        NOT NULL,
    total_amount    NUMERIC(12,2) NOT NULL DEFAULT 0,
    status          VARCHAR(20) NOT NULL DEFAULT 'DRAFT'
                        CHECK (status IN ('DRAFT','FINALIZED','CANCELLED')),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by      VARCHAR(100),
    updated_by      VARCHAR(100)
);

CREATE TABLE IF NOT EXISTS clinic_visit_bill_items (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    bill_id         UUID        NOT NULL REFERENCES clinic_visit_bills(id) ON DELETE CASCADE,
    line_type       VARCHAR(30) NOT NULL
                        CHECK (line_type IN ('CONSULTATION','PATHOLOGY','PHARMACY')),
    description     VARCHAR(300) NOT NULL,
    amount          NUMERIC(12,2) NOT NULL CHECK (amount > 0),
    source_id       UUID,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by      VARCHAR(100),
    updated_by      VARCHAR(100)
);

CREATE INDEX IF NOT EXISTS idx_clinic_visit_bills_opd_visit
    ON clinic_visit_bills(opd_visit_id);

CREATE INDEX IF NOT EXISTS idx_clinic_visit_bills_patient
    ON clinic_visit_bills(patient_id);

CREATE INDEX IF NOT EXISTS idx_clinic_visit_bills_visit_date
    ON clinic_visit_bills(visit_date);

CREATE INDEX IF NOT EXISTS idx_clinic_visit_bill_items_bill
    ON clinic_visit_bill_items(bill_id);
