package com.gyanganga.school.common;

import com.gyanganga.school.model.AuditLog;
import com.gyanganga.school.repo.Repos;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class AuditService {
    private final Repos.AuditRepo repo;

    public AuditService(Repos.AuditRepo repo) {
        this.repo = repo;
    }

    public static String currentUser() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        return a == null || !a.isAuthenticated() ? "system" : String.valueOf(a.getPrincipal());
    }

    public void log(String action, String entityType, Object entityId, String summary) {
        logAs(currentUser(), action, entityType, entityId, summary);
    }

    public void logAs(String user, String action, String entityType, Object entityId, String summary) {
        AuditLog l = new AuditLog();
        l.setUserEmail(user);
        l.setAction(action);
        l.setEntityType(entityType);
        l.setEntityId(entityId == null ? null : String.valueOf(entityId));
        l.setSummary(summary != null && summary.length() > 480 ? summary.substring(0, 480) : summary);
        repo.save(l);
    }
}
