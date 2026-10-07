package com.api.agenda_odontologica.api.controller;

import com.api.agenda_odontologica.api.repository.UserAccountRepository;
import com.api.agenda_odontologica.support.ApiClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, properties = {
        "spring.datasource.url=jdbc:h2:mem:regdisabled;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false",
        "app.admin.username=test-admin",
        "app.admin.password=test-password",
        "app.registration.enabled=false"
})
@AutoConfigureMockMvc
class RegistrationRestrictionsApiTest {
    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper mapper;
    @Autowired
    private UserAccountRepository users;

    @Test
    void disabledRegistrationAnswers403AndConfigReportsClosed() throws Exception {
        new ApiClient(mvc, mapper).send(HttpMethod.POST, "/api/auth/register",
                        Map.of("username", "nuevo.usuario", "password", "una-clave-larga-y-rara"), null)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value(
                        "El registro de cuentas nuevas está deshabilitado por el administrador."));
        assertFalse(users.existsByUsernameNormalized("nuevo.usuario"));

        mvc.perform(get("/api/auth/config")).andExpect(status().isOk())
                .andExpect(jsonPath("$.registrationOpen").value(false));
    }

    @Test
    void existingAccountsCanStillLogIn() throws Exception {
        new ApiClient(mvc, mapper).login("test-admin", "test-password");
    }
}
