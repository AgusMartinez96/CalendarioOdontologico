package com.api.agenda_odontologica.api.controller;

import com.api.agenda_odontologica.api.dto.PatientRequest;
import com.api.agenda_odontologica.api.dto.PatientResponse;
import com.api.agenda_odontologica.api.security.AppUserDetails;
import com.api.agenda_odontologica.api.service.PatientApiService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/patients")
public class PatientApiController {
    private final PatientApiService service;

    public PatientApiController(PatientApiService service) {
        this.service = service;
    }

    @GetMapping
    public List<PatientResponse> list(
            @AuthenticationPrincipal AppUserDetails user, @RequestParam(required = false) String search) {
        return service.list(user.getId(), search);
    }

    @GetMapping("/{id}")
    public PatientResponse get(@AuthenticationPrincipal AppUserDetails user, @PathVariable Long id) {
        return service.get(user.getId(), id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PatientResponse create(
            @AuthenticationPrincipal AppUserDetails user, @Valid @RequestBody PatientRequest request) {
        return service.create(user.getId(), request);
    }

    @PutMapping("/{id}")
    public PatientResponse update(
            @AuthenticationPrincipal AppUserDetails user,
            @PathVariable Long id,
            @Valid @RequestBody PatientRequest request) {
        return service.update(user.getId(), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal AppUserDetails user, @PathVariable Long id) {
        service.delete(user.getId(), id);
    }
}
