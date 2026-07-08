package com.histar.be.auth.service;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import com.histar.be.common.exception.BusinessRuleException;
import com.histar.be.config.HistarFirebaseProperties;
import com.histar.be.config.TestHookProperties;
import jakarta.annotation.PostConstruct;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class FirebaseAuthService {

    /** Must match FE/tests/fixtures/contracts/google-test-id-token.json when test hooks enabled. */
    public static final String TEST_HOOK_GOOGLE_ID_TOKEN = "histar-test-hook-google-id-token-fixture";

    private final HistarFirebaseProperties firebaseProperties;
    private final TestHookProperties testHookProperties;
    private FirebaseAuth firebaseAuth;

    @PostConstruct
    void init() {
        if (!firebaseProperties.isEnabled()) {
            log.info("Firebase auth disabled (histar.firebase.enabled=false)");
            return;
        }
        String raw = firebaseProperties.getServiceAccountJson();
        if (raw == null || raw.isBlank()) {
            log.warn("Firebase enabled but FIREBASE_SERVICE_ACCOUNT_JSON is empty — Google login unavailable");
            return;
        }
        try {
            byte[] jsonBytes = decodeServiceAccount(raw.trim());
            GoogleCredentials credentials =
                    GoogleCredentials.fromStream(new ByteArrayInputStream(jsonBytes));
            FirebaseOptions options =
                    FirebaseOptions.builder().setCredentials(credentials).build();
            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options);
            }
            firebaseAuth = FirebaseAuth.getInstance();
            log.info("Firebase Admin SDK initialized for Google login");
        } catch (Exception ex) {
            log.error("Failed to initialize Firebase Admin SDK: {}", ex.getMessage());
        }
    }

    public boolean isReady() {
        return firebaseAuth != null;
    }

    public VerifiedGoogleUser verifyIdToken(String idToken) {
        if (testHookProperties.isEnabled() && TEST_HOOK_GOOGLE_ID_TOKEN.equals(idToken)) {
            return new VerifiedGoogleUser(
                    "histar-test-hook-firebase-uid",
                    "google-hook-test@histar.vn",
                    "Google Hook Test User",
                    null);
        }
        if (firebaseAuth == null) {
            throw new BusinessRuleException(
                    "Đăng nhập Google chưa được cấu hình trên máy chủ. Liên hệ quản trị viên.");
        }
        try {
            FirebaseToken token = firebaseAuth.verifyIdToken(idToken);
            String email = token.getEmail();
            if (email == null || email.isBlank()) {
                throw new BusinessRuleException("Tài khoản Google không có email hợp lệ");
            }
            return new VerifiedGoogleUser(
                    token.getUid(),
                    email,
                    token.getName(),
                    token.getPicture());
        } catch (FirebaseAuthException ex) {
            throw new BusinessRuleException("Token Google không hợp lệ hoặc đã hết hạn");
        }
    }

    private static byte[] decodeServiceAccount(String raw) {
        if (raw.startsWith("{")) {
            return raw.getBytes(StandardCharsets.UTF_8);
        }
        return Base64.getDecoder().decode(raw);
    }

    public record VerifiedGoogleUser(String uid, String email, String name, String picture) {}
}
