package com.gyanganga.school.security;

import com.gyanganga.school.repo.Repos;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {
    public static final String COOKIE = "ggs_token";

    private final JwtService jwt;
    private final Repos.UserRepo users;

    public JwtAuthFilter(JwtService jwt, Repos.UserRepo users) {
        this.jwt = jwt;
        this.users = users;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        String token = extract(req);
        if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            jwt.parse(token).flatMap(c -> users.findByEmailIgnoreCase(c.getSubject())).filter(u -> u.isEnabled()).ifPresent(u -> {
                var auth = new UsernamePasswordAuthenticationToken(
                        u.getEmail(), null, List.of(new SimpleGrantedAuthority("ROLE_" + u.getRole().name())));
                SecurityContextHolder.getContext().setAuthentication(auth);
            });
        }
        chain.doFilter(req, res);
    }

    private String extract(HttpServletRequest req) {
        if (req.getCookies() != null) {
            for (Cookie c : req.getCookies()) {
                if (COOKIE.equals(c.getName())) return c.getValue();
            }
        }
        String h = req.getHeader("Authorization");
        if (h != null && h.startsWith("Bearer ")) return h.substring(7);
        return null;
    }
}
