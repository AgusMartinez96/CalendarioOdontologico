package com.api.agenda_odontologica.api.controller;

import com.api.agenda_odontologica.api.entity.UserAccount;
import com.api.agenda_odontologica.api.repository.UserAccountRepository;
import com.api.agenda_odontologica.support.ApiClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, properties = {
        "spring.datasource.url=jdbc:h2:mem:registration;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false",
        "app.admin.username=test-admin",
        "app.admin.password=test-password",
        "app.registration.max-users=1000",
        "app.registration.max-per-ip-per-hour=1000"
})
@AutoConfigureMockMvc
class RegistrationApiTest {
    private static final String REGISTER = "/api/auth/register";
    private static final String GOOD_PASSWORD = "una-clave-larga-y-rara";

    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper mapper;
    @Autowired
    private UserAccountRepository users;

    private ApiClient api;
    private int ipCounter;

    @BeforeEach
    void setUp() {
        api = new ApiClient(mvc, mapper);
    }

    @Test
    void registersStartsSessionAndNeverExposesPasswordOrHash() throws Exception {
        MvcResult result = api.send(HttpMethod.POST, REGISTER, credentials("maria.perez", GOOD_PASSWORD), null,
                        nextIp())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.authenticated").value(true))
                .andExpect(jsonPath("$.username").value("maria.perez"))
                .andReturn();
        String body = result.getResponse().getContentAsString();
        assertFalse(body.contains(GOOD_PASSWORD));
        assertFalse(body.contains("$2"));
        assertFalse(body.toLowerCase().contains("hash"));

        MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);
        assertNotNull(session);
        mvc.perform(get("/api/auth/session").session(session))
                .andExpect(jsonPath("$.authenticated").value(true))
                .andExpect(jsonPath("$.username").value("maria.perez"));
        api.send(HttpMethod.GET, "/api/v1/patients", null, session).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        UserAccount stored = users.findByUsernameNormalized("maria.perez").orElseThrow();
        assertEquals(UserAccount.ROLE_USER, stored.getRole());
        assertTrue(stored.getPasswordHash().startsWith("$2"), "se guarda con BCrypt");
        assertFalse(stored.getPasswordHash().contains(GOOD_PASSWORD));
        assertNotNull(stored.getLastLoginAt());

        api.login("maria.perez", GOOD_PASSWORD);
        api.send(HttpMethod.POST, "/api/auth/logout", null, session).andExpect(status().isNoContent());
    }

    @Test
    void rejectsUsernameThatDiffersOnlyByCase() throws Exception {
        api.send(HttpMethod.POST, REGISTER, credentials("Carlos99", GOOD_PASSWORD), null, nextIp())
                .andExpect(status().isCreated());

        api.send(HttpMethod.POST, REGISTER, credentials("cArLoS99", GOOD_PASSWORD), null, nextIp())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Ese nombre de usuario ya está en uso"))
                .andExpect(jsonPath("$.path").value(REGISTER));
        api.send(HttpMethod.POST, REGISTER, credentials("TEST-ADMIN", GOOD_PASSWORD), null, nextIp())
                .andExpect(status().isConflict());
    }

    @Test
    void simultaneousRegistrationsOfTheSameNameYieldOneWinnerAndNeverA500() throws Exception {
        int attempts = 6;
        ExecutorService pool = Executors.newFixedThreadPool(attempts);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Integer>> results = new ArrayList<>();
        for (int i = 0; i < attempts; i++) {
            String ip = nextIp();
            String name = i % 2 == 0 ? "Carrera" : "carrera";
            results.add(pool.submit(() -> {
                start.await();
                return api.send(HttpMethod.POST, REGISTER, credentials(name, GOOD_PASSWORD), null, ip)
                        .andReturn().getResponse().getStatus();
            }));
        }
        start.countDown();
        int created = 0;
        int conflicts = 0;
        for (Future<Integer> result : results) {
            int code = result.get();
            if (code == 201) {
                created++;
            } else if (code == 409) {
                conflicts++;
            } else {
                throw new AssertionError("estado inesperado: " + code);
            }
        }
        pool.shutdown();
        assertEquals(1, created);
        assertEquals(attempts - 1, conflicts);
    }

    @Test
    void validatesUsernameRules() throws Exception {
        for (String invalid : new String[] {"ab", "a".repeat(31), "con espacio", "ñandú", "_guion", ".punto",
                "-menos", "us@er", "user!", " padded", ""}) {
            api.send(HttpMethod.POST, REGISTER, credentials(invalid, GOOD_PASSWORD), null, nextIp())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors[0].field").value("username"));
        }
        for (String valid : new String[] {"abc", "a".repeat(30), "9lives", "a.b-c_d", "A1b2"}) {
            api.send(HttpMethod.POST, REGISTER, credentials(valid, GOOD_PASSWORD), null, nextIp())
                    .andExpect(status().isCreated());
        }
    }

    @Test
    void validatesPasswordRules() throws Exception {
        assertPasswordRejected("usuario.uno", "corta123");
        assertPasswordRejected("usuario.dos", "USUARIO.DOS");
        assertPasswordRejected("usuario.tres", "Password123");
        assertPasswordRejected("usuario.cuatro", "1234567890");
        assertPasswordRejected("usuario.cinco", "a".repeat(73));
        assertPasswordRejected("usuario.seis", "ñ".repeat(37));

        api.send(HttpMethod.POST, REGISTER, credentials("usuario.siete", "a".repeat(72)), null, nextIp())
                .andExpect(status().isCreated());
        api.send(HttpMethod.POST, REGISTER, credentials("usuario.ocho", "ñ".repeat(36)), null, nextIp())
                .andExpect(status().isCreated());
        api.send(HttpMethod.POST, REGISTER, credentials("usuario.nueve", "   espacios   "), null, nextIp())
                .andExpect(status().isCreated());
        api.login("usuario.nueve", "   espacios   ");
        api.send(HttpMethod.POST, REGISTER, "{\"username\":\"sin.clave\"}", null, nextIp())
                .andExpect(status().isBadRequest());
        api.send(HttpMethod.POST, REGISTER, "no es json", null, nextIp()).andExpect(status().isBadRequest());
    }

    @Test
    void requiresCsrfToken() throws Exception {
        mvc.perform(post(REGISTER).contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(credentials("sin.csrf.user", GOOD_PASSWORD))))
                .andExpect(status().isForbidden());
        assertFalse(users.existsByUsernameNormalized("sin.csrf.user"));
    }

    @Test
    void loginFailsIdenticallyForUnknownUserAndWrongPassword() throws Exception {
        api.send(HttpMethod.POST, REGISTER, credentials("existente", GOOD_PASSWORD), null, nextIp())
                .andExpect(status().isCreated());

        MvcResult wrongPassword = api.send(HttpMethod.POST, "/api/auth/login",
                credentials("existente", "otra-clave-equivocada"), null, "203.0.113.1").andReturn();
        MvcResult unknownUser = api.send(HttpMethod.POST, "/api/auth/login",
                credentials("no-existe-nadie", "otra-clave-equivocada"), null, "203.0.113.2").andReturn();

        assertEquals(401, wrongPassword.getResponse().getStatus());
        assertEquals(wrongPassword.getResponse().getStatus(), unknownUser.getResponse().getStatus());
        JsonNode a = mapper.readTree(wrongPassword.getResponse().getContentAsString());
        JsonNode b = mapper.readTree(unknownUser.getResponse().getContentAsString());
        assertEquals(a.get("message").asText(), b.get("message").asText());
        assertEquals(a.get("error").asText(), b.get("error").asText());
    }

    @Test
    void loginUpdatesLastLoginAndConfigReportsRegistrationOpen() throws Exception {
        api.send(HttpMethod.POST, REGISTER, credentials("ultimo.acceso", GOOD_PASSWORD), null, nextIp())
                .andExpect(status().isCreated());
        UserAccount before = users.findByUsernameNormalized("ultimo.acceso").orElseThrow();
        java.time.Instant first = before.getLastLoginAt();
        Thread.sleep(20);

        api.login("ultimo.acceso", GOOD_PASSWORD);

        assertTrue(users.findByUsernameNormalized("ultimo.acceso").orElseThrow()
                .getLastLoginAt().isAfter(first));
        mvc.perform(get("/api/auth/config")).andExpect(status().isOk())
                .andExpect(jsonPath("$.registrationOpen").value(true));
    }

    @Test
    void sendsSecurityHeadersAndNeverLeaksInternalErrorDetails() throws Exception {
        MvcResult result = mvc.perform(get("/api/auth/config")).andExpect(status().isOk()).andReturn();
        var response = result.getResponse();
        assertEquals("nosniff", response.getHeader("X-Content-Type-Options"));
        assertEquals("DENY", response.getHeader("X-Frame-Options"));
        assertEquals("strict-origin-when-cross-origin", response.getHeader("Referrer-Policy"));
        String csp = response.getHeader("Content-Security-Policy");
        assertNotNull(csp);
        assertTrue(csp.contains("script-src 'self';") && csp.contains("frame-ancestors 'none'"));
        assertFalse(csp.contains("script-src 'self' 'unsafe-inline'"));
        assertEquals(null, response.getHeader("Strict-Transport-Security"), "HSTS solo en prod");

        String body = mvc.perform(get("/api/v1/no-existe").session(api.login("test-admin", "test-password")))
                .andExpect(status().isNotFound()).andReturn().getResponse().getContentAsString();
        assertFalse(body.contains("Exception") || body.contains("\tat ") || body.contains("trace"));
    }

    private void assertPasswordRejected(String username, String password) throws Exception {
        api.send(HttpMethod.POST, REGISTER, credentials(username, password), null, nextIp())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("password"));
        assertFalse(users.existsByUsernameNormalized(username.toLowerCase()));
    }

    private String nextIp() {
        return "192.0.2." + (++ipCounter % 250 + 1) + "";
    }

    private static Map<String, String> credentials(String username, String password) {
        return Map.of("username", username, "password", password);
    }
}
