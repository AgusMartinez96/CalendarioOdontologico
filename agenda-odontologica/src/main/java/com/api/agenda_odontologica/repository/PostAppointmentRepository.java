package com.api.agenda_odontologica.repository;

import com.api.agenda_odontologica.entity.PostAppointment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostAppointmentRepository extends JpaRepository<PostAppointment, Long> {
}
