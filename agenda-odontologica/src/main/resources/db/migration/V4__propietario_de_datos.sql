-- Los datos existentes pasan a pertenecer al usuario admin. El hash '!' no es un
-- BCrypt valido (nadie puede entrar con el); el arranque de la aplicacion lo
-- reemplaza y ajusta el nombre segun ADMIN_USERNAME / ADMIN_PASSWORD.
INSERT INTO users (username, username_normalized, password_hash, role, enabled)
SELECT 'admin', 'admin', '!', 'ROLE_ADMIN', TRUE
WHERE NOT EXISTS (SELECT 1 FROM users WHERE role = 'ROLE_ADMIN');

ALTER TABLE patients ADD COLUMN owner_id BIGINT;
ALTER TABLE appointments ADD COLUMN owner_id BIGINT;

UPDATE patients
SET owner_id = (SELECT id FROM users WHERE role = 'ROLE_ADMIN' ORDER BY id LIMIT 1);

UPDATE appointments
SET owner_id = (SELECT p.owner_id FROM patients p WHERE p.id = appointments.patient_id);

ALTER TABLE patients ALTER COLUMN owner_id SET NOT NULL;
ALTER TABLE appointments ALTER COLUMN owner_id SET NOT NULL;

ALTER TABLE patients
    ADD CONSTRAINT fk_patients_owner FOREIGN KEY (owner_id) REFERENCES users (id);

ALTER TABLE patients DROP CONSTRAINT uk_patients_dni;
ALTER TABLE patients ADD CONSTRAINT uk_patients_owner_dni UNIQUE (owner_id, dni);
ALTER TABLE patients ADD CONSTRAINT uk_patients_id_owner UNIQUE (id, owner_id);

-- Un turno solo puede apuntar a un paciente del mismo dueño.
ALTER TABLE appointments
    ADD CONSTRAINT fk_appointments_owner FOREIGN KEY (owner_id) REFERENCES users (id);
ALTER TABLE appointments
    ADD CONSTRAINT fk_appointments_patient_owner
    FOREIGN KEY (patient_id, owner_id) REFERENCES patients (id, owner_id);

CREATE INDEX idx_patients_owner_id ON patients (owner_id);
CREATE INDEX idx_appointments_owner_start_at ON appointments (owner_id, start_at);
