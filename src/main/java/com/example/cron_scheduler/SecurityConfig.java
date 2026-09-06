package com.example.cron_scheduler;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.util.StringUtils;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    // No WWW-Authenticate header is added, so the browser never shows its own login popup.
    private static final AuthenticationEntryPoint NO_CHALLENGE_ENTRY_POINT = (request, response, authException) -> {
        response.setStatus(401);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("{\"status\":401,\"message\":\"Invalid username or password\"}");
    };

    @Value("${app.security.admin.username}")
    private String adminUsername;

    @Value("${app.security.admin.password-hash}")
    private String adminPasswordHash;

    @Value("${app.security.operator.username}")
    private String operatorUsername;

    @Value("${app.security.operator.password-hash}")
    private String operatorPasswordHash;

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.ignoringRequestMatchers("/h2-console/**", "/api/**", "/graphql/**"))
            .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/", "/index.html", "/css/**", "/js/**", "/webjars/**",
                    "/actuator/health", "/h2-console/**").permitAll()
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/**")
                    .hasAnyRole("ADMIN", "OPERATOR")
                .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/jobs/*/run")
                    .hasAnyRole("ADMIN", "OPERATOR")
                .requestMatchers("/api/**", "/graphql/**").hasRole("ADMIN")
                .anyRequest().authenticated())
            // httpBasic() keeps its own private entry point for bad-credential failures,
            // separate from exceptionHandling()'s fallback for missing credentials, so both
            // must point at this same entry point or the browser's native popup returns.
            .exceptionHandling(exceptions -> exceptions.authenticationEntryPoint(NO_CHALLENGE_ENTRY_POINT))
            .httpBasic(basic -> basic.authenticationEntryPoint(NO_CHALLENGE_ENTRY_POINT));
        return http.build();
    }

    @Bean
    UserDetailsService userDetailsService(PasswordEncoder passwordEncoder) {
        requireSecurityConfiguration();
        UserDetails admin = User.withUsername(adminUsername)
            .password(adminPasswordHash)
                .roles("ADMIN")
                .build();

        UserDetails operator = User.withUsername(operatorUsername)
            .password(operatorPasswordHash)
                .roles("OPERATOR")
                .build();

        return new InMemoryUserDetailsManager(admin, operator);
    }

    private void requireSecurityConfiguration() {
        if (!StringUtils.hasText(adminUsername) || !StringUtils.hasText(adminPasswordHash)
                || !StringUtils.hasText(operatorUsername) || !StringUtils.hasText(operatorPasswordHash)) {
            throw new IllegalStateException("Missing security environment variables. Configure "
                    + "ADMIN_USERNAME, ADMIN_PASSWORD_HASH, OPERATOR_USERNAME, and OPERATOR_PASSWORD_HASH.");
        }
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
