package com.api.agenda_odontologica.api.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Resuelve la IP del cliente para los limitadores. Por defecto usa solo la dirección del
 * socket (no confía en cabeceras). Si la app corre detrás de N proxies de confianza
 * (TRUSTED_PROXY_HOPS), toma la entrada N desde la derecha de X-Forwarded-For: esas
 * entradas las agregaron los proxies propios, no el cliente, así que no son falsificables.
 */
@Component
public class ClientIpResolver {
    private final int trustedProxyHops;

    public ClientIpResolver(@Value("${app.security.trusted-proxy-hops:0}") int trustedProxyHops) {
        if (trustedProxyHops < 0) {
            throw new IllegalArgumentException("TRUSTED_PROXY_HOPS no puede ser negativo.");
        }
        this.trustedProxyHops = trustedProxyHops;
    }

    public String resolve(HttpServletRequest request) {
        if (trustedProxyHops > 0) {
            String header = request.getHeader("X-Forwarded-For");
            if (header != null && !header.isBlank()) {
                String[] parts = header.split(",");
                int index = parts.length - trustedProxyHops;
                if (index >= 0) {
                    String candidate = parts[index].trim();
                    if (!candidate.isEmpty() && candidate.length() <= 45) {
                        return candidate;
                    }
                }
            }
        }
        return request.getRemoteAddr();
    }
}
