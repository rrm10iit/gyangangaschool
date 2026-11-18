package com.gyanganga.school.repo;

import com.gyanganga.school.model.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** All Spring Data repositories. Nested interfaces are enabled via considerNestedRepositories. */
public final class Repos {
    private Repos() {}

    public interface UserRepo extends JpaRepository<AppUser, Long>, JpaSpecificationExecutor<AppUser> {
        Optional<AppUser> findByEmailIgnoreCase(String email);

        boolean existsByEmailIgnoreCase(String email);

        long countByRoleAndEnabledTrue(Enums.Role role);
    }

    public interface ClassRepo extends JpaRepository<SchoolClass, Long>, JpaSpecificationExecutor<SchoolClass> {
        List<SchoolClass> findAllByOrderByDisplayOrderAscIdAsc();
    }

    public interface StudentRepo extends JpaRepository<Student, Long>, JpaSpecificationExecutor<Student> {
        boolean existsByAdmissionNo(String admissionNo);

        boolean existsByAdmissionNoAndIdNot(String admissionNo, Long id);

        long countByStatus(Enums.StudentStatus status);

        List<Student> findByStatus(Enums.StudentStatus status);

        @Query("select s.classId, count(s) from Student s where s.status = 'ACTIVE' group by s.classId")
        List<Object[]> countActiveByClass();
    }

    public interface StudentDocRepo extends JpaRepository<StudentDocument, Long> {
        List<StudentDocument> findByStudentIdOrderByIdDesc(Long studentId);
    }

    public interface TeacherRepo extends JpaRepository<Teacher, Long>, JpaSpecificationExecutor<Teacher> {
        boolean existsByEmployeeNo(String employeeNo);

        boolean existsByEmployeeNoAndIdNot(String employeeNo, Long id);

        long countByActiveTrue();

        List<Teacher> findByActiveTrue();
    }

    public interface FeeStructureRepo extends JpaRepository<FeeStructure, Long>, JpaSpecificationExecutor<FeeStructure> {
        List<FeeStructure> findByClassId(Long classId);
    }

    public interface PaymentRepo extends JpaRepository<FeePayment, Long>, JpaSpecificationExecutor<FeePayment> {
        List<FeePayment> findByPaymentDateBetweenOrderByPaymentDateAscIdAsc(LocalDate from, LocalDate to);

        List<FeePayment> findByStudentIdOrderByPaymentDateDescIdDesc(Long studentId);

        @Query("select p.studentId, sum(p.amount) from FeePayment p where p.academicYear = :year group by p.studentId")
        List<Object[]> sumByStudentForYear(@Param("year") int year);

        @Query("select coalesce(sum(p.amount),0) from FeePayment p where p.academicYear = :year")
        BigDecimal totalForYear(@Param("year") int year);

        @Query("select coalesce(sum(p.amount),0) from FeePayment p where p.studentId = :sid and p.academicYear = :year")
        BigDecimal totalForStudent(@Param("sid") Long studentId, @Param("year") int year);

        @Query(value = "select nextval('receipt_seq')", nativeQuery = true)
        long nextReceiptNumber();
    }

    public interface SalaryRepo extends JpaRepository<SalaryRecord, Long>, JpaSpecificationExecutor<SalaryRecord> {
        boolean existsByTeacherIdAndMonth(Long teacherId, String month);

        boolean existsByTeacherIdAndMonthAndIdNot(Long teacherId, String month, Long id);

        List<SalaryRecord> findByMonthBetweenOrderByMonthAscIdAsc(String from, String to);

        List<SalaryRecord> findByStatusAndPaidDateBetweenOrderByPaidDateAscIdAsc(Enums.SalaryStatus status, LocalDate from, LocalDate to);
    }

    public interface ExpenseRepo extends JpaRepository<Expense, Long>, JpaSpecificationExecutor<Expense> {
        List<Expense> findByExpenseDateBetweenOrderByExpenseDateAscIdAsc(LocalDate from, LocalDate to);
    }

    public interface FacilityRepo extends JpaRepository<Facility, Long>, JpaSpecificationExecutor<Facility> {
        List<Facility> findByPublishedTrueOrderByDisplayOrderAscIdAsc();
    }

    public interface AlbumRepo extends JpaRepository<GalleryAlbum, Long>, JpaSpecificationExecutor<GalleryAlbum> {
        List<GalleryAlbum> findByPublishedTrueOrderByEventDateDescIdDesc();
    }

    public interface PhotoRepo extends JpaRepository<GalleryPhoto, Long> {
        List<GalleryPhoto> findByAlbumIdOrderByDisplayOrderAscIdAsc(Long albumId);

        List<GalleryPhoto> findByAlbumIdIn(Collection<Long> albumIds);

        long countByAlbumId(Long albumId);

        void deleteByAlbumId(Long albumId);
    }

    public interface AnnouncementRepo extends JpaRepository<Announcement, Long>, JpaSpecificationExecutor<Announcement> {
        List<Announcement> findByPublishedTrueAndPublishDateLessThanEqualOrderByPinnedDescPublishDateDescIdDesc(
                LocalDate today);
    }

    public interface SettingRepo extends JpaRepository<SiteSetting, Long>, JpaSpecificationExecutor<SiteSetting> {
        Optional<SiteSetting> findBySettingKey(String key);

        List<SiteSetting> findAllByOrderBySortOrderAscIdAsc();
    }

    public interface ManagementRepo extends JpaRepository<ManagementMember, Long>, JpaSpecificationExecutor<ManagementMember> {
        List<ManagementMember> findByPublishedTrueOrderByDisplayOrderAscIdAsc();
    }

    public interface TestimonialRepo extends JpaRepository<Testimonial, Long>, JpaSpecificationExecutor<Testimonial> {
        List<Testimonial> findByPublishedTrueOrderByDisplayOrderAscIdAsc();
    }

    public interface EnquiryRepo extends JpaRepository<Enquiry, Long>, JpaSpecificationExecutor<Enquiry> {}

    public interface AuditRepo extends JpaRepository<AuditLog, Long>, JpaSpecificationExecutor<AuditLog> {}
}
