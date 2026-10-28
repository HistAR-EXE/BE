package com.histar.be.profile.service;

import com.histar.be.common.exception.BusinessRuleException;
import com.histar.be.media.service.MediaStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.util.UUID;
import java.io.ByteArrayInputStream;
import javax.imageio.ImageIO;

@Service
@RequiredArgsConstructor
public class AvatarUploadService {
    private final MediaStorageService storage;
    public String upload(UUID userId, MultipartFile file) {
        if (file.isEmpty() || file.getSize() > 5 * 1024 * 1024) throw new BusinessRuleException("Ảnh cần có dung lượng từ 1 byte đến 5 MB");
        try {
            byte[] bytes = file.getBytes();
            String type = file.getContentType();
            String extension;
            if ("image/webp".equals(type)) {
                if (bytes.length < 30 || !new String(bytes, 0, 4, java.nio.charset.StandardCharsets.US_ASCII).equals("RIFF")
                        || !new String(bytes, 8, 4, java.nio.charset.StandardCharsets.US_ASCII).equals("WEBP")) throw new IllegalArgumentException();
                long size = Integer.toUnsignedLong(java.nio.ByteBuffer.wrap(bytes, 4, 4).order(java.nio.ByteOrder.LITTLE_ENDIAN).getInt());
                String chunk = new String(bytes, 12, 4, java.nio.charset.StandardCharsets.US_ASCII);
                if (size + 8 != bytes.length || !java.util.Set.of("VP8 ", "VP8L", "VP8X").contains(chunk)) throw new IllegalArgumentException();
                extension = "webp";
            } else {
                if (!java.util.Set.of("image/png", "image/jpeg").contains(type == null ? "" : type)) throw new IllegalArgumentException();
                try (var input = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
                    var readers = ImageIO.getImageReaders(input);
                    if (!readers.hasNext()) throw new IllegalArgumentException();
                    var reader = readers.next();
                    try {
                        reader.setInput(input);
                        String format = reader.getFormatName();
                        if (!("image/png".equals(type) && "png".equalsIgnoreCase(format)) && !("image/jpeg".equals(type) && "JPEG".equalsIgnoreCase(format))) throw new IllegalArgumentException();
                        if (reader.getWidth(0) > 8192 || reader.getHeight(0) > 8192 || (long) reader.getWidth(0) * reader.getHeight(0) > 24000000) throw new IllegalArgumentException();
                        if (reader.read(0) == null) throw new IllegalArgumentException();
                    } finally { reader.dispose(); }
                }
                extension = "image/png".equals(type) ? "png" : "jpg";
            }
            return storage.uploadImage(bytes, type, "avatars/" + userId + "/" + UUID.randomUUID() + "." + extension);
        } catch (java.io.IOException | IllegalArgumentException ex) {
            throw new BusinessRuleException("File ảnh không hợp lệ. Vui lòng chọn ảnh JPG, PNG hoặc WebP hợp lệ.");
        }
    }
}
