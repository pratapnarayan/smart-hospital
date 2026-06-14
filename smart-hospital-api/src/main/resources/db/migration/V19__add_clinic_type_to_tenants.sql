-- V19__add_clinic_type_to_tenants.sql
ALTER TABLE public.tenants
    ADD COLUMN IF NOT EXISTS clinic_type VARCHAR(20) NOT NULL DEFAULT 'FULL_HMS'
        CHECK (clinic_type IN ('FULL_HMS', 'CLINIC_OPD'));
