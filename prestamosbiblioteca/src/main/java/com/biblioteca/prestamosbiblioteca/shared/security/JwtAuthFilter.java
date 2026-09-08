package com.biblioteca.prestamosbiblioteca.shared.security;

import com.biblioteca.prestamosbiblioteca.shared.config.AppSecurityProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** Lee el JWT desde la cookie httpOnly y establece la autenticación en el contexto. */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final AppUserDetailsService userDetailsService;
    private final String cookieName;

    public JwtAuthFilter(JwtService jwtService,
                         AppUserDetailsService userDetailsService,
                         AppSecurityProperties properties) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
        this.cookieName = properties.cookie().name();
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        String token = readTokenCookie(request);
        if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            String email = jwtService.extractEmailIfValid(token);
            if (email != null) {
                UserDetails userDetails = userDetailsService.loadUserByUsername(email);
                if (userDetails.isEnabled()) {
                    var authentication = new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities());
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            }
        }
        filterChain.doFilter(request, response);
    }

    private String readTokenCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (cookieName.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }
}
