package com.api.agenda_odontologica.service;

import com.api.agenda_odontologica.entity.PostAppointment;
import com.api.agenda_odontologica.repository.PostAppointmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PostAppointmentService {
    @Autowired
    private PostAppointmentRepository postAppointmentRepository;

    public List<PostAppointment> getAllPostTurnos() {
        return postAppointmentRepository.findAll();
    }

    public PostAppointment getPostTurnoById(Long id) {
        return postAppointmentRepository.findById(id).orElse(null);
    }

    public PostAppointment savePostTurno(PostAppointment postAppointment) {
        return postAppointmentRepository.save(postAppointment);
    }

    public void deletePostTurno(Long id) {
        postAppointmentRepository.deleteById(id);
    }
}