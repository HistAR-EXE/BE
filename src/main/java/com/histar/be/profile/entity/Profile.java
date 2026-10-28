package com.histar.be.profile.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@org.hibernate.annotations.DynamicUpdate
@Table(name = "profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Profile {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(unique = true, nullable = false)
    private String email;

    private String passwordHash;
    private String provider;
    private String role;
    private String tier;
    private String displayName;

    @Column(columnDefinition = "text")
    private String avatarUrl;

    private Integer level;
    private Integer totalPoints;
    private String city;
    @Column(length = 255)
    private String fullName;
    private java.time.LocalDate dateOfBirth;
    @Column(length = 255)
    private String gender;
    @Column(length = 255)
    private String phone;
    @Column(length = 1000)
    private String bio;
    @Column(length = 255)
    private String school;
    @Column(length = 255)
    private String className;
    @Column(length = 255)
    private String specialty;


    @Column(name = "org_id")
    private UUID orgId;

    @Column(name = "org_subscription")
    private String orgSubscription;

    @Column(name = "email_verified", nullable = false)
    @Builder.Default
    private Boolean emailVerified = false;

    @Column(name = "email_verified_at")
    private Instant emailVerifiedAt;

    @Column(name = "firebase_uid", length = 128)
    private String firebaseUid;

    @Column(name = "account_status", nullable = false, length = 20)
    @Builder.Default
    private String accountStatus = "ACTIVE";

    private Instant createdAt;
}
