package com.api.agenda_odontologica.api.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, properties = {
        "spring.datasource.url=jdbc:h2:mem:security;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false",
        "app.admin.username=test-admin",
        "app.admin.password=test-password",
        "app.security.login.max-attempts=5"
})
@AutoConfigureMockMvc
class AuthenticationApiControllerTest {
    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void protectsApiAndAllowsCsrfLoginAndHealthCheck() throws Exception {
        mvc.perform(get("/api/v1/patients"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.path").value("/api/v1/patients"));

        mvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.groups").doesNotExist())
                .andExpect(jsonPath("$.components").doesNotExist());
        mvc.perform(get("/v3/api-docs")).andExpect(status().isUnauthorized());
        mvc.perform(get("/swagger-ui.html")).andExpect(status().isUnauthorized());

        MvcResult csrfResult = mvc.perform(get("/api/auth/csrf"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode csrf = objectMapper.readTree(csrfResult.getResponse().getContentAsString());
        String token = csrf.get("token").asText();
        String headerName = csrf.get("headerName").asText();

        MvcResult loginResult = mvc.perform(post("/api/auth/login")
                        .cookie(new Cookie("XSRF-TOKEN", token))
                        .header(headerName, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"test-admin","password":"test-password"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authenticated").value(true))
                .andReturn();

        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);
        mvc.perform(get("/api/auth/session").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authenticated").value(true))
                .andExpect(jsonPath("$.username").value("test-admin"));
        mvc.perform(get("/api/v1/patients").session(session))
                .andExpect(status().isOk());
        mvc.perform(get("/v3/api-docs").session(session)).andExpect(status().isOk());
        mvc.perform(get("/swagger-ui.html").session(session)).andExpect(status().is3xxRedirection());
    }

    @Test
    void locksAfterConfiguredFailedAttemptsWithRetryAfterAndUniformError() throws Exception {
        String ip = "192.0.2.10";
        for (int i = 0; i < 5; i++) {
            login("test-admin", "wrong-password", ip).andExpect(status().isUnauthorized());
        }

        login("test-admin", "wrong-password", ip)
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.status").value(429))
                .andExpect(jsonPath("$.path").value("/api/auth/login"))
                .andExpect(header().string("Retry-After", "900"));

        login("test-admin", "test-password", ip).andExpect(status().isTooManyRequests());
    }

    @Test
    void successfulLoginResetsFailureCounter() throws Exception {
        String ip = "192.0.2.11";
        for (int i = 0; i < 5; i++) {
            login("test-admin", "wrong-password", ip).andExpect(status().isUnauthorized());
        }

        login("test-admin", "test-password", ip).andExpect(status().isOk());

        for (int i = 0; i < 5; i++) {
            login("test-admin", "wrong-password", ip).andExpect(status().isUnauthorized());
        }
        login("test-admin", "wrong-password", ip).andExpect(status().isTooManyRequests());
    }

    @Test
    void isolatesFailuresByIpAndUsername() throws Exception {
        String ip = "192.0.2.12";
        for (int i = 0; i < 5; i++) {
            login("test-admin", "wrong-password", ip).andExpect(status().isUnauthorized());
        }

        login("another-user", "wrong-password", ip).andExpect(status().isUnauthorized());
        login("test-admin", "wrong-password", "192.0.2.13").andExpect(status().isUnauthorized());
        login("test-admin", "wrong-password", ip).andExpect(status().isTooManyRequests());
    }

    private ResultActions login(String username, String password, String remoteAddress) throws Exception {
        MvcResult csrfResult = mvc.perform(get("/api/auth/csrf")
                        .with(request -> {
                            request.setRemoteAddr(remoteAddress);
                            return request;
                        }))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode csrf = objectMapper.readTree(csrfResult.getResponse().getContentAsString());
        String token = csrf.get("token").asText();
        return mvc.perform(post("/api/auth/login")
                .with(request -> {
                    request.setRemoteAddr(remoteAddress);
                    return request;
                })
                .cookie(new Cookie("XSRF-TOKEN", token))
                .header(csrf.get("headerName").asText(), token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "username", username,
                        "password", password))));
    }
}
