package com.api.agenda_odontologica.api.service;

import com.api.agenda_odontologica.api.dto.AppointmentRequest;
import com.api.agenda_odontologica.api.dto.AppointmentResponse;
import com.api.agenda_odontologica.api.entity.AppointmentRecord;
import com.api.agenda_odontologica.api.entity.AppointmentStatus;
import com.api.agenda_odontologica.api.entity.PatientRecord;
import com.api.agenda_odontologica.api.repository.AppointmentRecordRepository;
import com.api.agenda_odontologica.api.repository.PatientRecordRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
@Transactional
public class AppointmentApiService {
    private final AppointmentRecordRepository appointments;
    private final PatientRecordRepository patients;
    private final Clock clock;
    private final Duration defaultDuration;

    public AppointmentApiService(
            AppointmentRecordRepository appointments,
            PatientRecordRepository patients,
            Clock clock,
            @Value("${app.appointments.default-minutes:15}") long defaultDurationMinutes) {
        if (defaultDurationMinutes <= 0) {
            throw new IllegalArgumentException("La duración predeterminada de los turnos debe ser mayor que cero.");
        }
        this.appointments = appointments;
        this.patients = patients;
        this.clock = clock;
        this.defaultDuration = Duration.ofMinutes(defaultDurationMinutes);
    }

    @Transactional(readOnly = true)
    public List<AppointmentResponse> list(
            Instant from, Instant to, AppointmentStatus status, String patientSearch) {
        if (from == null || to == null || !to.isAfter(from)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "El rango de fechas es inválido.");
        }
        String search = patientSearch == null || patientSearch.isBlank() ? "" : patientSearch.trim();
        List<AppointmentRecord> results = status == null
                ? appointments.findInRange(from, to, search)
                : appointments.findInRangeByStatus(from, to, status, search);
        return results.stream().map(AppointmentResponse::new).toList();
    }

    @Transactional(readOnly = true)
    public AppointmentResponse get(Long id) {
        return new AppointmentResponse(requireAppointment(id));
    }

    public AppointmentResponse create(AppointmentRequest request) {
        validateStart(request.startAt());
        PatientRecord patient = requirePatient(request.patientId());
        Instant endAt = request.startAt().plus(defaultDuration);
        ensureAvailable(request.startAt(), endAt, request.estado(), null);
        AppointmentRecord appointment = new AppointmentRecord();
        apply(appointment, request, patient, endAt);
        return new AppointmentResponse(appointments.save(appointment));
    }

    public AppointmentResponse update(Long id, AppointmentRequest request) {
        AppointmentRecord appointment = requireAppointment(id);
        validateStart(request.startAt());
        PatientRecord patient = requirePatient(request.patientId());
        Duration existingDuration = Duration.between(appointment.getStartAt(), appointment.getEndAt());
        Instant endAt = request.startAt().plus(existingDuration);
        ensureAvailable(request.startAt(), endAt, request.estado(), id);
        apply(appointment, request, patient, endAt);
        return new AppointmentResponse(appointments.save(appointment));
    }

    public AppointmentResponse cancel(Long id) {
        AppointmentRecord appointment = requireAppointment(id);
        appointment.setEstado(AppointmentStatus.CANCELADO);
        return new AppointmentResponse(appointments.save(appointment));
    }

    private void validateStart(Instant startAt) {
        if (startAt == null || startAt.isBefore(clock.instant())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "No se pueden crear turnos en el pasado.");
        }
    }

    private void ensureAvailable(Instant startAt, Instant endAt, AppointmentStatus requestedStatus, Long excludeId) {
        AppointmentStatus status = requestedStatus == null ? AppointmentStatus.PROGRAMADO : requestedStatus;
        if (status == AppointmentStatus.CANCELADO) {
            return;
        }
        boolean overlaps = excludeId == null
                ? appointments.hasOverlap(startAt, endAt, AppointmentStatus.CANCELADO)
                : appointments.hasOverlapExcluding(startAt, endAt, AppointmentStatus.CANCELADO, excludeId);
        if (overlaps) {
            throw new ApiException(HttpStatus.CONFLICT, "El horario se superpone con otro turno.");
        }
    }

    private static void apply(
            AppointmentRecord appointment, AppointmentRequest request, PatientRecord patient, Instant endAt) {
        appointment.setStartAt(request.startAt());
        appointment.setEndAt(endAt);
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
