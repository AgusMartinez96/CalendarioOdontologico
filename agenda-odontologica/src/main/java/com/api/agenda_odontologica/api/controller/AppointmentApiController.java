package com.api.agenda_odontologica.api.controller;

import com.api.agenda_odontologica.api.dto.AppointmentRequest;
import com.api.agenda_odontologica.api.dto.AppointmentResponse;
import com.api.agenda_odontologica.api.entity.AppointmentStatus;
import com.api.agenda_odontologica.api.service.AppointmentApiService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/v1/appointments")
public class AppointmentApiController {
    private final AppointmentApiService service;

    public AppointmentApiController(AppointmentApiService service) {
        this.service = service;
    }

    @GetMapping
    public List<AppointmentResponse> list(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(required = false) AppointmentStatus status,
            @RequestParam(required = false) String patientSearch) {
        return service.list(from, to, status, patientSearch);
    }

    @GetMapping("/{id}")
    public AppointmentResponse get(@PathVariable Long id) {
        return service.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AppointmentResponse create(@Valid @RequestBody AppointmentRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    public AppointmentResponse update(
            @PathVariable Long id, @Valid @RequestBody AppointmentRequest request) {
        return service.update(id, request);
    }

    @PatchMapping("/{id}/cancel")
    public AppointmentResponse cancel(@PathVariable Long id) {
        return service.cancel(id);
    }
}
