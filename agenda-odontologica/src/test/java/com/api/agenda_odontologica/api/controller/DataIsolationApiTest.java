package com.api.agenda_odontologica.api.controller;

import com.api.agenda_odontologica.api.entity.UserAccount;
import com.api.agenda_odontologica.api.repository.UserAccountRepository;
import com.api.agenda_odontologica.support.ApiClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, properties = {
        "spring.datasource.url=jdbc:h2:mem:isolation;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false",
        "app.admin.username=test-admin",
        "app.admin.password=test-password"
})
@AutoConfigureMockMvc
class DataIsolationApiTest {
    private static final String P = "/api/v1/patients";
    private static final String A = "/api/v1/appointments";

    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper mapper;
    @Autowired
    private UserAccountRepository users;
    @Autowired
    private PasswordEncoder encoder;
    @Autowired
    private com.api.agenda_odontologica.api.repository.AppointmentRecordRepository appointmentRepo;
    @Autowired
    private com.api.agenda_odontologica.api.repository.PatientRecordRepository patientRepo;

    private ApiClient api;

    @BeforeEach
    void setUp() {
        api = new ApiClient(mvc, mapper);
        appointmentRepo.deleteAll();
        patientRepo.deleteAll();
        for (String name : new String[] {"alice", "bob"}) {
            if (!users.existsByUsernameNormalized(name)) {
                users.save(new UserAccount(name, encoder.encode("password-" + name), UserAccount.ROLE_USER));
            }
        }
    }

    @Test
    void accountCannotReadEditDeleteOrCancelAnotherAccountsData() throws Exception {
        MockHttpSession alice = api.login("alice", "password-alice");
        MockHttpSession bob = api.login("bob", "password-bob");
        long patientId = createPatient(alice, "111");
        long appointmentId = createAppointment(alice, patientId, 40);

        api.send(HttpMethod.GET, P + "/" + patientId, null, bob).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404)).andExpect(jsonPath("$.path").value(P + "/" + patientId));
        api.send(HttpMethod.PUT, P + "/" + patientId, patientBody("111"), bob).andExpect(status().isNotFound());
        api.send(HttpMethod.DELETE, P + "/" + patientId, null, bob).andExpect(status().isNotFound());
        api.send(HttpMethod.GET, A + "/" + appointmentId, null, bob).andExpect(status().isNotFound());
        api.send(HttpMethod.PUT, A + "/" + appointmentId, appointmentBody(patientId, 41), bob)
                .andExpect(status().isNotFound());
        api.send(HttpMethod.PATCH, A + "/" + appointmentId + "/cancel", null, bob)
                .andExpect(status().isNotFound());

        api.send(HttpMethod.GET, P, null, bob).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
        api.send(HttpMethod.GET, P + "?search=111", null, bob).andExpect(jsonPath("$.length()").value(0));
        api.send(HttpMethod.GET, listUrl(), null, bob).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        api.send(HttpMethod.GET, P + "/" + patientId, null, alice).andExpect(status().isOk());
        api.send(HttpMethod.GET, A + "/" + appointmentId, null, alice).andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("PROGRAMADO"));
    }

    @Test
    void adminAlsoCannotSeeAnotherAccountsData() throws Exception {
        MockHttpSession alice = api.login("alice", "password-alice");
        MockHttpSession admin = api.login("test-admin", "test-password");
        long patientId = createPatient(alice, "222");
        long appointmentId = createAppointment(alice, patientId, 42);

        api.send(HttpMethod.GET, P + "/" + patientId, null, admin).andExpect(status().isNotFound());
        api.send(HttpMethod.GET, A + "/" + appointmentId, null, admin).andExpect(status().isNotFound());
        api.send(HttpMethod.GET, P, null, admin).andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void twoAccountsCanRepeatDniAndTimeSlot() throws Exception {
        MockHttpSession alice = api.login("alice", "password-alice");
        MockHttpSession bob = api.login("bob", "password-bob");
        long alicePatient = createPatient(alice, "333");
        long bobPatient = createPatient(bob, "333");
        createAppointment(alice, alicePatient, 50);
        createAppointment(bob, bobPatient, 50);

        api.send(HttpMethod.POST, P, patientBody("333"), alice).andExpect(status().isConflict());
        api.send(HttpMethod.POST, A, appointmentBody(alicePatient, 50), alice).andExpect(status().isConflict());
    }

    @Test
    void cannotCreateOrMoveAppointmentWithAnotherAccountsPatient() throws Exception {
        MockHttpSession alice = api.login("alice", "password-alice");
        MockHttpSession bob = api.login("bob", "password-bob");
        long alicePatient = createPatient(alice, "444");
        long bobPatient = createPatient(bob, "445");
        long bobAppointment = createAppointment(bob, bobPatient, 60);

        api.send(HttpMethod.POST, A, appointmentBody(alicePatient, 61), bob).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("No se encontró el paciente."));
        api.send(HttpMethod.PUT, A + "/" + bobAppointment, appointmentBody(alicePatient, 62), bob)
                .andExpect(status().isNotFound());
    }

    @Test
    void ownerIsNeverTakenFromTheRequestBody() throws Exception {
        MockHttpSession alice = api.login("alice", "password-alice");
        MockHttpSession bob = api.login("bob", "password-bob");
        Long bobId = users.findByUsernameNormalized("bob").orElseThrow().getId();
        Map<String, Object> body = new LinkedHashMap<>(patientBody("555"));
        body.put("ownerId", bobId);
        body.put("owner_id", bobId);
        long id = api.json(api.send(HttpMethod.POST, P, body, alice).andExpect(status().isCreated()))
                .get("id").asLong();

        api.send(HttpMethod.GET, P + "/" + id, null, alice).andExpect(status().isOk());
        api.send(HttpMethod.GET, P + "/" + id, null, bob).andExpect(status().isNotFound());
        assertEquals(0, api.json(api.send(HttpMethod.GET, P + "?ownerId=" + bobId + "&search=555", null, bob))
                .size());
        assertEquals(1, api.json(api.send(HttpMethod.GET, P + "?ownerId=" + bobId + "&search=555", null, alice))
                .size());
    }

    private long createPatient(MockHttpSession session, String dni) throws Exception {
        return api.json(api.send(HttpMethod.POST, P, patientBody(dni), session)
                .andExpect(status().isCreated())).get("id").asLong();
    }

    private long createAppointment(MockHttpSession session, long patientId, int daysAhead) throws Exception {
        return api.json(api.send(HttpMethod.POST, A, appointmentBody(patientId, daysAhead), session)
                .andExpect(status().isCreated())).get("id").asLong();
    }

    private static Map<String, Object> patientBody(String dni) {
        return Map.of("nombre", "Ana", "apellido", "Pérez", "dni", dni, "telefono", "1100000000");
    }

    private static Map<String, Object> appointmentBody(long patientId, int daysAhead) {
        return Map.of("startAt", start(daysAhead).toString(), "patientId", patientId, "motivo", "Consulta");
    }

    private static Instant start(int daysAhead) {
        return Instant.now().truncatedTo(ChronoUnit.DAYS).plus(daysAhead, ChronoUnit.DAYS).plus(12, ChronoUnit.HOURS);
    }

    private static String listUrl() {
        return A + "?from=" + Instant.now().minus(1, ChronoUnit.DAYS)
                + "&to=" + Instant.now().plus(400, ChronoUnit.DAYS);
    }
}
