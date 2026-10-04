package com.api.agenda_odontologica.api.controller;

import com.api.agenda_odontologica.api.dto.LoginRequest;
import com.api.agenda_odontologica.api.service.ApiException;
import com.api.agenda_odontologica.api.security.LoginAttemptLimiter;
import com.api.agenda_odontologica.api.service.LoginRateLimitException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthApiController {
    private final AuthenticationManager authenticationManager;
    private final LoginAttemptLimiter loginAttemptLimiter;
    private final HttpSessionSecurityContextRepository contextRepository =
            new HttpSessionSecurityContextRepository();

    public AuthApiController(
            AuthenticationManager authenticationManager,
            LoginAttemptLimiter loginAttemptLimiter) {
        this.authenticationManager = authenticationManager;
        this.loginAttemptLimiter = loginAttemptLimiter;
    }

    @GetMapping("/csrf")
    public Map<String, String> csrf(CsrfToken token) {
        return Map.of("token", token.getToken(), "headerName", token.getHeaderName());
    }

    @GetMapping("/session")
    public Map<String, Object> session(Authentication authentication) {
        boolean authenticated = authentication != null
                && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getPrincipal());
        return Map.of("authenticated", authenticated,
                "username", authenticated ? authentication.getName() : "");
    }

    @PostMapping("/login")
    public Map<String, Object> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest servletRequest,
            HttpServletResponse servletResponse) {
        String clientIp = servletRequest.getRemoteAddr();
        long retryAfterSeconds = loginAttemptLimiter.retryAfterSeconds(clientIp, request.username());
        if (retryAfterSeconds > 0) {
            throw new LoginRateLimitException(retryAfterSeconds);
        }

        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(
                            request.username(), request.password()));
        } catch (AuthenticationException exception) {
            retryAfterSeconds = loginAttemptLimiter.recordFailure(clientIp, request.username());
            if (retryAfterSeconds > 0) {
                throw new LoginRateLimitException(retryAfterSeconds);
            }
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Usuario o contraseña incorrectos.");
        }

        loginAttemptLimiter.recordSuccess(clientIp, request.username());
        if (servletRequest.getSession(false) != null) {
            servletRequest.changeSessionId();
        }
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        contextRepository.saveContext(context, servletRequest, servletResponse);
        return Map.of("authenticated", true, "username", authentication.getName());
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(
            Authentication authentication,
            HttpServletRequest request,
            HttpServletResponse response) {
        new SecurityContextLogoutHandler().logout(request, response, authentication);
        SecurityContextHolder.clearContext();
    }
}
