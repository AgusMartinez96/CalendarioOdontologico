package com.api.agenda_odontologica.service;

import com.api.agenda_odontologica.dto.PatientDTO;
import com.api.agenda_odontologica.entity.Patient;
import com.api.agenda_odontologica.repository.PatientRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class PatientService {

    private final PatientRepository patientRepository;

  
    public PatientService(PatientRepository patientRepository) {
        this.patientRepository = patientRepository;
    }

    public List<PatientDTO> getAllPatients() {
        return patientRepository.findAll().stream()
                .map(PatientDTO::new)
                .collect(Collectors.toList());
    }

    public PatientDTO getPatientById(Long id) {
        return patientRepository.findById(id)
                .map(PatientDTO::new)
                .orElse(null);
    }

    public PatientDTO savePatient(PatientDTO patientDTO) {
        Patient patient = new Patient();
        patient.setNombre(patientDTO.getNombre());
        patient.setApellido(patientDTO.getApellido());
        patient.setDni(patientDTO.getDni());
        patient.setObraSocial(patientDTO.getObraSocial());
        patient = patientRepository.save(patient);
        return new PatientDTO(patient);
    }

    public PatientDTO updatePatient(Long id, PatientDTO patientDTO) {
        Optional<Patient> optionalPatient = patientRepository.findById(id);

        if (optionalPatient.isPresent()) {
            Patient patient = optionalPatient.get();
            patient.setNombre(patientDTO.getNombre());
            patient.setApellido(patientDTO.getApellido());
            patient.setDni(patientDTO.getDni());
            patient.setObraSocial(patientDTO.getObraSocial());

            patient = patientRepository.save(patient);
            return new PatientDTO(patient);
        } else {
            return null;
        }
    }
        public void deletePatient(Long id){
            patientRepository.deleteById(id);
        }

    public PatientDTO findOrCreatePatient(PatientDTO patientDTO) {
        Optional<Patient> optionalPatient = patientRepository.findByDni(patientDTO.getDni());

        if (optionalPatient.isPresent()) {
            return new PatientDTO(optionalPatient.get());
        } else {
            return savePatient(patientDTO);
        }
      }

    public PatientDTO findByDni(String dni) {
        Optional<Patient> optionalPatient = patientRepository.findByDni(dni);
        return optionalPatient.map(PatientDTO::new).orElse(null);
      }

    // NUEVO MÉTODO: Búsqueda dinámica de pacientes
    public List<PatientDTO> searchPatients(String nombre, String dni, String obraSocial) {
        if (nombre != null && !nombre.isEmpty()) {
            return patientRepository.findByNombreContainingIgnoreCase(nombre)
                    .stream().map(PatientDTO::new).toList();
        } else if (dni != null && !dni.isEmpty()) {
            return patientRepository.findByDni(dni)
                    .stream().map(PatientDTO::new).toList();
        } else if (obraSocial != null && !obraSocial.isEmpty()) {
            return patientRepository.findByObraSocialContainingIgnoreCase(obraSocial)
                    .stream().map(PatientDTO::new).toList();
        }
        // Si no se proporciona filtro, devuelve todos los pacientes
        return patientRepository.findAll()
                .stream().map(PatientDTO::new).toList();
    }
}
