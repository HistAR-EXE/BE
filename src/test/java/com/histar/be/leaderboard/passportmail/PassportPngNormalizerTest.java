package com.histar.be.leaderboard.passportmail;

import static org.junit.jupiter.api.Assertions.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class PassportPngNormalizerTest {
    @Test void acceptsOnlyExactPassportPng() throws Exception {
        var bytes = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(1200,1500,BufferedImage.TYPE_INT_ARGB), "png", bytes);
        byte[] normalized = new PassportPngNormalizer().normalize(new MockMultipartFile("image","passport.png","image/png",bytes.toByteArray()));
        assertEquals(1200, ImageIO.read(new java.io.ByteArrayInputStream(normalized)).getWidth());
    }
    @Test void rejectsBadSignatureAndDimensionsBeforeDecode() throws Exception {
        assertThrows(PassportMailException.class, () -> new PassportPngNormalizer().normalize(new MockMultipartFile("image","a.png","image/png",new byte[24])));
        var bytes = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(1,1,BufferedImage.TYPE_INT_ARGB), "png", bytes);
        assertThrows(PassportMailException.class, () -> new PassportPngNormalizer().normalize(new MockMultipartFile("image","a.png","image/png",bytes.toByteArray())));
    }
    @Test void rejectsOversizeAndWrongMime() {
        assertThrows(PassportMailException.class, () -> new PassportPngNormalizer().normalize(new MockMultipartFile("image","a.png","image/png",new byte[2097153])));
        assertThrows(PassportMailException.class, () -> new PassportPngNormalizer().normalize(new MockMultipartFile("image","a.png","image/jpeg",new byte[24])));
    }
    @Test void stripsAncillaryMetadata() throws Exception {
        var source=new ByteArrayOutputStream();ImageIO.write(new BufferedImage(1200,1500,BufferedImage.TYPE_INT_ARGB),"png",source);
        byte[] original=source.toByteArray();byte[] text="Note\0PRIVATE_METADATA_TO_STRIP".getBytes(java.nio.charset.StandardCharsets.ISO_8859_1);
        var payload=new ByteArrayOutputStream();var chunk=new java.io.DataOutputStream(payload);chunk.writeBytes("tEXt");chunk.write(text);
        byte[] data=payload.toByteArray();var crc=new java.util.zip.CRC32();crc.update(data);
        var injected=new ByteArrayOutputStream();injected.write(original,0,33);var out=new java.io.DataOutputStream(injected);out.writeInt(text.length);out.write(data);out.writeInt((int)crc.getValue());injected.write(original,33,original.length-33);
        byte[] normalized=new PassportPngNormalizer().normalize(new MockMultipartFile("image","x.png","image/png",injected.toByteArray()));
        assertFalse(new String(normalized,java.nio.charset.StandardCharsets.ISO_8859_1).contains("PRIVATE_METADATA_TO_STRIP"));
    }}

