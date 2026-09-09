package com.example.cron_scheduler;

import java.security.Principal;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Exposes the authenticated user's identity and role so the dashboard can hide
 * write actions the backend would reject for the current account.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @GetMapping("/me")
    public AuthResponse me(Principal principal) {
        List<String> roles = SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .toList();
        String role = roles.contains("ROLE_ADMIN") ? "ADMIN" : "OPERATOR";
        return new AuthResponse(principal.getName(), role);
    }

    public record AuthResponse(String username, String role) {
    }
}
