package com.api.agenda_odontologica.api.service;

import com.api.agenda_odontologica.api.dto.AppointmentRequest;
import com.api.agenda_odontologica.api.dto.AppointmentResponse;
import com.api.agenda_odontologica.api.entity.AppointmentRecord;
import com.api.agenda_odontologica.api.entity.AppointmentStatus;
import com.api.agenda_odontologica.api.entity.PatientRecord;
import com.api.agenda_odontologica.api.repository.AppointmentRecordRepository;
import com.api.agenda_odontologica.api.repository.PatientRecordRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

@Service
@Transactional
public class AppointmentApiService {
    private final AppointmentRecordRepository appointments;
    private final PatientRecordRepository patients;
    private final Clock clock;

    public AppointmentApiService(
            AppointmentRecordRepository appointments,
            PatientRecordRepository patients,
            Clock clock) {
        this.appointments = appointments;
        this.patients = patients;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<AppointmentResponse> list(
            Instant from, Instant to, AppointmentStatus status, String patientSearch) {
        if (from == null || to == null || !to.isAfter(from)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "El rango de fechas es inválido.");
        }
        return appointments.findInRange(from, to, status, normalize(patientSearch))
                .stream().map(AppointmentResponse::new).toList();
    }

    @Transactional(readOnly = true)
    public AppointmentResponse get(Long id) {
        return new AppointmentResponse(requireAppointment(id));
    }

    public AppointmentResponse create(AppointmentRequest request) {
        validateTime(request);
        PatientRecord patient = requirePatient(request.patientId());
        ensureAvailable(request, null);
        AppointmentRecord appointment = new AppointmentRecord();
        apply(appointment, request, patient);
        return new AppointmentResponse(appointments.save(appointment));
    }

    public AppointmentResponse update(Long id, AppointmentRequest request) {
        AppointmentRecord appointment = requireAppointment(id);
        validateTime(request);
        PatientRecord patient = requirePatient(request.patientId());
        ensureAvailable(request, id);
        apply(appointment, request, patient);
        return new AppointmentResponse(appointments.save(appointment));
    }

    public AppointmentResponse cancel(Long id) {
        AppointmentRecord appointment = requireAppointment(id);
        appointment.setEstado(AppointmentStatus.CANCELADO);
        return new AppointmentResponse(appointments.save(appointment));
    }

    private void validateTime(AppointmentRequest request) {
        if (request.startAt() == null || request.endAt() == null
                || !request.endAt().isAfter(request.startAt())) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "La fecha y hora de fin debe ser posterior al inicio.");
        }
        if (request.startAt().isBefore(clock.instant())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "No se pueden crear turnos en el pasado.");
        }
    }

    private void ensureAvailable(AppointmentRequest request, Long excludeId) {
        AppointmentStatus status = request.estado() == null ? AppointmentStatus.PROGRAMADO : request.estado();
        if (status != AppointmentStatus.CANCELADO && appointments.hasOverlap(
                request.startAt(), request.endAt(), AppointmentStatus.CANCELADO, excludeId)) {
            throw new ApiException(HttpStatus.CONFLICT, "El horario se superpone con otro turno.");
        }
    }

    private static void apply(
            AppointmentRecord appointment, AppointmentRequest request, PatientRecord patient) {
        appointment.setStartAt(request.startAt());
        appointment.setEndAt(request.endAt());
        appointment.setPatient(patient);
        appointment.setMotivo(request.motivo().trim());
        appointment.setNotas(normalize(request.notas()));
        appointment.setEstado(request.estado() == null ? AppointmentStatus.PROGRAMADO : request.estado());
    }

    private AppointmentRecord requireAppointment(Long id) {
        return appointments.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "No se encontró el turno."));
    }

    private PatientRecord requirePatient(Long id) {
        return patients.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "No se encontró el paciente."));
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
