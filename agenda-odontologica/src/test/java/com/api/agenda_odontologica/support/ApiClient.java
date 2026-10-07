package com.api.agenda_odontologica.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.Map;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Cliente de pruebas que arma cookie y cabecera CSRF en cada llamada. */
public class ApiClient {
    private final MockMvc mvc;
    private final ObjectMapper mapper;

    public ApiClient(MockMvc mvc, ObjectMapper mapper) {
        this.mvc = mvc;
        this.mapper = mapper;
    }

    public ResultActions send(HttpMethod method, String url, Object body, MockHttpSession session, String ip)
            throws Exception {
        MvcResult csrfResult = mvc.perform(MockMvcRequestBuilders.get("/api/auth/csrf")
                .with(request -> {
                    request.setRemoteAddr(ip);
                    return request;
                })).andExpect(status().isOk()).andReturn();
        JsonNode csrf = mapper.readTree(csrfResult.getResponse().getContentAsString());
        String token = csrf.get("token").asText();
        MockHttpServletRequestBuilder builder = MockMvcRequestBuilders.request(method, url)
                .with(request -> {
                    request.setRemoteAddr(ip);
                    return request;
                })
                .cookie(new Cookie("XSRF-TOKEN", token))
                .header(csrf.get("headerName").asText(), token);
        if (session != null) {
            builder.session(session);
        }
        if (body != null) {
            builder.contentType(MediaType.APPLICATION_JSON).content(
                    body instanceof String text ? text : mapper.writeValueAsString(body));
        }
        return mvc.perform(builder);
    }

    public ResultActions send(HttpMethod method, String url, Object body, MockHttpSession session)
            throws Exception {
        return send(method, url, body, session, "192.0.2.1");
    }

    public MockHttpSession login(String username, String password) throws Exception {
        MvcResult result = send(HttpMethod.POST, "/api/auth/login",
                Map.of("username", username, "password", password), null, "198.51.100.77")
                .andExpect(status().isOk()).andReturn();
        return (MockHttpSession) result.getRequest().getSession(false);
    }

    public JsonNode json(ResultActions actions) throws Exception {
        return mapper.readTree(actions.andReturn().getResponse().getContentAsString());
    }
}
