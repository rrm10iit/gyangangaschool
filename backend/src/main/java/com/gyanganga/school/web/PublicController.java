package com.gyanganga.school.web;

import com.gyanganga.school.common.AuditService;
import com.gyanganga.school.common.RateLimiter;
import com.gyanganga.school.config.AppProps;
import com.gyanganga.school.model.*;
import com.gyanganga.school.repo.Repos;
import com.gyanganga.school.service.EmailService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Duration;
import java.time.LocalDate;
import java.time.Year;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/public")
public class PublicController {
    public record SettingValue(String en, String hi) {}

    public record Stats(long students, long teachers, long classes, int years) {}

    public record SiteView(Map<String, SettingValue> settings, Stats stats) {}

    public record AlbumView(Long id, String title, String titleHi, String category, LocalDate eventDate,
                            String coverUrl, long photoCount) {}

    public record AlbumDetail(AlbumView album, List<GalleryPhoto> photos) {}

    public record EnquiryRequest(
            @NotBlank @Size(max = 150) String name,
            @NotBlank @Pattern(regexp = "^[0-9+()\\-\\s]{7,20}$", message = "Enter a valid phone number") String phone,
            @Email @Size(max = 190) String email,
            @Size(max = 60) String classInterested,
            @Size(max = 2000) String message,
            @Size(max = 200) String website) {}

    private final Repos.SettingRepo settings;
    private final Repos.StudentRepo students;
    private final Repos.TeacherRepo teachers;
    private final Repos.ClassRepo classes;
    private final Repos.FacilityRepo facilities;
    private final Repos.AlbumRepo albums;
    private final Repos.PhotoRepo photos;
    private final Repos.AnnouncementRepo announcements;
    private final Repos.TestimonialRepo testimonials;
    private final Repos.ManagementRepo management;
    private final Repos.EnquiryRepo enquiries;
    private final RateLimiter limiter;
    private final EmailService email;
    private final AuditService audit;
    private final AppProps props;

    public PublicController(Repos.SettingRepo settings, Repos.StudentRepo students, Repos.TeacherRepo teachers,
                            Repos.ClassRepo classes, Repos.FacilityRepo facilities, Repos.AlbumRepo albums,
                            Repos.PhotoRepo photos, Repos.AnnouncementRepo announcements,
                            Repos.TestimonialRepo testimonials, Repos.ManagementRepo management,
                            Repos.EnquiryRepo enquiries, RateLimiter limiter, EmailService email,
                            AuditService audit, AppProps props) {
        this.settings = settings;
        this.students = students;
        this.teachers = teachers;
        this.classes = classes;
        this.facilities = facilities;
        this.albums = albums;
        this.photos = photos;
        this.announcements = announcements;
        this.testimonials = testimonials;
        this.management = management;
        this.enquiries = enquiries;
        this.limiter = limiter;
        this.email = email;
        this.audit = audit;
        this.props = props;
    }

    private static <T> ResponseEntity<T> cached(T body) {
        return ResponseEntity.ok().cacheControl(CacheControl.maxAge(Duration.ofSeconds(30)).cachePublic()).body(body);
    }

    @GetMapping("/site")
    public ResponseEntity<SiteView> site() {
        Map<String, SettingValue> map = new LinkedHashMap<>();
        for (SiteSetting s : settings.findAllByOrderBySortOrderAscIdAsc()) {
            map.put(s.getSettingKey(), new SettingValue(s.getValueEn(), s.getValueHi()));
        }
        int years = 0;
        SettingValue founded = map.get("school.founded");
        if (founded != null && founded.en() != null) {
            try {
                years = Math.max(0, Year.now().getValue() - Integer.parseInt(founded.en().trim()));
            } catch (NumberFormatException ignored) {
                // leave at 0
            }
        }
        Stats stats = new Stats(students.countByStatus(Enums.StudentStatus.ACTIVE), teachers.countByActiveTrue(),
                classes.count(), years);
        return cached(new SiteView(map, stats));
    }

    @GetMapping("/classes")
    public ResponseEntity<List<SchoolClass>> classes() {
        return cached(classes.findAllByOrderByDisplayOrderAscIdAsc());
    }

    @GetMapping("/facilities")
    public ResponseEntity<List<Facility>> facilities() {
        return cached(facilities.findByPublishedTrueOrderByDisplayOrderAscIdAsc());
    }

    @GetMapping("/announcements")
    public ResponseEntity<List<Announcement>> announcements() {
        return cached(announcements
                .findByPublishedTrueAndPublishDateLessThanEqualOrderByPinnedDescPublishDateDescIdDesc(LocalDate.now()));
    }

    @GetMapping("/testimonials")
    public ResponseEntity<List<Testimonial>> testimonials() {
        return cached(testimonials.findByPublishedTrueOrderByDisplayOrderAscIdAsc());
    }

    @GetMapping("/management")
    public ResponseEntity<List<ManagementMember>> management() {
        return cached(management.findByPublishedTrueOrderByDisplayOrderAscIdAsc());
    }

    @GetMapping("/albums")
    public ResponseEntity<List<AlbumView>> albums() {
        List<GalleryAlbum> list = albums.findByPublishedTrueOrderByEventDateDescIdDesc();
        Map<Long, List<GalleryPhoto>> byAlbum = list.isEmpty() ? Map.of()
                : photos.findByAlbumIdIn(list.stream().map(GalleryAlbum::getId).toList()).stream()
                        .collect(Collectors.groupingBy(GalleryPhoto::getAlbumId));
        return cached(list.stream().map(a -> view(a, byAlbum.getOrDefault(a.getId(), List.of()))).toList());
    }

    @GetMapping("/albums/{id}")
    public ResponseEntity<AlbumDetail> album(@PathVariable Long id) {
        GalleryAlbum a = albums.findById(id).filter(GalleryAlbum::isPublished)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Album not found"));
        List<GalleryPhoto> list = photos.findByAlbumIdOrderByDisplayOrderAscIdAsc(id);
        return cached(new AlbumDetail(view(a, list), list));
    }

    private AlbumView view(GalleryAlbum a, List<GalleryPhoto> list) {
        String cover = list.stream().min(java.util.Comparator.comparingInt(GalleryPhoto::getDisplayOrder)
                .thenComparing(GalleryPhoto::getId)).map(GalleryPhoto::getUrl).orElse(null);
        return new AlbumView(a.getId(), a.getTitle(), a.getTitleHi(), a.getCategory(), a.getEventDate(), cover,
                list.size());
    }

    @PostMapping("/enquiries")
    public Map<String, String> enquire(@Valid @RequestBody EnquiryRequest req, HttpServletRequest http) {
        // Honeypot: real visitors never fill this hidden field. Pretend success to bots.
        if (req.website() != null && !req.website().isBlank()) return Map.of("status", "ok");
        if (!limiter.tryAcquire("enquiry:" + http.getRemoteAddr(), 5, Duration.ofHours(1))) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Too many enquiries from your connection. Please call the school office instead.");
        }
        Enquiry e = new Enquiry();
        e.setName(req.name().trim());
        e.setPhone(req.phone().trim());
        e.setEmail(blankToNull(req.email()));
        e.setClassInterested(blankToNull(req.classInterested()));
        e.setMessage(blankToNull(req.message()));
        enquiries.save(e);
        audit.logAs("public", "CREATE", "Enquiry", e.getId(), "New admission enquiry from " + e.getName());
        sendEmails(e);
        return Map.of("status", "ok");
    }

    private void sendEmails(Enquiry e) {
        String schoolName = settingEn("school.name", "Gyan Ganga School");
        String notify = props.brevo().notifyEmail();
        if (notify == null || notify.isBlank()) notify = settingEn("school.email", null);
        String esc = null;
        if (notify != null && !notify.isBlank()) {
            String html = "<h3>New admission enquiry</h3><table cellpadding=\"6\">"
                    + row("Name", e.getName()) + row("Phone", e.getPhone()) + row("Email", e.getEmail())
                    + row("Class", e.getClassInterested()) + row("Message", e.getMessage()) + "</table>"
                    + "<p>Review it in the admin panel: " + EmailService.esc(props.publicUrl()) + "/admin/enquiries</p>";
            email.send(notify, schoolName, "New admission enquiry: " + clean(e.getName()), html, e.getEmail());
        }
        if (e.getEmail() != null) {
            String html = "<p>Dear " + EmailService.esc(e.getName()) + ",</p>"
                    + "<p>Thank you for your interest in " + EmailService.esc(schoolName)
                    + ". We have received your admission enquiry and our office will contact you shortly on "
                    + EmailService.esc(e.getPhone()) + ".</p><p>Warm regards,<br/>" + EmailService.esc(schoolName) + "</p>";
            email.send(e.getEmail(), e.getName(), "We received your enquiry - " + schoolName, html, notify);
        }
    }

    private static String row(String k, String v) {
        return "<tr><td><b>" + k + "</b></td><td>" + EmailService.esc(v) + "</td></tr>";
    }

    /** Removes control characters (prevents header/subject injection). */
    private static String clean(String s) {
        return s.replaceAll("[\\p{Cntrl}]", " ");
    }

    private String settingEn(String key, String fallback) {
        return settings.findBySettingKey(key).map(SiteSetting::getValueEn).filter(v -> !v.isBlank()).orElse(fallback);
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
