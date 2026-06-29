package com.api.agenda_odontologica.service;

import com.api.agenda_odontologica.dto.AppointmentDTO;
import com.api.agenda_odontologica.dto.PatientDTO;
import com.api.agenda_odontologica.entity.Appointment;
import com.api.agenda_odontologica.entity.Patient;
import com.api.agenda_odontologica.repository.AppointmentRepository;
import com.api.agenda_odontologica.repository.PatientRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class AppointmentService {

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private PatientService patientService;

    public List<AppointmentDTO> getAllAppointments() {
        return appointmentRepository.findAll().stream()
                .map(AppointmentDTO::new)
                .collect(Collectors.toList());
    }

    public AppointmentDTO getAppointmentById(Long id) {
        return appointmentRepository.findById(id)
                .map(AppointmentDTO::new)
                .orElse(null);
    }

    public AppointmentDTO saveAppointment(AppointmentDTO appointmentDTO) {
        Appointment appointment = new Appointment();
        appointment.setFecha(appointmentDTO.getFecha());
        appointment.setAsistencia(appointmentDTO.getAsistencia());

        if (appointmentDTO.getPatientId() != null) {
            Patient patient = patientRepository.findById(appointmentDTO.getPatientId()).orElse(null);
            appointment.setPatient(patient);
        }

        appointment = appointmentRepository.save(appointment);
        return new AppointmentDTO(appointment);
    }

    public AppointmentDTO requestAppointment(AppointmentDTO appointmentDTO, PatientDTO patientDTO) {
        PatientDTO existingPatient = patientService.findOrCreatePatient(patientDTO);
        appointmentDTO.setPatientId(existingPatient.getId());
        return saveAppointment(appointmentDTO);
    }


        public AppointmentDTO updateAppointment (Long id, AppointmentDTO appointmentDTO){
            Optional<Appointment> optionalAppointment = appointmentRepository.findById(id);

            if (optionalAppointment.isPresent()) {
                Appointment appointment = optionalAppointment.get();
                appointment.setFecha(appointmentDTO.getFecha());
                appointment.setAsistencia(appointmentDTO.getAsistencia());

                if (appointmentDTO.getPatientId() != null) {
                    Patient patient = patientRepository.findById(appointmentDTO.getPatientId()).orElse(null);
                    appointment.setPatient(patient);
                }

                appointment = appointmentRepository.save(appointment);
                return new AppointmentDTO(appointment);
            } else {
                return null;
            }
        }

    public void deleteAppointment(Long id) {
        appointmentRepository.deleteById(id);
    }

    public List<AppointmentDTO> getAvailableAppointments() {
        return appointmentRepository.findByAsistenciaFalse().stream()
                .map(AppointmentDTO::new)
                .collect(Collectors.toList());
    }
}
