package com.example.cron_scheduler;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.security.Principal;
import java.util.List;
import java.util.Map;

/**
 * Admin endpoints for inspecting scheduler state. Restricted to ADMIN role.
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final JobService jobService;

    public AdminController(JobService jobService) {
        this.jobService = jobService;
    }

    @GetMapping("/scheduler-status")
    public Map<String, Object> schedulerStatus(Principal principal) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin role required");
        }
        boolean isAdmin = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(r -> r.equals("ROLE_ADMIN"));
        if (!isAdmin) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin role required");
        }

        List<Long> scheduled = jobService.getScheduledJobIds();
        List<Long> rehydrated = jobService.getLastRehydratedIds();
        var at = jobService.getLastRehydratedAt();

        return Map.of(
                "scheduledJobIds", scheduled,
                "scheduledCount", scheduled.size(),
                "lastRehydratedIds", rehydrated,
                "lastRehydratedAt", at == null ? null : at.toString()
        );
    }
}
