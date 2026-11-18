package com.gyanganga.school;

import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.gyanganga.school.model.Enums.Role;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.util.Map;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MvcResult;

class PublicAndUploadsIT extends IntegrationTest {

    private String admin() {
        return bearer(Role.ADMIN);
    }

    private MvcResult enquire(Map<String, Object> body, String ip) throws Exception {
        return mvc.perform(json(post("/api/public/enquiries"), body).with(r -> { r.setRemoteAddr(ip); return r; })).andReturn();
    }

    private static byte[] png(int w, int h, boolean alpha) throws Exception {
        BufferedImage img = new BufferedImage(w, h, alpha ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(img, "png", out);
        return out.toByteArray();
    }

    // ------------------------------------------------------------------ enquiries

    @Test
    void anEnquiryIsSavedAndShownToAdminsOnly() throws Exception {
        long before = enquiries.count();
        String name = "Parent " + uid();
        MvcResult r = enquire(Map.of("name", name, "phone", "+91 98765-43210", "classInterested", "Nursery",
                "message", "<script>alert(1)</script>"), freshIp());
        assertThat(r.getResponse().getStatus()).isEqualTo(200);
        assertThat(enquiries.count()).isEqualTo(before + 1);

        JsonNode list = get("/api/admin/enquiries?q={q}", admin(), 200, name);
        assertThat(list.get("total").asInt()).isEqualTo(1);
        assertThat(list.get("content").get(0).get("status").asText()).isEqualTo("NEW");
        get("/api/admin/enquiries", bearer(Role.TEACHER), 403);
    }

    @Test
    void enquiryInputIsValidated() throws Exception {
        String ip = freshIp();
        assertThat(enquire(Map.of("name", "", "phone", "9876543210"), ip).getResponse().getStatus()).isEqualTo(400);
        assertThat(enquire(Map.of("name", "A", "phone", "abc"), ip).getResponse().getStatus()).isEqualTo(400);
        assertThat(enquire(Map.of("name", "A", "phone", "9876543210", "email", "nope"), ip).getResponse().getStatus()).isEqualTo(400);
        assertThat(enquire(Map.of("name", "A", "phone", "9876543210", "message", "x".repeat(2001)), ip).getResponse().getStatus()).isEqualTo(400);
    }

    @Test
    void theHiddenSpamFieldMakesBotsLookSuccessfulWithoutSavingAnything() throws Exception {
        long before = enquiries.count();
        MvcResult r = enquire(Map.of("name", "Bot", "phone", "9876543210", "website", "http://spam.example"), freshIp());
        assertThat(r.getResponse().getStatus()).isEqualTo(200);
        assertThat(enquiries.count()).isEqualTo(before);
    }

    @Test
    void oneAddressCanSendOnlyFiveEnquiriesAnHour() throws Exception {
        String ip = freshIp();
        for (int i = 0; i < 5; i++) {
            assertThat(enquire(Map.of("name", "P" + i, "phone", "9876543210"), ip).getResponse().getStatus()).isEqualTo(200);
        }
        assertThat(enquire(Map.of("name", "P6", "phone", "9876543210"), ip).getResponse().getStatus()).isEqualTo(429);
        assertThat(enquire(Map.of("name", "Other", "phone", "9876543210"), freshIp()).getResponse().getStatus()).isEqualTo(200);
    }

    @Test
    void enquiryStatusCanBeUpdatedButNotTheParentsDetails() throws Exception {
        String name = "Keep " + uid();
        enquire(Map.of("name", name, "phone", "9876543210"), freshIp());
        JsonNode e = get("/api/admin/enquiries?q={q}", admin(), 200, name).get("content").get(0);
        Map<String, Object> change = om.convertValue(e, new com.fasterxml.jackson.core.type.TypeReference<>() {});
        change.put("status", "CONTACTED");
        change.put("name", "Tampered");
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .put("/api/admin/enquiries/" + e.get("id").asLong()).contentType("application/json")
                .content(om.writeValueAsString(change)).header("Authorization", admin())).andExpect(status().isOk());
        JsonNode after = get("/api/admin/enquiries/" + e.get("id").asLong(), admin(), 200);
        assertThat(after.get("status").asText()).isEqualTo("CONTACTED");
        assertThat(after.get("name").asText()).isEqualTo(name);
    }

    // ------------------------------------------------------------------ public content

    @Test
    void publicContentOnlyShowsPublishedItems() throws Exception {
        String visible = "Visible " + uid();
        String hidden = "Hidden " + uid();
        postJson("/api/admin/facilities", admin(), Map.of("title", visible, "published", true), 200);
        postJson("/api/admin/facilities", admin(), Map.of("title", hidden, "published", false), 200);
        String facilities = get("/api/public/facilities", null, 200).toString();
        assertThat(facilities).contains(visible).doesNotContain(hidden);

        postJson("/api/admin/announcements", admin(), Map.of("title", "Future " + visible, "publishDate",
                LocalDate.now().plusDays(3).toString(), "published", true), 200);
        postJson("/api/admin/announcements", admin(), Map.of("title", "Today " + visible, "publishDate",
                LocalDate.now().toString(), "published", true), 200);
        String news = get("/api/public/announcements", null, 200).toString();
        assertThat(news).contains("Today " + visible).doesNotContain("Future " + visible);
    }

    @Test
    void pinnedAnnouncementsComeFirst() throws Exception {
        String t = uid();
        postJson("/api/admin/announcements", admin(), Map.of("title", "Plain " + t, "publishDate", LocalDate.now().toString(), "published", true), 200);
        postJson("/api/admin/announcements", admin(), Map.of("title", "Pinned " + t, "publishDate",
                LocalDate.now().minusDays(30).toString(), "published", true, "pinned", true), 200);
        JsonNode list = get("/api/public/announcements", null, 200);
        int pinned = -1;
        int plain = -1;
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).get("title").asText().equals("Pinned " + t)) pinned = i;
            if (list.get(i).get("title").asText().equals("Plain " + t)) plain = i;
        }
        assertThat(pinned).isGreaterThanOrEqualTo(0).isLessThan(plain);
    }

    @Test
    void siteSettingsAreEditableButCannotBeCreatedOrDeletedAndKeepTheirKey() throws Exception {
        JsonNode all = get("/api/admin/settings?size=500", admin(), 200);
        JsonNode phone = null;
        for (JsonNode s : all.get("content")) if (s.get("key").asText().equals("school.phone")) phone = s;
        assertThat(phone).isNotNull();

        Map<String, Object> change = om.convertValue(phone, new com.fasterxml.jackson.core.type.TypeReference<>() {});
        String value = "+91 11111 " + uid();
        change.put("valueEn", value);
        change.put("key", "hacked.key");
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .put("/api/admin/settings/" + phone.get("id").asLong()).contentType("application/json")
                .content(om.writeValueAsString(change)).header("Authorization", admin())).andExpect(status().isOk());

        JsonNode site = get("/api/public/site", null, 200);
        assertThat(site.get("settings").get("school.phone").get("en").asText()).isEqualTo(value);
        assertThat(site.get("settings").has("hacked.key")).isFalse();
        postJson("/api/admin/settings", admin(), Map.of("key", "x", "groupName", "g", "label", "l"), 405);
    }

    @Test
    void publicSiteExposesStatsAndNoStaffData() throws Exception {
        String text = get("/api/public/site", null, 200).toString();
        assertThat(get("/api/public/site", null, 200).get("stats").has("students")).isTrue();
        bearer(Role.ADMIN); // make sure staff accounts exist
        assertThat(text).doesNotContain("passwordHash").doesNotContain("-it@example.com").doesNotContain("admin@gyanganga.school");
    }

    @Test
    void missingOrUnpublishedAlbumIs404() throws Exception {
        get("/api/public/albums/999999999", null, 404);
        long id = postJson("/api/admin/gallery/albums", admin(),
                Map.of("title", "Secret " + uid(), "category", "EVENTS", "published", false), 200).get("id").asLong();
        get("/api/public/albums/" + id, null, 404);
    }

    // ------------------------------------------------------------------ uploads

    @Test
    void uploadedImagesAreReencodedResizedAndServedPublicly() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "big.png", "image/png", png(3200, 1600, false));
        MvcResult r = mvc.perform(multipart("/api/admin/uploads").file(file).header("Authorization", admin()))
                .andExpect(status().isOk()).andReturn();
        String url = body(r).get("url").asText();
        assertThat(url).startsWith("/uploads/").endsWith(".jpg");

        MvcResult served = mvc.perform(MockMvcRequestBuilders.get(url)).andExpect(status().isOk()).andReturn();
        BufferedImage img = ImageIO.read(new java.io.ByteArrayInputStream(served.getResponse().getContentAsByteArray()));
        assertThat(img.getWidth()).isEqualTo(1600);   // longest side capped
        assertThat(img.getHeight()).isEqualTo(800);   // aspect ratio kept
        assertThat(served.getResponse().getHeader("Cache-Control")).contains("max-age=2592000");
    }

    @Test
    void imagesWithTransparencyStayPng() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "logo.png", "image/png", png(50, 50, true));
        MvcResult r = mvc.perform(multipart("/api/admin/uploads").file(file).header("Authorization", admin()))
                .andExpect(status().isOk()).andReturn();
        assertThat(body(r).get("url").asText()).endsWith(".png");
    }

    @Test
    void nonImagesAreRejectedEvenWithAnImageNameAndContentType() throws Exception {
        MockMultipartFile fake = new MockMultipartFile("file", "photo.jpg", "image/jpeg", "<?php echo 1; ?>".getBytes());
        mvc.perform(multipart("/api/admin/uploads").file(fake).header("Authorization", admin())).andExpect(status().isBadRequest());
        MockMultipartFile empty = new MockMultipartFile("file", "a.png", "image/png", new byte[0]);
        mvc.perform(multipart("/api/admin/uploads").file(empty).header("Authorization", admin())).andExpect(status().isBadRequest());
    }

    @Test
    void onlyAdminsCanUploadPublicImages() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "a.png", "image/png", png(10, 10, false));
        mvc.perform(multipart("/api/admin/uploads").file(file)).andExpect(status().isUnauthorized());
        mvc.perform(multipart("/api/admin/uploads").file(file).header("Authorization", bearer(Role.TEACHER))).andExpect(status().isForbidden());
    }

    @Test
    void studentDocumentsArePrivateAndOnlyRealPdfsOrImagesAreAccepted() throws Exception {
        long classId = createClass(admin());
        long studentId = createStudent(admin(), classId, Map.of());
        byte[] pdf = "%PDF-1.4\n1 0 obj<<>>endobj\ntrailer<<>>\n%%EOF".getBytes();

        MvcResult ok = mvc.perform(multipart("/api/admin/students/" + studentId + "/documents")
                .file(new MockMultipartFile("file", "birth.pdf", "application/pdf", pdf))
                .param("name", "Birth certificate").header("Authorization", admin()))
                .andExpect(status().isOk()).andReturn();
        String url = body(ok).get("fileUrl").asText();
        assertThat(url).matches("/api/admin/files/[a-f0-9-]{36}\\.pdf");

        // a script renamed to .pdf is refused
        mvc.perform(multipart("/api/admin/students/" + studentId + "/documents")
                .file(new MockMultipartFile("file", "evil.pdf", "application/pdf", "<script>alert(1)</script>".getBytes()))
                .param("name", "Evil").header("Authorization", admin())).andExpect(status().isBadRequest());

        // private: no anonymous access, staff can read it
        mvc.perform(MockMvcRequestBuilders.get(url)).andExpect(status().isUnauthorized());
        MvcResult read = mvc.perform(MockMvcRequestBuilders.get(url).header("Authorization", bearer(Role.TEACHER))).andExpect(status().isOk()).andReturn();
        assertThat(read.getResponse().getContentType()).isEqualTo("application/pdf");
        assertThat(read.getResponse().getHeader("X-Content-Type-Options")).isEqualTo("nosniff");
        assertThat(read.getResponse().getHeader("Cache-Control")).contains("no-store");

        // listed, and teachers cannot add or remove documents
        assertThat(get("/api/admin/students/" + studentId + "/documents", bearer(Role.TEACHER), 200)).hasSize(1);
        mvc.perform(multipart("/api/admin/students/" + studentId + "/documents")
                .file(new MockMultipartFile("file", "b.pdf", "application/pdf", pdf)).param("name", "x")
                .header("Authorization", bearer(Role.TEACHER))).andExpect(status().isForbidden());
    }

    @Test
    void privateFileNamesCannotEscapeTheFolder() throws Exception {
        for (String name : new String[] {"..%2F..%2Fapplication.yml", "..%5C..%5Cpom.xml", "not-a-uuid.pdf", "%2e%2e/pom.xml"}) {
            MvcResult r = mvc.perform(MockMvcRequestBuilders.get("/api/admin/files/" + name).header("Authorization", admin())).andReturn();
            assertThat(r.getResponse().getStatus()).as(name).isBetween(400, 404);
        }
    }

    @Test
    void galleryPhotosCanBeAddedCaptionedAndRemoved() throws Exception {
        long albumId = postJson("/api/admin/gallery/albums", admin(),
                Map.of("title", "Album " + uid(), "category", "SPORTS", "published", true), 200).get("id").asLong();
        MvcResult up = mvc.perform(multipart("/api/admin/gallery/albums/" + albumId + "/photos")
                .file(new MockMultipartFile("files", "1.png", "image/png", png(20, 20, false)))
                .file(new MockMultipartFile("files", "2.png", "image/png", png(20, 20, false)))
                .header("Authorization", admin())).andExpect(status().isOk()).andReturn();
        assertThat(body(up)).hasSize(2);

        JsonNode pub = get("/api/public/albums/" + albumId, null, 200);
        assertThat(pub.get("photos")).hasSize(2);
        assertThat(pub.get("album").get("photoCount").asInt()).isEqualTo(2);
        assertThat(pub.get("album").get("coverUrl").asText()).startsWith("/uploads/");

        long photoId = body(up).get(0).get("id").asLong();
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .put("/api/admin/gallery/albums/" + albumId + "/photos/" + photoId).contentType("application/json")
                .content("{\"caption\":\"Race day\"}").header("Authorization", admin())).andExpect(status().isOk());
        assertThat(get("/api/public/albums/" + albumId, null, 200).toString()).contains("Race day");

        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .delete("/api/admin/gallery/albums/" + albumId + "/photos/" + photoId).header("Authorization", admin()))
                .andExpect(status().isOk());
        assertThat(get("/api/public/albums/" + albumId, null, 200).get("photos")).hasSize(1);

        // deleting the album removes the remaining photo records too
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .delete("/api/admin/gallery/albums/" + albumId).header("Authorization", admin())).andExpect(status().isOk());
        get("/api/public/albums/" + albumId, null, 404);
    }
}
