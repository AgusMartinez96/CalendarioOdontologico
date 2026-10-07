package com.api.agenda_odontologica.api.service;

import com.api.agenda_odontologica.api.dto.PatientRequest;
import com.api.agenda_odontologica.api.entity.PatientRecord;
import com.api.agenda_odontologica.api.repository.AppointmentRecordRepository;
import com.api.agenda_odontologica.api.repository.PatientRecordRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PatientApiServiceTest {
    private static final Long OWNER = 7L;

    @Mock
    private PatientRecordRepository patients;

    @Mock
    private AppointmentRecordRepository appointments;

    @InjectMocks
    private PatientApiService service;

    @Test
    void createsPatient() {
        PatientRequest request = new PatientRequest(" Ana ", " Pérez ", "1234", " 111 ", null);
        when(patients.existsByOwnerIdAndDni(OWNER, "1234")).thenReturn(false);
        when(patients.saveAndFlush(any(PatientRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.create(OWNER, request);

        assertEquals("Ana", response.nombre());
        assertEquals("Pérez", response.apellido());
        assertEquals("111", response.telefono());
    }

    @Test
    void rejectsDuplicateDni() {
        when(patients.existsByOwnerIdAndDni(OWNER, "1234")).thenReturn(true);

        ApiException exception = assertThrows(ApiException.class,
                () -> service.create(OWNER, new PatientRequest("Ana", "Pérez", "1234", "111", null)));

        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
        verify(patients, never()).saveAndFlush(any());
    }

    @Test
    void returnsNotFoundWhenPatientDoesNotExist() {
        when(patients.findByIdAndOwnerId(44L, OWNER)).thenReturn(Optional.empty());

        ApiException exception = assertThrows(ApiException.class, () -> service.get(OWNER, 44L));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
    }
}
