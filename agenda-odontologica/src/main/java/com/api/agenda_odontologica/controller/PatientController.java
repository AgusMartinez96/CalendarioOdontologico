package com.api.agenda_odontologica.controller;

import com.api.agenda_odontologica.dto.PatientDTO;
import com.api.agenda_odontologica.service.PatientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/patients")
public class PatientController {

    @Autowired
    private PatientService patientService;

    @GetMapping
    public List<PatientDTO> getAllPatients() {
        return patientService.getAllPatients();
    }

    @GetMapping("/{id}")
    public PatientDTO getPatientById(@PathVariable Long id) {
        return patientService.getPatientById(id);
    }

    // Mostrar formulario para agregar paciente
    @GetMapping("/add")
    public String showAddPatientForm(Model model) {
        model.addAttribute("patient", new PatientDTO());
        return "add_patient"; // Muestra la plantilla add_patient.html
    }

    //Busqueda avanzada por nombre, dni u obra social
    @GetMapping("/search")
    public List<PatientDTO> searchPatients(
            @RequestParam(required = false) String nombre,
            @RequestParam(required = false) String dni,
            @RequestParam(required = false) String obraSocial) {
        return patientService.searchPatients(nombre, dni, obraSocial);
    }

    // Guardar paciente en la base de datos
    @PostMapping("/save")
    public String savePatient(@ModelAttribute PatientDTO patient, RedirectAttributes redirectAttributes) {
    try {
        patientService.savePatient(patient);
        redirectAttributes.addFlashAttribute("success", "Paciente guardado correctamente.");
    } catch (Exception e) {
        redirectAttributes.addFlashAttribute("error", "Error al guardar el paciente.");
    }
    return "redirect:/home";
}

    @PostMapping
    public PatientDTO createPatient(@RequestBody PatientDTO patientDTO) {
        return patientService.savePatient(patientDTO);
    }

    @PutMapping("/{id}")
    public PatientDTO updatePatient(@PathVariable Long id, @RequestBody PatientDTO patientDTO) {
        return patientService.updatePatient(id, patientDTO);
    }

    @DeleteMapping("/{id}")
    public void deletePatient(@PathVariable Long id) {
        patientService.deletePatient(id);
    }
}