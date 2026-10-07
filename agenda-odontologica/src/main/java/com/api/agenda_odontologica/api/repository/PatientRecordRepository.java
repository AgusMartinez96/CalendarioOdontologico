package com.api.agenda_odontologica.api.repository;

import com.api.agenda_odontologica.api.entity.PatientRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/** Todas las consultas exigen ownerId (nunca null): un paciente ajeno no existe para el llamador. */
public interface PatientRecordRepository extends JpaRepository<PatientRecord, Long> {
    Optional<PatientRecord> findByIdAndOwnerId(Long id, Long ownerId);

    boolean existsByOwnerIdAndDni(Long ownerId, String dni);

    boolean existsByOwnerIdAndDniAndIdNot(Long ownerId, String dni, Long id);

    long countByOwnerId(Long ownerId);

    @Query("""
            select p from PatientRecord p
            where p.ownerId = :ownerId
              and (:search = ''
               or lower(p.nombre) like lower(concat('%', :search, '%'))
               or lower(p.apellido) like lower(concat('%', :search, '%'))
               or lower(p.dni) like lower(concat('%', :search, '%'))
               or lower(p.telefono) like lower(concat('%', :search, '%')))
            order by p.apellido, p.nombre
            """)
    List<PatientRecord> search(@Param("ownerId") Long ownerId, @Param("search") String search);
}
