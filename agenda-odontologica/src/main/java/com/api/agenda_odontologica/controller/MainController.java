package com.api.agenda_odontologica.controller;

import com.api.agenda_odontologica.dto.AppointmentDTO;
import com.api.agenda_odontologica.dto.PatientDTO;
import com.api.agenda_odontologica.service.AppointmentService;
import com.api.agenda_odontologica.service.PatientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class MainController {

    @Autowired
    private PatientService patientService;

    @Autowired
    private AppointmentService appointmentService;

    @GetMapping("/")
    public String home(Model model) {
        List<PatientDTO> patients = patientService.getAllPatients();
        List<AppointmentDTO> appointments = appointmentService.getAllAppointments();
        model.addAttribute("patients", patients);
        model.addAttribute("appointments", appointments);
        return "home"; // Usa home.html como vista principal
    }
}