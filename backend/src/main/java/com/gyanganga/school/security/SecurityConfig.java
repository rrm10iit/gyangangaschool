package com.gyanganga.school.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    /** The JWT filter must only run inside the Spring Security chain. */
    @Bean
    org.springframework.boot.web.servlet.FilterRegistrationBean<JwtAuthFilter> jwtFilterRegistration(
            JwtAuthFilter filter) {
        var reg = new org.springframework.boot.web.servlet.FilterRegistrationBean<>(filter);
        reg.setEnabled(false);
        return reg;
    }

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http, JwtAuthFilter jwtFilter, ObjectMapper mapper) throws Exception {
        http.csrf(c -> c.disable()) // cookie is SameSite=Strict and the API only accepts JSON/multipart
                .cors(Customizer.withDefaults())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .headers(h -> h.frameOptions(f -> f.deny())
                        .contentTypeOptions(Customizer.withDefaults())
                        .referrerPolicy(r -> r.policy(
                                org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy
                                        .STRICT_ORIGIN_WHEN_CROSS_ORIGIN)))
                .authorizeHttpRequests(a -> a
                        .requestMatchers("/error", "/actuator/health/**", "/api/public/**", "/uploads/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/login", "/api/auth/logout").permitAll()
                        .requestMatchers("/api/auth/**").authenticated()
                        // finance: admin + accountant only
                        .requestMatchers("/api/admin/finance/**").hasAnyRole("ADMIN", "ACCOUNTANT")
                        // students and teachers: everyone can read, admin writes
                        .requestMatchers(HttpMethod.GET, "/api/admin/students/**", "/api/admin/teachers/**",
                                "/api/admin/classes/**", "/api/admin/dashboard/**")
                        .hasAnyRole("ADMIN", "TEACHER", "ACCOUNTANT")
                        .requestMatchers("/api/admin/files/**").hasAnyRole("ADMIN", "TEACHER", "ACCOUNTANT")
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .anyRequest().denyAll())
                .exceptionHandling(e -> e
                        .authenticationEntryPoint((req, res, ex) -> write(res, mapper, 401, "Authentication required"))
                        .accessDeniedHandler((req, res, ex) -> write(res, mapper, 403, "You do not have permission")))
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    private static void write(jakarta.servlet.http.HttpServletResponse res, ObjectMapper mapper, int status, String msg)
            throws java.io.IOException {
        res.setStatus(status);
        res.setContentType(MediaType.APPLICATION_JSON_VALUE);
        mapper.writeValue(res.getOutputStream(), Map.of("message", msg));
    }
}
