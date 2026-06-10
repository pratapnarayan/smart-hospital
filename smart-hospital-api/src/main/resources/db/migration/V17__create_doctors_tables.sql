-- Doctor module tables
-- specializations → doctor_specializations (M2M) → doctor_profiles
-- doctor_profiles.employee_id FK → employees.id

CREATE TABLE IF NOT EXISTS specializations (
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(100) NOT NULL UNIQUE,
    code        VARCHAR(20)  NOT NULL UNIQUE,
    description TEXT,
    active      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_by  VARCHAR(36)
);

CREATE TABLE IF NOT EXISTS doctor_profiles (
    id                    UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    employee_id           UUID          NOT NULL UNIQUE REFERENCES employees(id) ON DELETE CASCADE,
    profile_photo         VARCHAR(500),
    biography             TEXT,
    qualifications        TEXT,
    experience_years      INT           DEFAULT 0,
    consultation_fee      NUMERIC(10,2) DEFAULT 0,
    follow_up_fee         NUMERIC(10,2) DEFAULT 0,
    tele_consultation_fee NUMERIC(10,2) DEFAULT 0,
    languages             VARCHAR(300),
    awards                TEXT,
    achievements          TEXT,
    publications          TEXT,
    online_booking_enabled BOOLEAN      NOT NULL DEFAULT TRUE,
    display_on_portal     BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at            TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at            TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    created_by            VARCHAR(36),
    updated_by            VARCHAR(36)
);

CREATE INDEX IF NOT EXISTS idx_doctor_profiles_employee_id ON doctor_profiles(employee_id);

CREATE TABLE IF NOT EXISTS doctor_specializations (
    doctor_profile_id UUID NOT NULL REFERENCES doctor_profiles(id) ON DELETE CASCADE,
    specialization_id UUID NOT NULL REFERENCES specializations(id) ON DELETE CASCADE,
    PRIMARY KEY (doctor_profile_id, specialization_id)
);

CREATE TABLE IF NOT EXISTS doctor_schedules (
    id             UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    doctor_id      UUID        NOT NULL REFERENCES doctor_profiles(id) ON DELETE CASCADE,
    day_of_week    VARCHAR(10) NOT NULL CHECK (day_of_week IN ('MONDAY','TUESDAY','WEDNESDAY','THURSDAY','FRIDAY','SATURDAY','SUNDAY')),
    shift_start    TIME        NOT NULL,
    shift_end      TIME        NOT NULL,
    slot_duration_mins INT     NOT NULL DEFAULT 15,
    active         BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by     VARCHAR(36)
);

CREATE INDEX IF NOT EXISTS idx_doctor_schedules_doctor_id ON doctor_schedules(doctor_id);
