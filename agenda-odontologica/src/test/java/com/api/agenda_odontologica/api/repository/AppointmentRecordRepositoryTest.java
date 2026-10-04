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
        "spring.datasource.username=sa",
        "spring.datasource.password=",
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
                AppointmentStatus.CANCELADO)).isTrue();
        assertThat(appointments.hasOverlapExcluding(
                Instant.parse("2026-10-05T12:30:00Z"),
                Instant.parse("2026-10-05T13:30:00Z"),
                AppointmentStatus.CANCELADO, appointment.getId())).isFalse();
        assertThat(appointments.hasOverlap(
                Instant.parse("2026-10-05T13:00:00Z"),
                Instant.parse("2026-10-05T14:00:00Z"),
                AppointmentStatus.CANCELADO)).isFalse();

        appointment.setEstado(AppointmentStatus.CANCELADO);
        appointments.saveAndFlush(appointment);
        assertThat(appointments.hasOverlap(
                Instant.parse("2026-10-05T12:30:00Z"),
                Instant.parse("2026-10-05T13:30:00Z"),
                AppointmentStatus.CANCELADO)).isFalse();
    }

    @Test
    void supportsAppointmentListsWithNoNullableFilters() {
        PatientRecord ana = patients.saveAndFlush(
                new PatientRecord("Ana", "Alvarez", "101", "111", null));
        PatientRecord beto = patients.saveAndFlush(
                new PatientRecord("Beto", "Benitez", "202", "222", null));
        saveAppointment(ana, "2026-10-05T12:00:00Z", "2026-10-05T13:00:00Z",
                AppointmentStatus.PROGRAMADO);
        saveAppointment(beto, "2026-10-06T12:00:00Z", "2026-10-06T13:00:00Z",
                AppointmentStatus.CONFIRMADO);

        Instant from = Instant.parse("2026-10-05T00:00:00Z");
        Instant to = Instant.parse("2026-10-07T00:00:00Z");
        assertThat(appointments.findInRange(from, to, "")).hasSize(2);
        assertThat(appointments.findInRange(from, to, "Beto"))
                .extracting(appointment -> appointment.getPatient().getNombre())
                .containsExactly("Beto");
        assertThat(appointments.findInRangeByStatus(
                from, to, AppointmentStatus.CONFIRMADO, "")).hasSize(1);
        assertThat(appointments.findInRange(
                Instant.parse("2026-10-06T00:00:00Z"),
                Instant.parse("2026-10-07T00:00:00Z"), "")).hasSize(1);
        assertThat(appointments.findInRangeByStatus(
                Instant.parse("2026-10-06T00:00:00Z"),
                Instant.parse("2026-10-07T00:00:00Z"),
                AppointmentStatus.CONFIRMADO, "Beto"))
                .extracting(appointment -> appointment.getPatient().getNombre())
                .containsExactly("Beto");
    }

    @Test
    void supportsPatientListsWithNoNullableTextFilter() {
        patients.saveAndFlush(new PatientRecord("Ana", "Alvarez", "101", "111", null));
        patients.saveAndFlush(new PatientRecord("Beto", "Benitez", "202", "222", null));

        assertThat(patients.search("")).hasSize(2);
        assertThat(patients.search("beto"))
                .extracting(PatientRecord::getNombre)
                .containsExactly("Beto");
    }

    private void saveAppointment(
            PatientRecord patient, String startAt, String endAt, AppointmentStatus status) {
        AppointmentRecord appointment = new AppointmentRecord();
        appointment.setPatient(patient);
        appointment.setStartAt(Instant.parse(startAt));
        appointment.setEndAt(Instant.parse(endAt));
        appointment.setMotivo("Consulta");
        appointment.setEstado(status);
        appointments.saveAndFlush(appointment);
    }
}
