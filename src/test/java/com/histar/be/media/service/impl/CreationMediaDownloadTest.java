package com.histar.be.media.service.impl;

import com.histar.be.config.MinioProperties;
import com.histar.be.common.exception.BusinessRuleException;
import io.minio.*;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;

class CreationMediaDownloadTest {
    @Test void historicalHostStillReadsConfiguredBucketWithoutFetchingOldHost() throws Exception {
        var client = mock(MinioClient.class);
        var properties = new MinioProperties();
        properties.setBucket("histar");
        properties.setPublicUrl("https://new.example");
        var response = mock(GetObjectResponse.class);
        when(response.readAllBytes()).thenReturn(new byte[]{1,2,3});
        when(client.getObject(any(GetObjectArgs.class))).thenReturn(response);
        var service = new MinioMediaStorageService(client, properties);
        assertThat(service.downloadImage("https://old.example/histar/creations/user/photo.png")).containsExactly(1,2,3);
        verify(client).getObject(argThat(args -> args.bucket().equals("histar") && args.object().equals("creations/user/photo.png")));
    }
    @Test void rejectsForeignBucketTraversalAndNonCreationPaths() {
        var client = mock(MinioClient.class);
        var properties = new MinioProperties(); properties.setBucket("histar"); properties.setPublicUrl("https://new.example");
        var service = new MinioMediaStorageService(client, properties);
        for (String url : new String[]{"https://old.example/other/creations/x.png", "https://old.example/histar/creations/../secret", "https://old.example/histar/creations/%2e%2e/secret", "https://old.example/histar/avatar.png", "file:///histar/creations/x.png"}) {
            assertThatThrownBy(() -> service.downloadImage(url)).isInstanceOf(BusinessRuleException.class);
        }
        verifyNoInteractions(client);
    }
}
