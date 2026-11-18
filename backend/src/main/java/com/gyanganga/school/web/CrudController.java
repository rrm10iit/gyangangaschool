package com.gyanganga.school.web;

import com.gyanganga.school.common.AuditService;
import com.gyanganga.school.common.PageDto;
import com.gyanganga.school.model.BaseEntity;
import jakarta.validation.Valid;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

/**
 * Generic list/get/create/update/delete endpoints with audit logging.
 * Subclasses add the mapping prefix, search filters and business validation.
 */
public abstract class CrudController<T extends BaseEntity> {
    protected final JpaRepository<T, Long> repo;
    protected final JpaSpecificationExecutor<T> specs;
    protected final AuditService audit;
    private final String label;

    protected <R extends JpaRepository<T, Long> & JpaSpecificationExecutor<T>> CrudController(
            R repo, AuditService audit, String label) {
        this.repo = repo;
        this.specs = repo;
        this.audit = audit;
        this.label = label;
    }

    /** Build a filter from the search text and request params. Return null for no filtering. */
    protected Specification<T> filter(String q, Map<String, String> params) {
        return null;
    }

    protected Sort defaultSort() {
        return Sort.by(Sort.Direction.DESC, "id");
    }

    /** Validation / normalisation hook. {@code id} is null on create. */
    protected void beforeSave(T entity, Long id) {}

    protected void beforeDelete(T entity) {}

    protected String describe(T entity) {
        return label + " #" + entity.getId();
    }

    /** Extra properties that clients may not overwrite on update. */
    protected String[] readOnlyOnUpdate() {
        return new String[0];
    }

    @GetMapping
    public PageDto<T> list(
            @RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size,
            @RequestParam Map<String, String> params) {
        int s = Math.min(Math.max(size, 1), 500);
        var pageable = PageRequest.of(Math.max(page, 0), s, defaultSort());
        return PageDto.of(specs.findAll(filter(q.trim(), params), pageable));
    }

    @GetMapping("/{id}")
    public T get(@PathVariable Long id) {
        return find(id);
    }

    @PostMapping
    @Transactional
    public T create(@Valid @RequestBody T body) {
        body.setId(null);
        beforeSave(body, null);
        T saved = repo.save(body);
        audit.log("CREATE", label, saved.getId(), describe(saved));
        return saved;
    }

    @PutMapping("/{id}")
    @Transactional
    public T update(@PathVariable Long id, @Valid @RequestBody T body) {
        T existing = find(id);
        List<String> ignore = new ArrayList<>(List.of("id", "createdAt", "updatedAt"));
        ignore.addAll(List.of(readOnlyOnUpdate()));
        BeanUtils.copyProperties(body, existing, ignore.toArray(new String[0]));
        beforeSave(existing, id);
        T saved = repo.save(existing);
        audit.log("UPDATE", label, id, describe(saved));
        return saved;
    }

    @DeleteMapping("/{id}")
    @Transactional
    public void delete(@PathVariable Long id) {
        T existing = find(id);
        beforeDelete(existing);
        String desc = describe(existing);
        repo.delete(existing);
        repo.flush();
        audit.log("DELETE", label, id, desc);
    }

    protected T find(Long id) {
        return repo.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, label + " not found"));
    }

    protected static ResponseStatusException bad(String msg) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, msg);
    }

    protected static ResponseStatusException conflict(String msg) {
        return new ResponseStatusException(HttpStatus.CONFLICT, msg);
    }

    // ---- specification helpers ----

    protected static <E> Specification<E> likeAny(String q, String... fields) {
        if (q == null || q.isBlank()) return null;
        String pattern = "%" + q.toLowerCase().replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
        return (root, query, cb) -> {
            var preds = new jakarta.persistence.criteria.Predicate[fields.length];
            for (int i = 0; i < fields.length; i++) {
                preds[i] = cb.like(cb.lower(root.get(fields[i]).as(String.class)), pattern, '\\');
            }
            return cb.or(preds);
        };
    }

    protected static <E> Specification<E> eq(String field, Object value) {
        if (value == null) return null;
        return (root, query, cb) -> cb.equal(root.get(field), value);
    }

    protected static Long longParam(Map<String, String> params, String key) {
        String v = params.get(key);
        if (v == null || v.isBlank()) return null;
        try {
            return Long.valueOf(v);
        } catch (NumberFormatException e) {
            throw bad("Invalid " + key);
        }
    }

    protected static java.time.LocalDate dateParam(Map<String, String> params, String key) {
        String v = params.get(key);
        if (v == null || v.isBlank()) return null;
        try {
            return java.time.LocalDate.parse(v);
        } catch (Exception e) {
            throw bad("Invalid " + key);
        }
    }
}
