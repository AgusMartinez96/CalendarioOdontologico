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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppointmentApiServiceTest {
    private static final Instant NOW = Instant.parse("2026-10-03T12:00:00Z");

    @Mock
    private AppointmentRecordRepository appointments;

    @Mock
    private PatientRecordRepository patients;

    private AppointmentApiService service;
    private PatientRecord patient;

    @BeforeEach
    void setUp() {
        service = new AppointmentApiService(appointments, patients, Clock.fixed(NOW, ZoneOffset.UTC));
        patient = new PatientRecord("Ana", "Pérez", "1234", "111", null);
    }

    @Test
    void createsAppointmentWhenSlotIsFree() {
        when(patients.findById(1L)).thenReturn(Optional.of(patient));
        when(appointments.hasOverlap(any(), any(), eq(AppointmentStatus.CANCELADO)))
                .thenReturn(false);
        when(appointments.save(any(AppointmentRecord.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.create(request(NOW.plusSeconds(3600), NOW.plusSeconds(5400)));

        assertEquals(AppointmentStatus.PROGRAMADO, response.estado());
        assertEquals(NOW.plusSeconds(3600), response.startAt());
        verify(appointments).hasOverlap(NOW.plusSeconds(3600), NOW.plusSeconds(5400),
                AppointmentStatus.CANCELADO);
    }

    @Test
    void rejectsAppointmentInThePast() {
        ApiException exception = assertThrows(ApiException.class,
                () -> service.create(request(NOW.minusSeconds(60), NOW.plusSeconds(60))));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
    }

    @Test
    void rejectsEndBeforeStart() {
        ApiException exception = assertThrows(ApiException.class,
                () -> service.create(request(NOW.plusSeconds(3600), NOW.plusSeconds(1800))));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
    }

    @Test
    void rejectsOverlappingAppointment() {
        when(patients.findById(1L)).thenReturn(Optional.of(patient));
        when(appointments.hasOverlap(any(), any(), eq(AppointmentStatus.CANCELADO)))
                .thenReturn(true);

        ApiException exception = assertThrows(ApiException.class,
                () -> service.create(request(NOW.plusSeconds(3600), NOW.plusSeconds(5400))));

        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
    }

    @Test
    void rejectsMissingPatient() {
        when(patients.findById(1L)).thenReturn(Optional.empty());

        ApiException exception = assertThrows(ApiException.class,
                () -> service.create(request(NOW.plusSeconds(3600), NOW.plusSeconds(5400))));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
    }

    private AppointmentRequest request(Instant start, Instant end) {
        return new AppointmentRequest(start, end, 1L, "Consulta", null, null);
    }
}
