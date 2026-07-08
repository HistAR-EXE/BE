package com.histar.be.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "histar")
@Getter
@Setter
public class HistarOrgProperties {

    private Org org = new Org();
    private Group group = new Group();
    private Tier tier = new Tier();

    @Getter
    @Setter
    public static class Org {
        private int inviteTtlDays = 7;
    }

    @Getter
    @Setter
    public static class Group {
        private int codeTtlDays = 30;
        private int maxGroupsPerUser = 20;
    }

    @Getter
    @Setter
    public static class Tier {
        private int freeChatDailyLimit = 10;
        private String freePhotoFrameIds = "classic,heritage";
    }
}
