package com.api.agenda_odontologica.api.service;

import com.api.agenda_odontologica.api.dto.PatientRequest;
import com.api.agenda_odontologica.api.dto.PatientResponse;
import com.api.agenda_odontologica.api.entity.PatientRecord;
import com.api.agenda_odontologica.api.repository.AppointmentRecordRepository;
import com.api.agenda_odontologica.api.repository.PatientRecordRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class PatientApiService {
    private final PatientRecordRepository patients;
    private final AppointmentRecordRepository appointments;

    public PatientApiService(PatientRecordRepository patients, AppointmentRecordRepository appointments) {
        this.patients = patients;
        this.appointments = appointments;
    }

    @Transactional(readOnly = true)
    public List<PatientResponse> list(String search) {
        String query = normalize(search);
        return patients.search(query).stream().map(PatientResponse::new).toList();
    }

    @Transactional(readOnly = true)
    public PatientResponse get(Long id) {
        return new PatientResponse(requirePatient(id));
    }

    public PatientResponse create(PatientRequest request) {
        if (patients.existsByDni(request.dni())) {
            throw new ApiException(HttpStatus.CONFLICT, "Ya existe un paciente con ese DNI.");
        }
        return save(new PatientRecord(request.nombre().trim(), request.apellido().trim(),
                request.dni().trim(), request.telefono().trim(), normalize(request.email())));
    }

    public PatientResponse update(Long id, PatientRequest request) {
        PatientRecord patient = requirePatient(id);
        if (patients.existsByDniAndIdNot(request.dni(), id)) {
            throw new ApiException(HttpStatus.CONFLICT, "Ya existe un paciente con ese DNI.");
        }
        patient.setNombre(request.nombre().trim());
        patient.setApellido(request.apellido().trim());
        patient.setDni(request.dni().trim());
        patient.setTelefono(request.telefono().trim());
        patient.setEmail(normalize(request.email()));
        return save(patient);
    }

    public void delete(Long id) {
        PatientRecord patient = requirePatient(id);
        if (appointments.existsByPatientId(id)) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "No se puede eliminar un paciente que tiene turnos registrados.");
        }
        patients.delete(patient);
    }

    private PatientResponse save(PatientRecord patient) {
        try {
            return new PatientResponse(patients.save(patient));
        } catch (DataIntegrityViolationException exception) {
            throw new ApiException(HttpStatus.CONFLICT, "Ya existe un paciente con ese DNI.");
        }
    }

    private PatientRecord requirePatient(Long id) {
        return patients.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "No se encontró el paciente."));
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
