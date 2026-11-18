package com.gyanganga.school.service;

import com.gyanganga.school.config.AppProps;
import jakarta.annotation.PostConstruct;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class FileStorageService {
    private static final int MAX_IMAGE_SIDE = 1600;
    private static final Pattern PRIVATE_NAME = Pattern.compile("[a-f0-9\\-]{36}\\.(pdf|jpg|png)");

    private final AppProps props;
    private Path publicDir;
    private Path privateDir;

    public FileStorageService(AppProps props) {
        this.props = props;
    }

    @PostConstruct
    void init() throws IOException {
        Path root = Path.of(props.uploadDir()).toAbsolutePath().normalize();
        publicDir = root.resolve("public");
        privateDir = root.resolve("private");
        Files.createDirectories(publicDir);
        Files.createDirectories(privateDir);
    }

    public Path publicDir() {
        return publicDir;
    }

    /** Validates, downsizes and re-encodes an image (this also strips EXIF metadata). Returns the public URL. */
    public String storeImage(MultipartFile file) {
        if (file == null || file.isEmpty()) throw bad("No file uploaded");
        try (InputStream in = file.getInputStream()) {
            BufferedImage src = ImageIO.read(in);
            if (src == null) throw bad("Unsupported image. Please upload a JPG or PNG file.");
            BufferedImage img = scale(src);
            boolean alpha = img.getColorModel().hasAlpha();
            String ext = alpha ? "png" : "jpg";
            Path dir = publicDir.resolve(LocalDate.now().getYear() + "/" + String.format("%02d", LocalDate.now().getMonthValue()));
            Files.createDirectories(dir);
            String name = UUID.randomUUID() + "." + ext;
            Path target = dir.resolve(name);
            if (alpha) {
                ImageIO.write(img, "png", target.toFile());
            } else {
                writeJpeg(img, target);
            }
            return "/uploads/" + publicDir.relativize(target).toString().replace('\\', '/');
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not save the image");
        }
    }

    /** Stores a private document (PDF, JPG or PNG). Returns the authenticated download URL. */
    public String storeDocument(MultipartFile file) {
        if (file == null || file.isEmpty()) throw bad("No file uploaded");
        try {
            byte[] head = new byte[8];
            int n;
            try (InputStream in = file.getInputStream()) {
                n = in.readNBytes(head, 0, 8);
            }
            String ext;
            if (n >= 4 && head[0] == '%' && head[1] == 'P' && head[2] == 'D' && head[3] == 'F') ext = "pdf";
            else if (n >= 3 && (head[0] & 0xFF) == 0xFF && (head[1] & 0xFF) == 0xD8) ext = "jpg";
            else if (n >= 4 && (head[0] & 0xFF) == 0x89 && head[1] == 'P' && head[2] == 'N' && head[3] == 'G') ext = "png";
            else throw bad("Only PDF, JPG or PNG documents are allowed");
            String name = UUID.randomUUID() + "." + ext;
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, privateDir.resolve(name), StandardCopyOption.REPLACE_EXISTING);
            }
            return "/api/admin/files/" + name;
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not save the document");
        }
    }

    public Path privateFile(String name) {
        if (name == null || !PRIVATE_NAME.matcher(name).matches()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Not found");
        }
        Path p = privateDir.resolve(name).normalize();
        if (!p.startsWith(privateDir) || !Files.isRegularFile(p)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Not found");
        }
        return p;
    }

    public static String contentType(String name) {
        String n = name.toLowerCase(Locale.ROOT);
        if (n.endsWith(".pdf")) return "application/pdf";
        if (n.endsWith(".png")) return "image/png";
        return "image/jpeg";
    }

    /** Copies a bundled resource into the public upload dir (used for seed data). */
    public String copySeedImage(InputStream in, String fileName) throws IOException {
        Path dir = publicDir.resolve("seed");
        Files.createDirectories(dir);
        Files.copy(in, dir.resolve(fileName), StandardCopyOption.REPLACE_EXISTING);
        return "/uploads/seed/" + fileName;
    }

    private static BufferedImage scale(BufferedImage src) {
        int w = src.getWidth();
        int h = src.getHeight();
        double ratio = Math.min(1.0, (double) MAX_IMAGE_SIDE / Math.max(w, h));
        int nw = Math.max(1, (int) Math.round(w * ratio));
        int nh = Math.max(1, (int) Math.round(h * ratio));
        boolean alpha = src.getColorModel().hasAlpha();
        BufferedImage out = new BufferedImage(nw, nh, alpha ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        if (!alpha) {
            g.setColor(java.awt.Color.WHITE);
            g.fillRect(0, 0, nw, nh);
        }
        g.drawImage(src, 0, 0, nw, nh, null);
        g.dispose();
        return out;
    }

    private static void writeJpeg(BufferedImage img, Path target) throws IOException {
        ImageWriter writer = ImageIO.getImageWritersByFormatName("jpeg").next();
        ImageWriteParam param = writer.getDefaultWriteParam();
        param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        param.setCompressionQuality(0.82f);
        try (ImageOutputStream out = ImageIO.createImageOutputStream(target.toFile())) {
            writer.setOutput(out);
            writer.write(null, new IIOImage(img, null, null), param);
        } finally {
            writer.dispose();
        }
    }

    private static ResponseStatusException bad(String msg) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, msg);
    }
}
