package com.api.agenda_odontologica.api.controller;

import com.api.agenda_odontologica.support.ApiClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, properties = {
        "spring.datasource.url=jdbc:h2:mem:regratelimit;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false",
        "app.admin.username=test-admin",
        "app.admin.password=test-password",
        "app.registration.max-users=100",
        "app.registration.max-per-ip-per-hour=2"
})
@AutoConfigureMockMvc
class RegistrationRateLimitApiTest {
    private static final String PASSWORD = "una-clave-larga-y-rara";

    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper mapper;

    @Test
    void answers429WithRetryAfterAfterTheConfiguredAttemptsPerIp() throws Exception {
        ApiClient api = new ApiClient(mvc, mapper);
        String ip = "198.51.100.5";

        api.send(HttpMethod.POST, "/api/auth/register", creds("rl.uno"), null, ip).andExpect(status().isCreated());
        api.send(HttpMethod.POST, "/api/auth/register", creds("rl.dos"), null, ip).andExpect(status().isCreated());

        MvcResult blocked = api.send(HttpMethod.POST, "/api/auth/register", creds("rl.tres"), null, ip)
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.status").value(429))
                .andExpect(jsonPath("$.path").value("/api/auth/register"))
                .andReturn();
        String retryAfter = blocked.getResponse().getHeader("Retry-After");
        assertNotNull(retryAfter);
        long seconds = Long.parseLong(retryAfter);
        assertTrue(seconds > 0 && seconds <= 3600, "Retry-After=" + seconds);

        api.send(HttpMethod.POST, "/api/auth/register", creds("rl.tres"), null, "198.51.100.6")
                .andExpect(status().isCreated());
    }

    @Test
    void invalidRequestsDoNotConsumeAttempts() throws Exception {
        ApiClient api = new ApiClient(mvc, mapper);
        String ip = "198.51.100.9";
        for (int i = 0; i < 5; i++) {
            api.send(HttpMethod.POST, "/api/auth/register", Map.of("username", "x", "password", "corta"), null, ip)
                    .andExpect(status().isBadRequest());
        }
        api.send(HttpMethod.POST, "/api/auth/register", creds("rl.valido"), null, ip)
                .andExpect(status().isCreated());
    }

    private static Map<String, String> creds(String username) {
        return Map.of("username", username, "password", PASSWORD);
    }
}
