package com.api.agenda_odontologica.api.service;

import com.api.agenda_odontologica.api.dto.PatientRequest;
import com.api.agenda_odontologica.api.dto.PatientResponse;
import com.api.agenda_odontologica.api.entity.PatientRecord;
import com.api.agenda_odontologica.api.repository.AppointmentRecordRepository;
import com.api.agenda_odontologica.api.repository.PatientRecordRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

/** El ownerId siempre proviene del usuario autenticado y se aplica a cada operación. */
@Service
@Transactional
public class PatientApiService {
    private final PatientRecordRepository patients;
    private final AppointmentRecordRepository appointments;

    private final long maxPatientsPerUser;

    public PatientApiService(
            PatientRecordRepository patients,
            AppointmentRecordRepository appointments,
            @Value("${app.limits.max-patients-per-user:200}") long maxPatientsPerUser) {
        if (maxPatientsPerUser < 1) {
            throw new IllegalArgumentException("MAX_PATIENTS_PER_USER debe ser mayor que cero.");
        }
        this.patients = patients;
        this.appointments = appointments;
        this.maxPatientsPerUser = maxPatientsPerUser;
    }

    @Transactional(readOnly = true)
    public List<PatientResponse> list(Long ownerId, String search) {
        requireOwner(ownerId);
        String query = search == null || search.isBlank() ? "" : search.trim();
        return patients.search(ownerId, query).stream().map(PatientResponse::new).toList();
    }

    @Transactional(readOnly = true)
    public PatientResponse get(Long ownerId, Long id) {
        return new PatientResponse(requirePatient(ownerId, id));
    }

    public PatientResponse create(Long ownerId, PatientRequest request) {
        requireOwner(ownerId);
        String dni = request.dni().trim();
        if (patients.existsByOwnerIdAndDni(ownerId, dni)) {
            throw new ApiException(HttpStatus.CONFLICT, "Ya existe un paciente con ese DNI.");
        }
        if (patients.countByOwnerId(ownerId) >= maxPatientsPerUser) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "Llegaste al máximo de " + maxPatientsPerUser + " pacientes por cuenta.");
        }
        return save(new PatientRecord(ownerId, request.nombre().trim(), request.apellido().trim(),
                dni, request.telefono().trim(), normalize(request.email())));
    }

    public PatientResponse update(Long ownerId, Long id, PatientRequest request) {
        PatientRecord patient = requirePatient(ownerId, id);
        String dni = request.dni().trim();
        if (patients.existsByOwnerIdAndDniAndIdNot(ownerId, dni, id)) {
            throw new ApiException(HttpStatus.CONFLICT, "Ya existe un paciente con ese DNI.");
        }
        patient.setNombre(request.nombre().trim());
        patient.setApellido(request.apellido().trim());
        patient.setDni(dni);
        patient.setTelefono(request.telefono().trim());
        patient.setEmail(normalize(request.email()));
        return save(patient);
    }

    public void delete(Long ownerId, Long id) {
        PatientRecord patient = requirePatient(ownerId, id);
        if (appointments.existsByPatientIdAndOwnerId(id, ownerId)) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "No se puede eliminar un paciente que tiene turnos registrados.");
        }
        patients.delete(patient);
    }

    private PatientResponse save(PatientRecord patient) {
        try {
            return new PatientResponse(patients.saveAndFlush(patient));
        } catch (DataIntegrityViolationException exception) {
            throw new ApiException(HttpStatus.CONFLICT, "Ya existe un paciente con ese DNI.");
        }
    }

    private PatientRecord requirePatient(Long ownerId, Long id) {
        requireOwner(ownerId);
        return patients.findByIdAndOwnerId(id, ownerId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "No se encontró el paciente."));
    }

    private static void requireOwner(Long ownerId) {
        Objects.requireNonNull(ownerId, "ownerId");
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
