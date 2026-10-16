package com.histar.be.admin;

import static org.assertj.core.api.Assertions.assertThat;

import com.histar.be.admin.dto.AdminArtifactRequest;
import com.histar.be.admin.dto.AdminDiscoveryPointRequest;
import com.histar.be.admin.dto.AdminQuestRequest;
import com.histar.be.artifact.entity.Artifact;
import com.histar.be.discovery.entity.DiscoveryPoint;
import com.histar.be.quest.entity.Quest;
import jakarta.persistence.Version;
import java.lang.reflect.Field;
import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

class AdminContentEntityContractTest {

    @Test
    void contentEntitiesMapSoftDeleteAndOptimisticLockFields() throws Exception {
        for (Class<?> entityType : new Class<?>[] {DiscoveryPoint.class, Artifact.class, Quest.class}) {
            assertThat(entityType.getDeclaredField("deletedAt")).isNotNull();
            assertThat(entityType.getDeclaredField("deletedBy")).isNotNull();
            assertThat(entityType.getDeclaredField("deleteReason")).isNotNull();
            Field version = entityType.getDeclaredField("rowVersion");
            assertThat(version.getAnnotation(Version.class)).isNotNull();
        }
    }

    @Test
    void adminUpdateRequestsCarryRowVersion() {
        for (Class<?> requestType :
                new Class<?>[] {AdminDiscoveryPointRequest.class, AdminArtifactRequest.class, AdminQuestRequest.class}) {
            assertThat(Arrays.stream(requestType.getRecordComponents())
                            .map(RecordComponent::getName))
                    .contains("rowVersion");
        }
    }
}
