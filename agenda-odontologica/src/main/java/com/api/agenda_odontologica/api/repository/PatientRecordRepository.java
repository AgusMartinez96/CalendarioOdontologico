package com.api.agenda_odontologica.api.repository;

import com.api.agenda_odontologica.api.entity.PatientRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PatientRecordRepository extends JpaRepository<PatientRecord, Long> {
    boolean existsByDni(String dni);

    boolean existsByDniAndIdNot(String dni, Long id);

    @Query("""
            select p from PatientRecord p
            where :search is null
               or lower(p.nombre) like lower(concat('%', :search, '%'))
               or lower(p.apellido) like lower(concat('%', :search, '%'))
               or lower(p.dni) like lower(concat('%', :search, '%'))
               or lower(p.telefono) like lower(concat('%', :search, '%'))
            order by p.apellido, p.nombre
            """)
    List<PatientRecord> search(@Param("search") String search);
}
