package com.api.agenda_odontologica.api.controller;

import com.api.agenda_odontologica.support.ApiClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, properties = {
        "spring.datasource.url=jdbc:h2:mem:regcapacity;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false",
        "app.admin.username=test-admin",
        "app.admin.password=test-password",
        "app.registration.max-users=3",
        "app.registration.max-per-ip-per-hour=100"
})
@AutoConfigureMockMvc
class RegistrationCapacityApiTest {
    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper mapper;

    @Test
    void closesRegistrationWhenMaxUsersIsReached() throws Exception {
        ApiClient api = new ApiClient(mvc, mapper);
        String password = "una-clave-larga-y-rara";

        mvc.perform(get("/api/auth/config")).andExpect(jsonPath("$.registrationOpen").value(true));
        api.send(HttpMethod.POST, "/api/auth/register", Map.of("username", "primero", "password", password), null)
                .andExpect(status().isCreated());
        api.send(HttpMethod.POST, "/api/auth/register", Map.of("username", "segundo", "password", password), null)
                .andExpect(status().isCreated());

        api.send(HttpMethod.POST, "/api/auth/register", Map.of("username", "tercero", "password", password), null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Registro cerrado por cupo"));
        mvc.perform(get("/api/auth/config")).andExpect(jsonPath("$.registrationOpen").value(false));

        api.login("primero", password);
        api.login("test-admin", "test-password");
    }
}
