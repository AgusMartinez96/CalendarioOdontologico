package com.api.agenda_odontologica.api.repository;

import com.api.agenda_odontologica.api.entity.AppointmentRecord;
import com.api.agenda_odontologica.api.entity.AppointmentStatus;
import com.api.agenda_odontologica.api.entity.PatientRecord;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
    private static final Long OWNER = 1L;
    private static final Long OTHER = 2L;

    @Autowired
    private PatientRecordRepository patients;

    @Autowired
    private AppointmentRecordRepository appointments;

    @Test
    void findsOverlapsButIgnoresCancelledAppointments() {
        PatientRecord patient = patients.saveAndFlush(
                new PatientRecord(OWNER, "Ana", "Pérez", "100", "111", null));
        AppointmentRecord appointment = saveAppointment(patient, "2026-10-05T12:00:00Z",
                "2026-10-05T13:00:00Z", AppointmentStatus.PROGRAMADO);

        assertThat(appointments.hasOverlap(OWNER,
                Instant.parse("2026-10-05T12:30:00Z"),
                Instant.parse("2026-10-05T13:30:00Z"),
                AppointmentStatus.CANCELADO)).isTrue();
        assertThat(appointments.hasOverlapExcluding(OWNER,
                Instant.parse("2026-10-05T12:30:00Z"),
                Instant.parse("2026-10-05T13:30:00Z"),
                AppointmentStatus.CANCELADO, appointment.getId())).isFalse();
        assertThat(appointments.hasOverlap(OWNER,
                Instant.parse("2026-10-05T13:00:00Z"),
                Instant.parse("2026-10-05T14:00:00Z"),
                AppointmentStatus.CANCELADO)).isFalse();

        appointment.setEstado(AppointmentStatus.CANCELADO);
        appointments.saveAndFlush(appointment);
        assertThat(appointments.hasOverlap(OWNER,
                Instant.parse("2026-10-05T12:30:00Z"),
                Instant.parse("2026-10-05T13:30:00Z"),
                AppointmentStatus.CANCELADO)).isFalse();
    }

    @Test
    void overlapIsEvaluatedOnlyBetweenAppointmentsOfTheSameOwner() {
        PatientRecord mine = patients.saveAndFlush(new PatientRecord(OWNER, "Ana", "Pérez", "100", "111", null));
        patients.saveAndFlush(new PatientRecord(OTHER, "Beto", "Gómez", "200", "222", null));
        saveAppointment(mine, "2026-10-05T12:00:00Z", "2026-10-05T13:00:00Z", AppointmentStatus.PROGRAMADO);

        Instant start = Instant.parse("2026-10-05T12:15:00Z");
        Instant end = Instant.parse("2026-10-05T12:45:00Z");
        assertThat(appointments.hasOverlap(OWNER, start, end, AppointmentStatus.CANCELADO)).isTrue();
        assertThat(appointments.hasOverlap(OTHER, start, end, AppointmentStatus.CANCELADO)).isFalse();
        assertThat(appointments.hasOverlapExcluding(OTHER, start, end, AppointmentStatus.CANCELADO, -1L))
                .isFalse();
    }

    @Test
    void supportsAppointmentListsWithNoNullableFilters() {
        PatientRecord ana = patients.saveAndFlush(
                new PatientRecord(OWNER, "Ana", "Alvarez", "101", "111", null));
        PatientRecord beto = patients.saveAndFlush(
                new PatientRecord(OWNER, "Beto", "Benitez", "202", "222", null));
        saveAppointment(ana, "2026-10-05T12:00:00Z", "2026-10-05T13:00:00Z",
                AppointmentStatus.PROGRAMADO);
        saveAppointment(beto, "2026-10-06T12:00:00Z", "2026-10-06T13:00:00Z",
                AppointmentStatus.CONFIRMADO);

        Instant from = Instant.parse("2026-10-05T00:00:00Z");
        Instant to = Instant.parse("2026-10-07T00:00:00Z");
        assertThat(appointments.findInRange(OWNER, from, to, "")).hasSize(2);
        assertThat(appointments.findInRange(OWNER, from, to, "Beto"))
                .extracting(appointment -> appointment.getPatient().getNombre())
                .containsExactly("Beto");
        assertThat(appointments.findInRangeByStatus(
                OWNER, from, to, AppointmentStatus.CONFIRMADO, "")).hasSize(1);
        assertThat(appointments.findInRange(OWNER,
                Instant.parse("2026-10-06T00:00:00Z"),
                Instant.parse("2026-10-07T00:00:00Z"), "")).hasSize(1);
        assertThat(appointments.findInRangeByStatus(OWNER,
                Instant.parse("2026-10-06T00:00:00Z"),
                Instant.parse("2026-10-07T00:00:00Z"),
                AppointmentStatus.CONFIRMADO, "Beto"))
                .extracting(appointment -> appointment.getPatient().getNombre())
                .containsExactly("Beto");
        assertThat(appointments.findInRange(OTHER, from, to, "")).isEmpty();
        assertThat(appointments.findInRangeByStatus(OTHER, from, to, AppointmentStatus.CONFIRMADO, ""))
                .isEmpty();
    }

    @Test
    void supportsPatientListsWithNoNullableTextFilter() {
        patients.saveAndFlush(new PatientRecord(OWNER, "Ana", "Alvarez", "101", "111", null));
        patients.saveAndFlush(new PatientRecord(OWNER, "Beto", "Benitez", "202", "222", null));
        patients.saveAndFlush(new PatientRecord(OTHER, "Beto", "Benitez", "303", "333", null));

        assertThat(patients.search(OWNER, "")).hasSize(2);
        assertThat(patients.search(OWNER, "beto"))
                .extracting(PatientRecord::getNombre)
                .containsExactly("Beto");
        assertThat(patients.search(OTHER, "")).hasSize(1);
        assertThat(patients.search(3L, "")).isEmpty();
    }

    @Test
    void patientLookupsAreScopedToTheOwner() {
        PatientRecord mine = patients.saveAndFlush(new PatientRecord(OWNER, "Ana", "Pérez", "100", "111", null));

        assertThat(patients.findByIdAndOwnerId(mine.getId(), OWNER)).isPresent();
        assertThat(patients.findByIdAndOwnerId(mine.getId(), OTHER)).isEmpty();
        assertThat(patients.existsByOwnerIdAndDni(OWNER, "100")).isTrue();
        assertThat(patients.existsByOwnerIdAndDni(OTHER, "100")).isFalse();
        assertThat(patients.existsByOwnerIdAndDniAndIdNot(OWNER, "100", mine.getId())).isFalse();
        assertThat(patients.countByOwnerId(OWNER)).isEqualTo(1);
        assertThat(patients.countByOwnerId(OTHER)).isZero();
    }

    @Test
    void dniIsUniquePerOwnerNotGlobally() {
        patients.saveAndFlush(new PatientRecord(OWNER, "Ana", "Pérez", "100", "111", null));
        patients.saveAndFlush(new PatientRecord(OTHER, "Ana", "Pérez", "100", "111", null));

        assertThatThrownBy(() -> patients.saveAndFlush(
                new PatientRecord(OWNER, "Otra", "Persona", "100", "999", null)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void appointmentLookupsAreScopedToTheOwner() {
        PatientRecord patient = patients.saveAndFlush(new PatientRecord(OWNER, "Ana", "Pérez", "100", "111", null));
        AppointmentRecord appointment = saveAppointment(patient, "2026-10-05T12:00:00Z",
                "2026-10-05T13:00:00Z", AppointmentStatus.PROGRAMADO);

        assertThat(appointments.findByIdAndOwnerId(appointment.getId(), OWNER)).isPresent();
        assertThat(appointments.findByIdAndOwnerId(appointment.getId(), OTHER)).isEmpty();
        assertThat(appointments.existsByPatientIdAndOwnerId(patient.getId(), OWNER)).isTrue();
        assertThat(appointments.existsByPatientIdAndOwnerId(patient.getId(), OTHER)).isFalse();
        assertThat(appointments.countByOwnerId(OWNER)).isEqualTo(1);
        assertThat(appointments.countByOwnerId(OTHER)).isZero();
    }

    private AppointmentRecord saveAppointment(
            PatientRecord patient, String startAt, String endAt, AppointmentStatus status) {
        AppointmentRecord appointment = new AppointmentRecord(patient.getOwnerId());
        appointment.setPatient(patient);
        appointment.setStartAt(Instant.parse(startAt));
        appointment.setEndAt(Instant.parse(endAt));
        appointment.setMotivo("Consulta");
        appointment.setEstado(status);
        return appointments.saveAndFlush(appointment);
    }
}
