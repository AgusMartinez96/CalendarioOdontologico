package com.api.agenda_odontologica;

import com.api.agenda_odontologica.service.PatientService;
import com.api.agenda_odontologica.dto.PatientDTO;
import com.api.agenda_odontologica.entity.Patient;
import com.api.agenda_odontologica.repository.PatientRepository;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PatientServiceTest {

    @Test
    void testSavePatient() {
        PatientRepository repo = mock(PatientRepository.class);
        PatientService service = new PatientService(repo);

        PatientDTO dto = new PatientDTO();
        dto.setNombre("Juan");
        dto.setApellido("Perez");
        dto.setDni("12345678");
        dto.setObraSocial("OSDE");

        Patient patient = new Patient();
        patient.setNombre("Juan");
        patient.setApellido("Perez");
        patient.setDni("12345678");
        patient.setObraSocial("OSDE");

        when(repo.save(any(Patient.class))).thenReturn(patient);

        PatientDTO saved = service.savePatient(dto);
        assertEquals("Juan", saved.getNombre());
        assertEquals("Perez", saved.getApellido());
    }

    @Test
    void testGetPatientById() {
        PatientRepository repo = mock(PatientRepository.class);
        PatientService service = new PatientService(repo);

        Patient patient = new Patient();
        patient.setId(1L);
        patient.setNombre("Ana");

        when(repo.findById(1L)).thenReturn(java.util.Optional.of(patient));

        PatientDTO found = service.getPatientById(1L);
        assertNotNull(found);
        assertEquals(1L, found.getId());
        assertEquals("Ana", found.getNombre());
    }
}
