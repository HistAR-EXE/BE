package com.histar.be.profile;
import com.histar.be.profile.service.AvatarUploadService;
import com.histar.be.media.service.MediaStorageService;
import org.springframework.mock.web.MockMultipartFile;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.UUID;
class AvatarUploadServiceTest {
 @Test void validPngUsesUserScopedStorageKey() throws Exception {
  var output=new java.io.ByteArrayOutputStream();
  javax.imageio.ImageIO.write(new java.awt.image.BufferedImage(2,2,java.awt.image.BufferedImage.TYPE_INT_RGB), "png", output);
  var storage=mock(MediaStorageService.class);
  var id=UUID.randomUUID();
  when(storage.uploadImage(any(),eq("image/png"),startsWith("avatars/"+id+"/"))).thenReturn("/media/avatar.png");
  assertThat(new AvatarUploadService(storage).upload(id,new MockMultipartFile("file","../../x.png","image/png",output.toByteArray()))).isEqualTo("/media/avatar.png");
 }
 @Test void rejectsFakeImagesBeforeStorage() {
  var storage=mock(MediaStorageService.class);
  assertThatThrownBy(() -> new AvatarUploadService(storage).upload(UUID.randomUUID(),new MockMultipartFile("file","a.png","image/png","not an image".getBytes()))).isInstanceOf(RuntimeException.class);
  verifyNoInteractions(storage);
 }
 @Test void rejectsEmptyAndOversizeFiles() {
  var service=new AvatarUploadService(mock(MediaStorageService.class));
  for(byte[] bytes:new byte[][]{new byte[0],new byte[5*1024*1024+1]}) assertThatThrownBy(() -> service.upload(UUID.randomUUID(),new MockMultipartFile("file","a.jpg","image/jpeg",bytes))).isInstanceOf(RuntimeException.class);
 }
}
