package com.api.agenda_odontologica.controller;

import com.api.agenda_odontologica.entity.PostAppointment;
import com.api.agenda_odontologica.service.PostAppointmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/postturnos")
public class PostAppointmentController {
    @Autowired
    private PostAppointmentService postAppointmentService;

    @GetMapping
    public List<PostAppointment> getAllPostTurnos() {
        return postAppointmentService.getAllPostTurnos();
    }

    @GetMapping("/{id}")
    public PostAppointment getPostTurnoById(@PathVariable Long id) {
        return postAppointmentService.getPostTurnoById(id);
    }

    @PostMapping
    public PostAppointment createPostTurno(@RequestBody PostAppointment postAppointment) {
        return postAppointmentService.savePostTurno(postAppointment);
    }

    @PutMapping("/{id}")
    public PostAppointment updatePostTurno(@PathVariable Long id, @RequestBody PostAppointment postAppointment) {
        postAppointment.setId(id);
        return postAppointmentService.savePostTurno(postAppointment);
    }

    @DeleteMapping("/{id}")
    public void deletePostTurno(@PathVariable Long id) {
        postAppointmentService.deletePostTurno(id);
    }
}
