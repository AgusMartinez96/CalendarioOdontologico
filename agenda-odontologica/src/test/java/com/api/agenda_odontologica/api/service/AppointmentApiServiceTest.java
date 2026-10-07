package com.api.agenda_odontologica.api.service;

import com.api.agenda_odontologica.api.dto.AppointmentRequest;
import com.api.agenda_odontologica.api.entity.AppointmentRecord;
import com.api.agenda_odontologica.api.entity.AppointmentStatus;
import com.api.agenda_odontologica.api.entity.PatientRecord;
import com.api.agenda_odontologica.api.repository.AppointmentRecordRepository;
import com.api.agenda_odontologica.api.repository.PatientRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppointmentApiServiceTest {
    private static final Long OWNER = 7L;
    private static final Instant NOW = Instant.parse("2026-10-03T12:00:00Z");

    @Mock
    private AppointmentRecordRepository appointments;

    @Mock
    private PatientRecordRepository patients;

    private AppointmentApiService service;
    private PatientRecord patient;

    @BeforeEach
    void setUp() {
        service = new AppointmentApiService(appointments, patients, Clock.fixed(NOW, ZoneOffset.UTC), 15);
        patient = new PatientRecord(OWNER, "Ana", "Pérez", "1234", "111", null);
    }

    @Test
    void createsAppointmentWhenSlotIsFree() {
        when(patients.findByIdAndOwnerId(1L, OWNER)).thenReturn(Optional.of(patient));
        when(appointments.hasOverlap(eq(OWNER), any(), any(), eq(AppointmentStatus.CANCELADO)))
                .thenReturn(false);
        when(appointments.save(any(AppointmentRecord.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.create(OWNER, request(NOW.plusSeconds(3600)));

        assertEquals(AppointmentStatus.PROGRAMADO, response.estado());
        assertEquals(NOW.plusSeconds(3600), response.startAt());
        assertEquals(NOW.plusSeconds(4500), response.endAt());
        verify(appointments).hasOverlap(OWNER, NOW.plusSeconds(3600), NOW.plusSeconds(4500),
                AppointmentStatus.CANCELADO);
    }

    @Test
    void createsAppointmentUsingConfiguredDefaultDuration() {
        service = new AppointmentApiService(
                appointments, patients, Clock.fixed(NOW, ZoneOffset.UTC), 45);
        when(patients.findByIdAndOwnerId(1L, OWNER)).thenReturn(Optional.of(patient));
        when(appointments.hasOverlap(eq(OWNER), any(), any(), eq(AppointmentStatus.CANCELADO)))
                .thenReturn(false);
        when(appointments.save(any(AppointmentRecord.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.create(OWNER, request(NOW.plusSeconds(3600)));

        assertEquals(NOW.plusSeconds(6300), response.endAt());
    }

    @Test
    void updatingStartPreservesExistingAppointmentDuration() {
        AppointmentRecord appointment = new AppointmentRecord(OWNER);
        appointment.setStartAt(NOW.plusSeconds(3600));
        appointment.setEndAt(NOW.plusSeconds(7200));
        when(appointments.findByIdAndOwnerId(1L, OWNER)).thenReturn(Optional.of(appointment));
        when(patients.findByIdAndOwnerId(1L, OWNER)).thenReturn(Optional.of(patient));
        when(appointments.hasOverlapExcluding(eq(OWNER), any(), any(), eq(AppointmentStatus.CANCELADO), eq(1L)))
                .thenReturn(false);
        when(appointments.save(any(AppointmentRecord.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.update(OWNER, 1L, request(NOW.plusSeconds(10_800)));

        assertEquals(NOW.plusSeconds(10_800), response.startAt());
        assertEquals(NOW.plusSeconds(14_400), response.endAt());
        verify(appointments).hasOverlapExcluding(OWNER, NOW.plusSeconds(10_800), NOW.plusSeconds(14_400),
                AppointmentStatus.CANCELADO, 1L);
    }

    @Test
    void rejectsAppointmentInThePast() {
        ApiException exception = assertThrows(ApiException.class,
                () -> service.create(OWNER, request(NOW.minusSeconds(60))));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
    }

    @Test
    void rejectsOverlappingAppointment() {
        when(patients.findByIdAndOwnerId(1L, OWNER)).thenReturn(Optional.of(patient));
        when(appointments.hasOverlap(eq(OWNER), any(), any(), eq(AppointmentStatus.CANCELADO)))
                .thenReturn(true);

        ApiException exception = assertThrows(ApiException.class,
                () -> service.create(OWNER, request(NOW.plusSeconds(3600))));

        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
        verify(appointments).hasOverlap(OWNER, NOW.plusSeconds(3600), NOW.plusSeconds(4500),
                AppointmentStatus.CANCELADO);
    }

    @Test
    void cancelledAppointmentDoesNotBlockItsCalculatedSlot() {
        when(patients.findByIdAndOwnerId(1L, OWNER)).thenReturn(Optional.of(patient));
        when(appointments.save(any(AppointmentRecord.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.create(OWNER, request(
                NOW.plusSeconds(3600), AppointmentStatus.CANCELADO));

        assertEquals(AppointmentStatus.CANCELADO, response.estado());
        assertEquals(NOW.plusSeconds(4500), response.endAt());
        verify(appointments, never()).hasOverlap(any(), any(), any(), any());
    }

    @Test
    void rejectsMissingPatient() {
        when(patients.findByIdAndOwnerId(1L, OWNER)).thenReturn(Optional.empty());

        ApiException exception = assertThrows(ApiException.class,
                () -> service.create(OWNER, request(NOW.plusSeconds(3600))));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
    }

    private AppointmentRequest request(Instant start) {
        return request(start, null);
    }

    private AppointmentRequest request(Instant start, AppointmentStatus status) {
        return new AppointmentRequest(start, 1L, "Consulta", null, status);
    }
}
