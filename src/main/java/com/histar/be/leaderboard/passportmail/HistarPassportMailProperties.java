package com.histar.be.leaderboard.passportmail;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import lombok.Getter;
import lombok.Setter;

@Component
@ConfigurationProperties(prefix = "histar.passport-mail")
@Getter @Setter
public class HistarPassportMailProperties {
    @org.springframework.beans.factory.annotation.Value("${HISTAR_PASSPORT_MAIL_ENABLED:true}")
    private boolean enabled = true;
    @org.springframework.beans.factory.annotation.Value("${HISTAR_PASSPORT_MAIL_FROM:TimeLens <histar.timelens@gmail.com>}")
    private String from = "TimeLens <histar.timelens@gmail.com>";
}

