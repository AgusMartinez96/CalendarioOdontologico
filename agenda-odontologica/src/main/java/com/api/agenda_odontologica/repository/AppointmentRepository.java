package com.api.agenda_odontologica.repository;

import com.api.agenda_odontologica.entity.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {
    List<Appointment> findByAsistenciaFalse();
}