package com.gyanganga.school.web;

import com.gyanganga.school.common.AuditService;
import com.gyanganga.school.model.*;
import com.gyanganga.school.repo.Repos;
import com.gyanganga.school.service.FileStorageService;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

/** Classes, students and teachers. */
public final class AcademicControllers {
    private AcademicControllers() {}

    @RestController
    @RequestMapping("/api/admin/classes")
    public static class ClassController extends CrudController<SchoolClass> {
        private final Repos.StudentRepo students;

        public ClassController(Repos.ClassRepo repo, AuditService audit, Repos.StudentRepo students) {
            super(repo, audit, "Class");
            this.students = students;
        }

        @Override
        protected Sort defaultSort() {
            return Sort.by("displayOrder", "id");
        }

        @Override
        protected Specification<SchoolClass> filter(String q, Map<String, String> p) {
            return likeAny(q, "name");
        }

        @Override
        protected String describe(SchoolClass c) {
            return "Class " + c.getName();
        }

        @Override
        protected void beforeDelete(SchoolClass c) {
            if (students.count((root, query, cb) -> cb.equal(root.get("classId"), c.getId())) > 0) {
                throw conflict("This class still has students. Move or remove them first.");
            }
        }
    }

    @RestController
    @RequestMapping("/api/admin/students")
    public static class StudentController extends CrudController<Student> {
        private final Repos.StudentRepo students;
        private final Repos.ClassRepo classes;
        private final Repos.StudentDocRepo docs;
        private final FileStorageService storage;

        public StudentController(Repos.StudentRepo repo, AuditService audit, Repos.ClassRepo classes,
                                 Repos.StudentDocRepo docs, FileStorageService storage) {
            super(repo, audit, "Student");
            this.students = repo;
            this.classes = classes;
            this.docs = docs;
            this.storage = storage;
        }

        @Override
        protected Sort defaultSort() {
            return Sort.by("classId", "section", "rollNo", "fullName");
        }

        @Override
        protected Specification<Student> filter(String q, Map<String, String> p) {
            Long classId = longParam(p, "classId");
            String section = p.get("section");
            String status = p.get("status");
            Specification<Student> st = null;
            if (status != null && !status.isBlank()) {
                try {
                    st = eq("status", Enums.StudentStatus.valueOf(status));
                } catch (IllegalArgumentException e) {
                    throw bad("Invalid status");
                }
            }
            return Specification.allOf(
                    likeAny(q, "fullName", "admissionNo", "guardianName", "guardianPhone"),
                    eq("classId", classId),
                    section == null || section.isBlank() ? null : eq("section", section),
                    st);
        }

        @Override
        protected void beforeSave(Student s, Long id) {
            if (!classes.existsById(s.getClassId())) throw bad("Selected class does not exist");
            boolean dup = id == null
                    ? students.existsByAdmissionNo(s.getAdmissionNo())
                    : students.existsByAdmissionNoAndIdNot(s.getAdmissionNo(), id);
            if (dup) throw conflict("Admission number " + s.getAdmissionNo() + " is already in use");
            if (s.getFeeDiscount() == null) s.setFeeDiscount(java.math.BigDecimal.ZERO);
            if (s.getStatus() == null) s.setStatus(Enums.StudentStatus.ACTIVE);
        }

        @Override
        protected String describe(Student s) {
            return "Student " + s.getFullName() + " (" + s.getAdmissionNo() + ")";
        }

        @GetMapping("/{id}/documents")
        public List<StudentDocument> documents(@PathVariable Long id) {
            find(id);
            return docs.findByStudentIdOrderByIdDesc(id);
        }

        @PostMapping("/{id}/documents")
        public StudentDocument addDocument(@PathVariable Long id, @RequestParam String name,
                                           @RequestParam("file") MultipartFile file) {
            Student s = find(id);
            if (name.isBlank() || name.length() > 150) throw bad("Document name is required (max 150 characters)");
            StudentDocument d = new StudentDocument();
            d.setStudentId(id);
            d.setName(name.trim());
            d.setFileUrl(storage.storeDocument(file));
            docs.save(d);
            audit.log("CREATE", "Student document", d.getId(), "Added '" + d.getName() + "' for " + s.getFullName());
            return d;
        }

        @DeleteMapping("/{id}/documents/{docId}")
        public void deleteDocument(@PathVariable Long id, @PathVariable Long docId) {
            StudentDocument d = docs.findById(docId)
                    .filter(x -> x.getStudentId().equals(id))
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found"));
            docs.delete(d);
            audit.log("DELETE", "Student document", docId, "Removed '" + d.getName() + "' (student #" + id + ")");
        }
    }

    @RestController
    @RequestMapping("/api/admin/teachers")
    public static class TeacherController extends CrudController<Teacher> {
        private final Repos.TeacherRepo teachers;

        public TeacherController(Repos.TeacherRepo repo, AuditService audit) {
            super(repo, audit, "Teacher");
            this.teachers = repo;
        }

        @Override
        protected Sort defaultSort() {
            return Sort.by("fullName");
        }

        @Override
        protected Specification<Teacher> filter(String q, Map<String, String> p) {
            String active = p.get("active");
            return Specification.allOf(
                    likeAny(q, "fullName", "employeeNo", "subjects", "classesAssigned", "phone"),
                    active == null || active.isBlank() ? null : eq("active", Boolean.parseBoolean(active)));
        }

        @Override
        protected void beforeSave(Teacher t, Long id) {
            boolean dup = id == null
                    ? teachers.existsByEmployeeNo(t.getEmployeeNo())
                    : teachers.existsByEmployeeNoAndIdNot(t.getEmployeeNo(), id);
            if (dup) throw conflict("Employee number " + t.getEmployeeNo() + " is already in use");
            if (t.getMonthlySalary() == null) t.setMonthlySalary(java.math.BigDecimal.ZERO);
        }

        @Override
        protected String describe(Teacher t) {
            return "Teacher " + t.getFullName() + " (" + t.getEmployeeNo() + ")";
        }
    }
}
