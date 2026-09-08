package com.biblioteca.prestamosbiblioteca.user.web;

import com.biblioteca.prestamosbiblioteca.shared.security.AppUserPrincipal;
import com.biblioteca.prestamosbiblioteca.shared.security.AuthCookieService;
import com.biblioteca.prestamosbiblioteca.shared.security.JwtService;
import com.biblioteca.prestamosbiblioteca.user.domain.AppUser;
import com.biblioteca.prestamosbiblioteca.user.domain.AppUserService;
import com.biblioteca.prestamosbiblioteca.user.web.dto.ActivateRequest;
import com.biblioteca.prestamosbiblioteca.user.web.dto.AuthUserResponse;
import com.biblioteca.prestamosbiblioteca.user.web.dto.LoginRequest;
import com.biblioteca.prestamosbiblioteca.user.web.dto.RegisterRequest;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AppUserService userService;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final AuthCookieService cookieService;

    public AuthController(AppUserService userService,
                          AuthenticationManager authenticationManager,
                          JwtService jwtService,
                          AuthCookieService cookieService) {
        this.userService = userService;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.cookieService = cookieService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthUserResponse> register(@Valid @RequestBody RegisterRequest request) {
        AppUser user = userService.register(request.name(), request.email(), request.password());
        return authenticatedResponse(user);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthUserResponse> login(@Valid @RequestBody LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password()));
        AppUser user = ((AppUserPrincipal) authentication.getPrincipal()).getUser();
        return authenticatedResponse(user);
    }

    @PostMapping("/activate")
    public ResponseEntity<AuthUserResponse> activate(@Valid @RequestBody ActivateRequest request) {
        AppUser user = userService.activate(request.token(), request.password());
        return authenticatedResponse(user);
    }

    @GetMapping("/activation/{token}")
    public Map<String, Boolean> checkActivation(@PathVariable String token) {
        return Map.of("valid", userService.isActivationTokenValid(token));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        ResponseCookie cookie = cookieService.clear();
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .build();
    }

    @GetMapping("/me")
    public AuthUserResponse me(@AuthenticationPrincipal AppUserPrincipal principal) {
        return AuthUserResponse.from(principal.getUser());
    }

    private ResponseEntity<AuthUserResponse> authenticatedResponse(AppUser user) {
        ResponseCookie cookie = cookieService.build(jwtService.generateToken(user));
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(AuthUserResponse.from(user));
    }
}
