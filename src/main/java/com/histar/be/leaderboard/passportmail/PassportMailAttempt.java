package com.histar.be.leaderboard.passportmail;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name="passport_mail_attempts", uniqueConstraints=@UniqueConstraint(columnNames={"user_id","request_id"}))
public class PassportMailAttempt {
    @Id private UUID id;
    @Column(name="user_id",nullable=false) private UUID userId;
    @Column(name="request_id",nullable=false) private UUID requestId;
    @Column(name="image_sha256",nullable=false,length=64) private String imageSha256;
    @Column(name="created_at",nullable=false) private Instant createdAt;
    @Column(nullable=false,length=16) private String state;
    protected PassportMailAttempt() {}
    static PassportMailAttempt created(UUID id,UUID userId,UUID requestId,String hash,Instant now) {
        var attempt=new PassportMailAttempt();attempt.id=id;attempt.userId=userId;attempt.requestId=requestId;
        attempt.imageSha256=hash;attempt.createdAt=now;attempt.state="CREATED";return attempt;
    }
    public UUID getId(){return id;}
    public Instant getCreatedAt(){return createdAt;}
    public String getState(){return state;}
    public String getImageSha256(){return imageSha256;}
    void finish(String state){this.state=state;}
}
