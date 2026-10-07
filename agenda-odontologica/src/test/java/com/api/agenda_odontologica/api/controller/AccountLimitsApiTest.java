package com.api.agenda_odontologica.api.controller;

import com.api.agenda_odontologica.support.ApiClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, properties = {
        "spring.datasource.url=jdbc:h2:mem:accountlimits;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false",
        "app.admin.username=test-admin",
        "app.admin.password=test-password",
        "app.registration.max-users=100",
        "app.registration.max-per-ip-per-hour=100",
        "app.limits.max-patients-per-user=2",
        "app.limits.max-appointments-per-user=2"
})
@AutoConfigureMockMvc
class AccountLimitsApiTest {
    private static final String PASSWORD = "una-clave-larga-y-rara";

    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper mapper;

    @Test
    void enforcesPatientAndAppointmentCapsPerAccountOnly() throws Exception {
        ApiClient api = new ApiClient(mvc, mapper);
        MockHttpSession first = register(api, "tope.uno", "198.51.100.21");
        MockHttpSession second = register(api, "tope.dos", "198.51.100.22");

        long p1 = patient(api, first, "1");
        long p2 = patient(api, first, "2");
        api.send(HttpMethod.POST, "/api/v1/patients", patientBody("3"), first)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Llegaste al máximo de 2 pacientes por cuenta."))
                .andExpect(jsonPath("$.path").value("/api/v1/patients"));
        patient(api, second, "1");

        appointment(api, first, p1, 30);
        appointment(api, first, p2, 31);
        api.send(HttpMethod.POST, "/api/v1/appointments", appointmentBody(p1, 32), first)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Llegaste al máximo de 2 turnos por cuenta."));
        long other = patient(api, second, "9");
        appointment(api, second, other, 30);
    }

    private MockHttpSession register(ApiClient api, String username, String ip) throws Exception {
        MvcResult result = api.send(HttpMethod.POST, "/api/auth/register",
                Map.of("username", username, "password", PASSWORD), null, ip)
                .andExpect(status().isCreated()).andReturn();
        return (MockHttpSession) result.getRequest().getSession(false);
    }

    private long patient(ApiClient api, MockHttpSession session, String dni) throws Exception {
        return api.json(api.send(HttpMethod.POST, "/api/v1/patients", patientBody(dni), session)
                .andExpect(status().isCreated())).get("id").asLong();
    }

    private void appointment(ApiClient api, MockHttpSession session, long patientId, int days) throws Exception {
        api.send(HttpMethod.POST, "/api/v1/appointments", appointmentBody(patientId, days), session)
                .andExpect(status().isCreated());
    }

    private static Map<String, Object> patientBody(String dni) {
        return Map.of("nombre", "Ana", "apellido", "Pérez", "dni", dni, "telefono", "1100000000");
    }

    private static Map<String, Object> appointmentBody(long patientId, int days) {
        Instant start = Instant.now().truncatedTo(ChronoUnit.DAYS).plus(days, ChronoUnit.DAYS)
                .plus(12, ChronoUnit.HOURS);
        return Map.of("startAt", start.toString(), "patientId", patientId, "motivo", "Consulta");
    }
}
