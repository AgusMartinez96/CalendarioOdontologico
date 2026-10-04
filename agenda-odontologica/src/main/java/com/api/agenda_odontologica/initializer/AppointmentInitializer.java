package com.api.agenda_odontologica.initializer;

import com.api.agenda_odontologica.entity.Appointment;
import com.api.agenda_odontologica.repository.AppointmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.context.annotation.Profile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
@Profile("legacy-ui")
public class AppointmentInitializer implements CommandLineRunner {

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Override
    public void run(String... args) throws Exception {
        // Definir el rango de fechas: ahora hasta 2 meses adelante
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime twoMonthsAhead = now.plusMonths(2);

        // Verificar turnos existentes en el rango
        List<Appointment> existingAppointments = appointmentRepository.findAll();

        List<LocalDateTime> existingDates = existingAppointments.stream()
                .map(Appointment::getFecha)
                .filter(date -> !date.isBefore(now) && !date.isAfter(twoMonthsAhead))
                .toList();

        List<Appointment> appointments = new ArrayList<>();

        // Generar turnos para cada lunes y viernes hasta 2 meses adelante
        LocalDateTime startDate = now;
        while (startDate.isBefore(twoMonthsAhead)) {
            if (startDate.getDayOfWeek() == java.time.DayOfWeek.MONDAY ||
                    startDate.getDayOfWeek() == java.time.DayOfWeek.FRIDAY) {

                // Generar 13 turnos desde las 13:00 hasta las 19:30
                LocalDateTime appointmentTime = startDate.withHour(13).withMinute(0).withSecond(0).withNano(0);
                for (int i = 0; i < 13; i++) {
                    if (!existingDates.contains(appointmentTime)) {
                        appointments.add(createAppointment(appointmentTime));
                    }
                    appointmentTime = appointmentTime.plusMinutes(30); // Incremento de 30 minutos
                }
            }
            startDate = startDate.plusDays(1); // Avanzar al siguiente día
        }

        // Guardar nuevos turnos
        if (!appointments.isEmpty()) {
            appointmentRepository.saveAll(appointments);
        }
    }

    private Appointment createAppointment(LocalDateTime dateTime) {
        Appointment appointment = new Appointment();
        appointment.setFecha(dateTime);
        appointment.setAsistencia(false);
        return appointment;
    }
}
