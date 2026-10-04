package com.api.agenda_odontologica.controller;

import com.api.agenda_odontologica.dto.PatientDTO;
import com.api.agenda_odontologica.service.PatientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.context.annotation.Profile;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@Profile("legacy-ui")
@RequestMapping("/patients")
public class PatientController {

    @Autowired
    private PatientService patientService;

    // Vista HTML con la lista de pacientes
    @GetMapping("/list")
    public String listPatients(Model model) {
        model.addAttribute("patients", patientService.getAllPatients());
        return "patients"; // plantilla con la tabla
    }

    // API REST: obtener todos los pacientes en JSON
    @GetMapping
    @ResponseBody
    public List<PatientDTO> getAllPatients() {
        return patientService.getAllPatients();
    }

    // API REST: obtener paciente por ID en JSON
    @GetMapping("/{id}")
    public PatientDTO getPatientById(@PathVariable Long id) {
        return patientService.getPatientById(id);
    }

    // Formulario HTML para nuevo paciente
    @GetMapping("/new")
public String newPatientForm(Model model) {
    model.addAttribute("patient", new PatientDTO());
    return "add_patient";
    }

    //Busqueda avanzada (JSON) por nombre, dni u obra social
    @GetMapping("/search")
    public List<PatientDTO> searchPatients(
            @RequestParam(required = false) String nombre,
            @RequestParam(required = false) String dni,
            @RequestParam(required = false) String obraSocial) {
        return patientService.searchPatients(nombre, dni, obraSocial);
    }

    // Guardar paciente en la base de datos desde formulario HTML
    @PostMapping("/save")
    public String savePatient(@ModelAttribute PatientDTO patient, RedirectAttributes redirectAttributes) {
    try {
        patientService.savePatient(patient);
        redirectAttributes.addFlashAttribute("success", "Paciente guardado correctamente.");
    } catch (Exception e) {
        redirectAttributes.addFlashAttribute("error", "Error al guardar el paciente.");
    }
    return "redirect:/patients/list";
}
     // API REST: crear paciente en JSON
    @PostMapping
    public PatientDTO createPatient(@RequestBody PatientDTO patientDTO) {
        return patientService.savePatient(patientDTO);
    }

    // API REST: actualizar paciente en JSON
    @PutMapping("/{id}")
    public PatientDTO updatePatient(@PathVariable Long id, @RequestBody PatientDTO patientDTO) {
        return patientService.updatePatient(id, patientDTO);
    }

    // API REST: eliminar paciente
    @DeleteMapping("/{id}")
    public void deletePatient(@PathVariable Long id) {
        patientService.deletePatient(id);
    }
}