package com.api.agenda_odontologica.api.repository;

import com.api.agenda_odontologica.api.entity.AppointmentRecord;
import com.api.agenda_odontologica.api.entity.AppointmentStatus;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import com.api.agenda_odontologica.api.entity.PatientRecord;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:appointments;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false"
})
@EntityScan(basePackageClasses = PatientRecord.class)
class AppointmentRecordRepositoryTest {
    @Autowired
    private PatientRecordRepository patients;

    @Autowired
    private AppointmentRecordRepository appointments;

    @Test
    void findsOverlapsButIgnoresCancelledAppointments() {
        PatientRecord patient = patients.saveAndFlush(
                new PatientRecord("Ana", "Pérez", "100", "111", null));
        AppointmentRecord appointment = new AppointmentRecord();
        appointment.setPatient(patient);
        appointment.setStartAt(Instant.parse("2026-10-05T12:00:00Z"));
        appointment.setEndAt(Instant.parse("2026-10-05T13:00:00Z"));
        appointment.setMotivo("Consulta");
        appointment.setEstado(AppointmentStatus.PROGRAMADO);
        appointments.saveAndFlush(appointment);

        assertThat(appointments.hasOverlap(
                Instant.parse("2026-10-05T12:30:00Z"),
                Instant.parse("2026-10-05T13:30:00Z"),
                AppointmentStatus.CANCELADO, null)).isTrue();
        assertThat(appointments.hasOverlap(
                Instant.parse("2026-10-05T13:00:00Z"),
                Instant.parse("2026-10-05T14:00:00Z"),
                AppointmentStatus.CANCELADO, null)).isFalse();

        appointment.setEstado(AppointmentStatus.CANCELADO);
        appointments.saveAndFlush(appointment);
        assertThat(appointments.hasOverlap(
                Instant.parse("2026-10-05T12:30:00Z"),
                Instant.parse("2026-10-05T13:30:00Z"),
                AppointmentStatus.CANCELADO, null)).isFalse();
    }
}
