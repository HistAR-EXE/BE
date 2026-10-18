package com.histar.be.leaderboard.passportmail;

import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.ByteBuffer;
import java.util.Arrays;
import javax.imageio.ImageIO;
import org.springframework.web.multipart.MultipartFile;

/** Reads bounded IHDR dimensions before decoding; writes fresh pixels only, stripping PNG metadata. */
public class PassportPngNormalizer {
    static final int MAX_BYTES=2*1024*1024;
    private static final byte[] SIGNATURE={(byte)137,80,78,71,13,10,26,10};
    public byte[] normalize(MultipartFile file) {
        if(file==null || file.isEmpty() || file.getSize()>MAX_BYTES || !"image/png".equalsIgnoreCase(file.getContentType())) throw invalid();
        try(InputStream input=file.getInputStream()) {
            byte[] bytes=input.readNBytes(MAX_BYTES+1);
            if(bytes.length>MAX_BYTES || bytes.length<33 || !Arrays.equals(SIGNATURE,Arrays.copyOf(bytes,8))) throw invalid();
            ByteBuffer header=ByteBuffer.wrap(bytes);
            if(header.getInt(8)!=13 || header.getInt(12)!=0x49484452 || header.getInt(16)!=1200 || header.getInt(20)!=1500) throw invalid();
            BufferedImage decoded;
            try(var pixels=new javax.imageio.stream.MemoryCacheImageInputStream(new ByteArrayInputStream(bytes))) {
                var readers=ImageIO.getImageReadersByFormatName("png");
                if(!readers.hasNext()) throw invalid();
                var reader=readers.next();
                try {reader.setInput(pixels,true,true);decoded=reader.read(0);} finally {reader.dispose();}
            }
            if(decoded==null || decoded.getWidth()!=1200 || decoded.getHeight()!=1500) throw invalid();
            BufferedImage clean=new BufferedImage(1200,1500,BufferedImage.TYPE_INT_ARGB);
            var graphics=clean.createGraphics();
            try { graphics.drawImage(decoded,0,0,null); } finally { graphics.dispose(); decoded.flush(); }
            var output=new ByteArrayOutputStream();
            try(var pixels=new javax.imageio.stream.MemoryCacheImageOutputStream(output)) {
                if(!ImageIO.write(clean,"png",pixels)) throw invalid();
            } finally { clean.flush(); }
            if(output.size()>MAX_BYTES) throw invalid();
            return output.toByteArray();
        } catch(IOException | IllegalArgumentException ex) { throw invalid(); }
    }
    private PassportMailException invalid() {
        return new PassportMailException(422,"INVALID_IMAGE","Vui lòng gửi ảnh hộ chiếu PNG đúng 1200 × 1500 pixel, tối đa 2 MiB.");
    }
}


