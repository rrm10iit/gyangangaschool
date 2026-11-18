package com.gyanganga.school.web;

import com.gyanganga.school.common.AuditService;
import com.gyanganga.school.common.PageDto;
import com.gyanganga.school.common.RateLimiter;
import com.gyanganga.school.config.AppProps;
import com.gyanganga.school.model.AppUser;
import com.gyanganga.school.model.Enums;
import com.gyanganga.school.repo.Repos;
import com.gyanganga.school.security.JwtAuthFilter;
import com.gyanganga.school.security.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

public final class AuthControllers {
    private AuthControllers() {}

    public record UserView(Long id, String email, String fullName, Enums.Role role, boolean enabled, Instant createdAt) {
        static UserView of(AppUser u) {
            return new UserView(u.getId(), u.getEmail(), u.getFullName(), u.getRole(), u.isEnabled(), u.getCreatedAt());
        }
    }

    public record LoginRequest(@NotBlank @Size(max = 190) String email, @NotBlank @Size(max = 200) String password) {}

    public record ChangePasswordRequest(@NotBlank String currentPassword,
                                        @NotBlank @Size(min = 10, max = 100) String newPassword) {}

    public record UserRequest(@NotBlank @Email @Size(max = 190) String email,
                              @NotBlank @Size(max = 150) String fullName,
                              @NotNull Enums.Role role,
                              boolean enabled,
                              @Size(min = 10, max = 100) String password) {}

    @RestController
    @RequestMapping("/api/auth")
    public static class AuthController {
        private static final int MAX_FAILS = 5;
        private static final Duration WINDOW = Duration.ofMinutes(15);

        private final Repos.UserRepo users;
        private final PasswordEncoder encoder;
        private final JwtService jwt;
        private final AppProps props;
        private final RateLimiter limiter;
        private final AuditService audit;
        // Used to keep the response time similar when the account does not exist.
        private final String dummyHash;

        public AuthController(Repos.UserRepo users, PasswordEncoder encoder, JwtService jwt, AppProps props,
                              RateLimiter limiter, AuditService audit) {
            this.users = users;
            this.encoder = encoder;
            this.jwt = jwt;
            this.props = props;
            this.limiter = limiter;
            this.audit = audit;
            this.dummyHash = encoder.encode("not-a-real-password");
        }

        @PostMapping("/login")
        public UserView login(@Valid @RequestBody LoginRequest req, HttpServletRequest http,
                              HttpServletResponse res) {
            String email = req.email().trim().toLowerCase();
            String ipKey = "login-ip:" + http.getRemoteAddr();
            String userKey = "login-user:" + email;
            if (limiter.isBlocked(ipKey, MAX_FAILS * 4, WINDOW) || limiter.isBlocked(userKey, MAX_FAILS, WINDOW)) {
                throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                        "Too many failed attempts. Please try again in 15 minutes.");
            }
            AppUser user = users.findByEmailIgnoreCase(email).orElse(null);
            boolean ok = encoder.matches(req.password(), user == null ? dummyHash : user.getPasswordHash())
                    && user != null && user.isEnabled();
            if (!ok) {
                limiter.record(ipKey);
                limiter.record(userKey);
                if (user != null) audit.logAs(email, "LOGIN_FAILED", "Auth", user.getId(), "Failed login attempt");
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Incorrect email or password");
            }
            limiter.reset(userKey);
            res.addHeader(HttpHeaders.SET_COOKIE, cookie(jwt.generate(user.getEmail(), user.getRole().name()),
                    Duration.ofHours(props.jwtExpiryHours())).toString());
            audit.logAs(user.getEmail(), "LOGIN", "Auth", user.getId(), "Signed in");
            return UserView.of(user);
        }

        @PostMapping("/logout")
        public Map<String, String> logout(HttpServletResponse res) {
            res.addHeader(HttpHeaders.SET_COOKIE, cookie("", Duration.ZERO).toString());
            return Map.of("status", "ok");
        }

        @GetMapping("/me")
        public UserView me(Authentication auth) {
            return UserView.of(users.findByEmailIgnoreCase(auth.getName())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required")));
        }

        @PostMapping("/change-password")
        @Transactional
        public Map<String, String> changePassword(@Valid @RequestBody ChangePasswordRequest req, Authentication auth) {
            AppUser u = users.findByEmailIgnoreCase(auth.getName())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required"));
            if (!encoder.matches(req.currentPassword(), u.getPasswordHash())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Current password is incorrect");
            }
            u.setPasswordHash(encoder.encode(req.newPassword()));
            users.save(u);
            audit.log("UPDATE", "Auth", u.getId(), "Changed own password");
            return Map.of("status", "ok");
        }

        private ResponseCookie cookie(String value, Duration maxAge) {
            return ResponseCookie.from(JwtAuthFilter.COOKIE, value)
                    .httpOnly(true)
                    .secure(props.cookieSecure())
                    .sameSite("Strict")
                    .path("/")
                    .maxAge(maxAge)
                    .build();
        }
    }

    @RestController
    @RequestMapping("/api/admin/users")
    public static class UserController {
        private final Repos.UserRepo users;
        private final PasswordEncoder encoder;
        private final AuditService audit;

        public UserController(Repos.UserRepo users, PasswordEncoder encoder, AuditService audit) {
            this.users = users;
            this.encoder = encoder;
            this.audit = audit;
        }

        @GetMapping
        public PageDto<UserView> list(@RequestParam(defaultValue = "") String q,
                                      @RequestParam(defaultValue = "0") int page,
                                      @RequestParam(defaultValue = "50") int size) {
            var spec = CrudController.<AppUser>likeAny(q.trim(), "email", "fullName");
            var p = users.findAll(spec, PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 200),
                    Sort.by("fullName")));
            return new PageDto<>(p.getContent().stream().map(UserView::of).toList(), p.getTotalElements(),
                    p.getNumber(), p.getSize());
        }

        @PostMapping
        @Transactional
        public UserView create(@Valid @RequestBody UserRequest req) {
            if (req.password() == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password is required");
            if (users.existsByEmailIgnoreCase(req.email())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "A user with this email already exists");
            }
            AppUser u = new AppUser();
            u.setEmail(req.email().trim().toLowerCase());
            u.setFullName(req.fullName().trim());
            u.setRole(req.role());
            u.setEnabled(req.enabled());
            u.setPasswordHash(encoder.encode(req.password()));
            users.save(u);
            audit.log("CREATE", "User", u.getId(), "Created " + u.getRole() + " account " + u.getEmail());
            return UserView.of(u);
        }

        @PutMapping("/{id}")
        @Transactional
        public UserView update(@PathVariable Long id, @Valid @RequestBody UserRequest req, Authentication auth) {
            AppUser u = users.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
            boolean self = u.getEmail().equalsIgnoreCase(auth.getName());
            String email = req.email().trim().toLowerCase();
            if (!u.getEmail().equalsIgnoreCase(email) && users.existsByEmailIgnoreCase(email)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "A user with this email already exists");
            }
            boolean losesAdmin = u.getRole() == Enums.Role.ADMIN && u.isEnabled()
                    && (req.role() != Enums.Role.ADMIN || !req.enabled());
            if (losesAdmin && (self || users.countByRoleAndEnabledTrue(Enums.Role.ADMIN) <= 1)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "You cannot demote or disable your own account or the last administrator");
            }
            u.setEmail(email);
            u.setFullName(req.fullName().trim());
            u.setRole(req.role());
            u.setEnabled(req.enabled());
            if (req.password() != null && !req.password().isBlank()) u.setPasswordHash(encoder.encode(req.password()));
            users.save(u);
            audit.log("UPDATE", "User", id, "Updated account " + u.getEmail() + " (" + u.getRole() + ")");
            return UserView.of(u);
        }

        @DeleteMapping("/{id}")
        @Transactional
        public void delete(@PathVariable Long id, Authentication auth) {
            AppUser u = users.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
            if (u.getEmail().equalsIgnoreCase(auth.getName())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You cannot delete your own account");
            }
            if (u.getRole() == Enums.Role.ADMIN && u.isEnabled() && users.countByRoleAndEnabledTrue(Enums.Role.ADMIN) <= 1) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You cannot delete the last administrator");
            }
            users.delete(u);
            audit.log("DELETE", "User", id, "Deleted account " + u.getEmail());
        }
    }
}
