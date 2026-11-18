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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

/** Admin CMS endpoints. Everything under /api/admin that is not academic or finance requires ADMIN. */
public final class ContentControllers {
    private ContentControllers() {}

    @RestController
    @RequestMapping("/api/admin/facilities")
    public static class FacilityController extends CrudController<Facility> {
        public FacilityController(Repos.FacilityRepo repo, AuditService audit) {
            super(repo, audit, "Facility");
        }

        @Override
        protected Sort defaultSort() {
            return Sort.by("displayOrder", "id");
        }

        @Override
        protected Specification<Facility> filter(String q, Map<String, String> p) {
            return likeAny(q, "title");
        }

        @Override
        protected String describe(Facility f) {
            return "Facility " + f.getTitle();
        }
    }

    @RestController
    @RequestMapping("/api/admin/announcements")
    public static class AnnouncementController extends CrudController<Announcement> {
        public AnnouncementController(Repos.AnnouncementRepo repo, AuditService audit) {
            super(repo, audit, "Announcement");
        }

        @Override
        protected Sort defaultSort() {
            return Sort.by(Sort.Direction.DESC, "publishDate", "id");
        }

        @Override
        protected Specification<Announcement> filter(String q, Map<String, String> p) {
            return likeAny(q, "title");
        }

        @Override
        protected String describe(Announcement a) {
            return "Announcement " + a.getTitle();
        }
    }

    @RestController
    @RequestMapping("/api/admin/management")
    public static class ManagementController extends CrudController<ManagementMember> {
        public ManagementController(Repos.ManagementRepo repo, AuditService audit) {
            super(repo, audit, "Management member");
        }

        @Override
        protected Sort defaultSort() {
            return Sort.by("displayOrder", "id");
        }

        @Override
        protected String describe(ManagementMember m) {
            return "Management member " + m.getName();
        }
    }

    @RestController
    @RequestMapping("/api/admin/testimonials")
    public static class TestimonialController extends CrudController<Testimonial> {
        public TestimonialController(Repos.TestimonialRepo repo, AuditService audit) {
            super(repo, audit, "Testimonial");
        }

        @Override
        protected Sort defaultSort() {
            return Sort.by("displayOrder", "id");
        }

        @Override
        protected String describe(Testimonial t) {
            return "Testimonial by " + t.getName();
        }
    }

    /** Site text settings: edit only. Keys are created by migrations / the seeder. */
    @RestController
    @RequestMapping("/api/admin/settings")
    public static class SettingController extends CrudController<SiteSetting> {
        public SettingController(Repos.SettingRepo repo, AuditService audit) {
            super(repo, audit, "Site setting");
        }

        @Override
        protected Sort defaultSort() {
            return Sort.by("sortOrder", "id");
        }

        @Override
        protected String[] readOnlyOnUpdate() {
            return new String[] {"settingKey", "groupName", "label", "kind", "sortOrder"};
        }

        @Override
        protected String describe(SiteSetting s) {
            return "Setting '" + s.getLabel() + "'";
        }

        @Override
        public SiteSetting create(SiteSetting body) {
            throw new ResponseStatusException(HttpStatus.METHOD_NOT_ALLOWED, "Settings cannot be created");
        }

        @Override
        public void delete(Long id) {
            throw new ResponseStatusException(HttpStatus.METHOD_NOT_ALLOWED, "Settings cannot be deleted");
        }
    }

    @RestController
    @RequestMapping("/api/admin/enquiries")
    public static class EnquiryController extends CrudController<Enquiry> {
        public EnquiryController(Repos.EnquiryRepo repo, AuditService audit) {
            super(repo, audit, "Enquiry");
        }

        @Override
        protected Specification<Enquiry> filter(String q, Map<String, String> p) {
            String status = p.get("status");
            Specification<Enquiry> st = null;
            if (status != null && !status.isBlank()) {
                try {
                    st = eq("status", Enums.EnquiryStatus.valueOf(status));
                } catch (IllegalArgumentException e) {
                    throw bad("Invalid status");
                }
            }
            return Specification.allOf(likeAny(q, "name", "phone", "email", "message"), st);
        }

        @Override
        protected String[] readOnlyOnUpdate() {
            return new String[] {"name", "phone", "email", "classInterested", "message"};
        }

        @Override
        protected String describe(Enquiry e) {
            return "Enquiry from " + e.getName() + " (" + e.getStatus() + ")";
        }

        @Override
        public Enquiry create(Enquiry body) {
            throw new ResponseStatusException(HttpStatus.METHOD_NOT_ALLOWED, "Enquiries come from the public form");
        }
    }

    @RestController
    @RequestMapping("/api/admin/audit-log")
    public static class AuditController extends CrudController<AuditLog> {
        public AuditController(Repos.AuditRepo repo, AuditService audit) {
            super(repo, audit, "Audit log");
        }

        @Override
        protected Specification<AuditLog> filter(String q, Map<String, String> p) {
            return likeAny(q, "userEmail", "action", "entityType", "summary");
        }

        @Override
        public AuditLog create(AuditLog body) {
            throw new ResponseStatusException(HttpStatus.METHOD_NOT_ALLOWED, "Read only");
        }

        @Override
        public AuditLog update(Long id, AuditLog body) {
            throw new ResponseStatusException(HttpStatus.METHOD_NOT_ALLOWED, "Read only");
        }

        @Override
        public void delete(Long id) {
            throw new ResponseStatusException(HttpStatus.METHOD_NOT_ALLOWED, "Read only");
        }
    }

    /** Gallery albums, plus photo management inside an album. */
    @RestController
    @RequestMapping("/api/admin/gallery/albums")
    public static class AlbumController extends CrudController<GalleryAlbum> {
        private final Repos.PhotoRepo photos;
        private final FileStorageService storage;

        public AlbumController(Repos.AlbumRepo repo, AuditService audit, Repos.PhotoRepo photos,
                               FileStorageService storage) {
            super(repo, audit, "Album");
            this.photos = photos;
            this.storage = storage;
        }

        @Override
        protected Specification<GalleryAlbum> filter(String q, Map<String, String> p) {
            return likeAny(q, "title", "category");
        }

        @Override
        protected String describe(GalleryAlbum a) {
            return "Album " + a.getTitle();
        }

        @Override
        protected void beforeDelete(GalleryAlbum a) {
            photos.deleteByAlbumId(a.getId());
        }

        @GetMapping("/{id}/photos")
        public List<GalleryPhoto> photos(@PathVariable Long id) {
            find(id);
            return photos.findByAlbumIdOrderByDisplayOrderAscIdAsc(id);
        }

        @PostMapping("/{id}/photos")
        @Transactional
        public List<GalleryPhoto> upload(@PathVariable Long id, @RequestParam("files") List<MultipartFile> files) {
            GalleryAlbum album = find(id);
            if (files.isEmpty() || files.size() > 30) throw bad("Upload between 1 and 30 photos at a time");
            long order = photos.countByAlbumId(id);
            List<GalleryPhoto> saved = new java.util.ArrayList<>();
            for (MultipartFile f : files) {
                GalleryPhoto p = new GalleryPhoto();
                p.setAlbumId(id);
                p.setUrl(storage.storeImage(f));
                p.setDisplayOrder((int) order++);
                saved.add(photos.save(p));
            }
            audit.log("CREATE", "Gallery photo", id, "Added " + saved.size() + " photo(s) to album " + album.getTitle());
            return saved;
        }

        @PutMapping("/{id}/photos/{photoId}")
        @Transactional
        public GalleryPhoto updatePhoto(@PathVariable Long id, @PathVariable Long photoId,
                                        @RequestBody Map<String, Object> body) {
            GalleryPhoto p = photo(id, photoId);
            Object caption = body.get("caption");
            if (caption != null && caption.toString().length() > 250) throw bad("Caption is too long");
            p.setCaption(caption == null || caption.toString().isBlank() ? null : caption.toString().trim());
            if (body.get("displayOrder") instanceof Number n) p.setDisplayOrder(n.intValue());
            return photos.save(p);
        }

        @DeleteMapping("/{id}/photos/{photoId}")
        @Transactional
        public void deletePhoto(@PathVariable Long id, @PathVariable Long photoId) {
            GalleryPhoto p = photo(id, photoId);
            photos.delete(p);
            audit.log("DELETE", "Gallery photo", photoId, "Removed photo from album #" + id);
        }

        private GalleryPhoto photo(Long albumId, Long photoId) {
            return photos.findById(photoId)
                    .filter(p -> p.getAlbumId().equals(albumId))
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Photo not found"));
        }
    }
}
