package com.api.agenda_odontologica.api.repository;

import com.api.agenda_odontologica.api.entity.AppointmentRecord;
import com.api.agenda_odontologica.api.entity.AppointmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/** Todas las consultas exigen ownerId (nunca null): un turno ajeno no existe para el llamador. */
public interface AppointmentRecordRepository extends JpaRepository<AppointmentRecord, Long> {
    Optional<AppointmentRecord> findByIdAndOwnerId(Long id, Long ownerId);

    boolean existsByPatientIdAndOwnerId(Long patientId, Long ownerId);

    long countByOwnerId(Long ownerId);

    @Query("""
            select (count(a) > 0) from AppointmentRecord a
            where a.ownerId = :ownerId
              and a.estado <> :cancelled
              and a.startAt < :endAt
              and a.endAt > :startAt
            """)
    boolean hasOverlap(
            @Param("ownerId") Long ownerId,
            @Param("startAt") Instant startAt,
            @Param("endAt") Instant endAt,
            @Param("cancelled") AppointmentStatus cancelled);

    @Query("""
            select (count(a) > 0) from AppointmentRecord a
            where a.ownerId = :ownerId
              and a.estado <> :cancelled
              and a.startAt < :endAt
              and a.endAt > :startAt
              and a.id <> :excludeId
            """)
    boolean hasOverlapExcluding(
            @Param("ownerId") Long ownerId,
            @Param("startAt") Instant startAt,
            @Param("endAt") Instant endAt,
            @Param("cancelled") AppointmentStatus cancelled,
            @Param("excludeId") Long excludeId);

    @Query("""
            select a from AppointmentRecord a
            join fetch a.patient p
            where a.ownerId = :ownerId
              and a.startAt < :to and a.endAt > :from
              and (:search = ''
                   or lower(p.nombre) like lower(concat('%', :search, '%'))
                   or lower(p.apellido) like lower(concat('%', :search, '%'))
                   or lower(p.dni) like lower(concat('%', :search, '%')))
            order by a.startAt
            """)
    List<AppointmentRecord> findInRange(
            @Param("ownerId") Long ownerId,
            @Param("from") Instant from,
            @Param("to") Instant to,
            @Param("search") String search);

    @Query("""
            select a from AppointmentRecord a
            join fetch a.patient p
            where a.ownerId = :ownerId
              and a.startAt < :to and a.endAt > :from
              and a.estado = :status
              and (:search = ''
                   or lower(p.nombre) like lower(concat('%', :search, '%'))
                   or lower(p.apellido) like lower(concat('%', :search, '%'))
                   or lower(p.dni) like lower(concat('%', :search, '%')))
            order by a.startAt
            """)
    List<AppointmentRecord> findInRangeByStatus(
            @Param("ownerId") Long ownerId,
            @Param("from") Instant from,
            @Param("to") Instant to,
            @Param("status") AppointmentStatus status,
            @Param("search") String search);
}
