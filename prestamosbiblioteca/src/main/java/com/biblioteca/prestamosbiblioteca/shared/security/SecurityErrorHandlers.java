package com.biblioteca.prestamosbiblioteca.shared.security;

import com.biblioteca.prestamosbiblioteca.shared.web.ApiError;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import org.springframework.http.MediaType;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

/**
 * Traduce las fallas de autenticación/autorización de Spring Security al formato {@link ApiError}.
 * El JSON se arma a mano (forma fija y controlada) para no depender del ObjectMapper del contenedor.
 */
@Component
public class SecurityErrorHandlers {

    public AuthenticationEntryPoint authenticationEntryPoint() {
        return (request, response, ex) -> write(response, HttpServletResponse.SC_UNAUTHORIZED,
                "UNAUTHENTICATED", "Se requiere autenticación", request.getRequestURI());
    }

    public AccessDeniedHandler accessDeniedHandler() {
        return (request, response, ex) -> write(response, HttpServletResponse.SC_FORBIDDEN,
                "ACCESS_DENIED", "No tenés permisos para esta operación", request.getRequestURI());
    }

    private void write(HttpServletResponse response, int status, String code, String message, String path)
            throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        String json = "{"
                + "\"timestamp\":\"" + Instant.now() + "\","
                + "\"status\":" + status + ","
                + "\"code\":\"" + code + "\","
                + "\"message\":\"" + escape(message) + "\","
                + "\"path\":\"" + escape(path) + "\"}";
        response.getWriter().write(json);
    }

    private String escape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
