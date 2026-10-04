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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.stereotype.Controller;
import org.springframework.context.annotation.Profile;

import java.util.List;

@Controller
@Profile("legacy-ui")
@RequestMapping("/appointments")
public class AppointmentController {

    @Autowired
    private AppointmentService appointmentService;

    @Autowired
    private PatientService patientService;

    // Vista HTML con lista de turnos
    @GetMapping("/list")
    public String listAppointments(Model model) {
        model.addAttribute("appointments", appointmentService.getAllAppointments());
        return "appointments"; // plantilla appointments.html
    }

    // API REST: obtener todos los turnos
    @GetMapping
    @ResponseBody
    public List<AppointmentDTO> getAllAppointments() {
        return appointmentService.getAllAppointments();
    }

    // API REST: obtener turno por ID
    @GetMapping("/{id}")
    @ResponseBody
    public AppointmentDTO getAppointmentById(@PathVariable Long id) {
        return appointmentService.getAppointmentById(id);
    }
    
    // API REST: buscar paciente por DNI
    @GetMapping("/findPatientByDni/{dni}")
    @ResponseBody
    public PatientDTO findPatientByDni(@PathVariable String dni) {
        return patientService.findByDni(dni);
    }

    // API REST: turnos disponibles
    @GetMapping("/available")
    @ResponseBody
    public ResponseEntity<List<AppointmentDTO>> getAvailableAppointments() {
        try {
            List<AppointmentDTO> appointments = appointmentService.getAvailableAppointments();
            return new ResponseEntity<>(appointments, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(List.of(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // Vista HTML: formulario para asignar turno
    @GetMapping("/assign/{patientId}")
    public String showAssignAppointmentForm(@PathVariable Long patientId, Model model) {
        model.addAttribute("patientId", patientId);
        return "assign_appointment"; // plantilla assign_appointment.html
    }

     // API REST: crear turno
    @PostMapping
    @ResponseBody
    public AppointmentDTO createAppointment(@RequestBody AppointmentDTO appointmentDTO) {
        return appointmentService.saveAppointment(appointmentDTO);
    }

    // API REST: solicitar turno
    @PostMapping("/request")
    @ResponseBody
    public AppointmentDTO requestAppointment(@RequestBody AppointmentRequest appointmentRequest) {
        return appointmentService.requestAppointment(
                appointmentRequest.getAppointmentDTO(),
                appointmentRequest.getPatientDTO()
        );
    }

    // Confirmar turno (texto simple)
    @PostMapping("/confirm/{appointmentId}/{patientId}")
    @ResponseBody
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

    // Guardar turno desde formulario HTML
    @PostMapping("/save")
    public String saveAppointment(@ModelAttribute AppointmentDTO appointmentDTO, RedirectAttributes redirectAttributes) {
        try {
            appointmentService.saveAppointment(appointmentDTO);
            redirectAttributes.addFlashAttribute("success", "Turno asignado correctamente.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al asignar el turno.");
        }
        return "redirect:/appointments/list"; // redirige a la vista HTML
    }

    // API REST: actualizar turno
    @PutMapping("/{id}")
    @ResponseBody
    public AppointmentDTO updateAppointment(@PathVariable Long id, @RequestBody AppointmentDTO appointmentDTO) {
        return appointmentService.updateAppointment(id, appointmentDTO);
    }

    // API REST: eliminar turno
    @DeleteMapping("/{id}")
    @ResponseBody
    public void deleteAppointment(@PathVariable Long id) {
        appointmentService.deleteAppointment(id);
    }
}
