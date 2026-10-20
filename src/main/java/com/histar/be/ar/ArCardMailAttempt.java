package com.histar.be.ar;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name="ar_card_mail_attempts", uniqueConstraints=@UniqueConstraint(columnNames={"user_id","request_id"}))
public class ArCardMailAttempt {
    @Id private UUID id;
    @Column(name="user_id",nullable=false) private UUID userId;
    @Column(name="request_id",nullable=false) private UUID requestId;
    @Column(name="created_at",nullable=false) private Instant createdAt;
    @Column(nullable=false,length=16) private String state;
    @Column(name="artifact_id",length=32) private String artifactId;
    protected ArCardMailAttempt() {}
    static ArCardMailAttempt created(UUID userId, UUID requestId, String artifactId, Instant now) {
        var value=new ArCardMailAttempt();value.id=UUID.randomUUID();value.userId=userId;
        value.requestId=requestId;value.artifactId=artifactId;value.createdAt=now;value.state="CREATED";return value;
    }
    public UUID getId() { return id; }
    public Instant getCreatedAt() { return createdAt; }
    public String getArtifactId() { return artifactId; }
    public String getState() { return state; }
    void finish(String state) { this.state=state; }
}
