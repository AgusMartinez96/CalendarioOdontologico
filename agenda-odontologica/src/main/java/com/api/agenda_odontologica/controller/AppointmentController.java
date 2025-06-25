package com.api.agenda_odontologica.controller;

import com.api.agenda_odontologica.dto.AppointmentDTO;
import com.api.agenda_odontologica.dto.AppointmentRequest;
import com.api.agenda_odontologica.dto.PatientDTO;
import com.api.agenda_odontologica.service.AppointmentService;
import com.api.agenda_odontologica.service.PatientService;
import org.springframework.ui.Model;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/appointment")
public class AppointmentController {

    @Autowired
    private AppointmentService appointmentService;

    @Autowired
    private PatientService patientService;

    @GetMapping
    public List<AppointmentDTO> getAllAppointments() {
        return appointmentService.getAllAppointments();
    }

    @GetMapping("/{id}")
    public AppointmentDTO getAppointmentById(@PathVariable Long id) {
        return appointmentService.getAppointmentById(id);
    }
    @GetMapping("/findPatientByDni/{dni}") //endpoint verificar DNI
    public PatientDTO findPatientByDni(@PathVariable String dni) {
        return patientService.findByDni(dni);
    }

    @GetMapping("/available") //endpoint mostrar turnos disponibles
    public ResponseEntity<List<AppointmentDTO>> getAvailableAppointments() {
        try {
            List<AppointmentDTO> appointments = appointmentService.getAvailableAppointments();
            return new ResponseEntity<>(appointments, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(List.of(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // Mostrar formulario para asignar turno
    @GetMapping("/assign/{patientId}")
    public String showAssignAppointmentForm(@PathVariable Long patientId, Model model) {
        model.addAttribute("patientId", patientId);
        return "assign_appointment"; // Muestra la plantilla assign_appointment.html
    }


    @PostMapping
    public AppointmentDTO createAppointment(@RequestBody AppointmentDTO appointmentDTO) {
        return appointmentService.saveAppointment(appointmentDTO);
    }
    @PostMapping("/request")
    public AppointmentDTO requestAppointment(@RequestBody AppointmentRequest appointmentRequest) {
        return appointmentService.requestAppointment(appointmentRequest.getAppointmentDTO(), appointmentRequest.getPatientDTO());
    }

    @PostMapping("/confirm/{appointmentId}/{patientId}") //endpoint confirmar turno
    public String confirmAppointment(@PathVariable Long appointmentId, @PathVariable Long patientId) {
        AppointmentDTO appointmentDTO = appointmentService.getAppointmentById(appointmentId);
        if (appointmentDTO != null && !appointmentDTO.getAsistencia()) {
            appointmentDTO.setPatientId(patientId);
            appointmentDTO.setAsistencia(true);
            appointmentService.saveAppointment(appointmentDTO);
            return "Muchas gracias, lo esperamos!";
        } else {
            return "El turno no está disponible.";
        }
    }

    // Guardar turno en la base de datos
    @PostMapping("/save")
    public String saveAppointment(@ModelAttribute AppointmentDTO appointmentDTO) {
        appointmentService.saveAppointment(appointmentDTO);
        return "redirect:/dashboard"; // Redirige de nuevo al panel despues de asignar el turno
    }

    @PutMapping("/{id}")
    public AppointmentDTO updateAppointment(@PathVariable Long id, @RequestBody AppointmentDTO appointmentDTO) {
        return appointmentService.updateAppointment(id, appointmentDTO);
    }

    @DeleteMapping("/{id}")
    public void deleteAppointment(@PathVariable Long id) {
        appointmentService.deleteAppointment(id);
    }
}
