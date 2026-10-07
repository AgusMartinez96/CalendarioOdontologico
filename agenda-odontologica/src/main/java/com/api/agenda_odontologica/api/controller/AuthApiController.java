package com.api.agenda_odontologica.api.controller;

import com.api.agenda_odontologica.api.dto.LoginRequest;
import com.api.agenda_odontologica.api.dto.RegisterRequest;
import com.api.agenda_odontologica.api.security.AppUserDetails;
import com.api.agenda_odontologica.api.security.ClientIpResolver;
import com.api.agenda_odontologica.api.security.LoginAttemptLimiter;
import com.api.agenda_odontologica.api.service.AccountService;
import com.api.agenda_odontologica.api.service.ApiException;
import com.api.agenda_odontologica.api.service.LoginRateLimitException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
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
    private final AccountService accounts;
    private final ClientIpResolver clientIpResolver;
    private final HttpSessionSecurityContextRepository contextRepository =
            new HttpSessionSecurityContextRepository();

    public AuthApiController(
            AuthenticationManager authenticationManager,
            LoginAttemptLimiter loginAttemptLimiter,
            AccountService accounts,
            ClientIpResolver clientIpResolver) {
        this.authenticationManager = authenticationManager;
        this.loginAttemptLimiter = loginAttemptLimiter;
        this.accounts = accounts;
        this.clientIpResolver = clientIpResolver;
    }

    @GetMapping("/csrf")
    public Map<String, String> csrf(CsrfToken token) {
        return Map.of("token", token.getToken(), "headerName", token.getHeaderName());
    }

    @GetMapping("/config")
    public Map<String, Object> config() {
        return Map.of("registrationOpen", accounts.registrationOpen());
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
        String clientIp = clientIpResolver.resolve(servletRequest);
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
        accounts.recordLogin(((AppUserDetails) authentication.getPrincipal()).getId());
        establishSession(authentication, servletRequest, servletResponse);
        return Map.of("authenticated", true, "username", authentication.getName());
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletRequest servletRequest,
            HttpServletResponse servletResponse) {
        AppUserDetails user = accounts.register(
                request.username(), request.password(), clientIpResolver.resolve(servletRequest));
        Authentication authentication = UsernamePasswordAuthenticationToken.authenticated(
                user, null, user.getAuthorities());
        establishSession(authentication, servletRequest, servletResponse);
        return Map.of("authenticated", true, "username", user.getUsername());
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

    /** Mismo mecanismo para login y registro: nuevo id de sesión y contexto guardado. */
    private void establishSession(
            Authentication authentication, HttpServletRequest request, HttpServletResponse response) {
        if (request.getSession(false) != null) {
            request.changeSessionId();
        }
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        contextRepository.saveContext(context, request, response);
    }
}
