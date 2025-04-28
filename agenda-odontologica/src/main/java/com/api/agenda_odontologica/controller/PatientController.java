package com.api.agenda_odontologica.controller;

import com.api.agenda_odontologica.dto.PatientDTO;
import com.api.agenda_odontologica.service.PatientService;
import org.springframework.ui.Model;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
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
    public String savePatient(@ModelAttribute PatientDTO patientDTO) {
        patientService.savePatient(patientDTO);
        return "redirect:/dashboard"; // Redirige de nuevo al panel después de guardar
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