package com.api.agenda_odontologica.repository;

import com.api.agenda_odontologica.entity.Patient;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PatientRepository extends JpaRepository<Patient, Long> {
    List<Patient> findByNombreContainingIgnoreCase(String nombre); // Busqueda parcial por nombre
    Optional<Patient> findByDni(String dni); // Busqueda exacta por DNI
    List<Patient> findByObraSocialContainingIgnoreCase(String obraSocial); // Busqueda parcial por obra social
}