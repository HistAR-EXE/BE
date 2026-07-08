package com.histar.be.auth.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.histar.be.common.exception.BusinessRuleException;
import com.histar.be.config.HistarFirebaseProperties;
import com.histar.be.config.TestHookProperties;
import org.junit.jupiter.api.Test;

class FirebaseAuthServiceTest {

    @Test
    void verifyIdToken_throwsWhenFirebaseNotInitialized() {
        HistarFirebaseProperties props = new HistarFirebaseProperties();
        props.setEnabled(true);
        props.setServiceAccountJson("");
        TestHookProperties hooks = new TestHookProperties();
        hooks.setEnabled(false);
        FirebaseAuthService service = new FirebaseAuthService(props, hooks);

        assertThatThrownBy(() -> service.verifyIdToken("any-token"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("chưa được cấu hình");
    }

    @Test
    void verifyIdToken_returnsFixtureUserWhenTestHookEnabled() {
        HistarFirebaseProperties props = new HistarFirebaseProperties();
        props.setEnabled(false);
        TestHookProperties hooks = new TestHookProperties();
        hooks.setEnabled(true);
        FirebaseAuthService service = new FirebaseAuthService(props, hooks);

        var user = service.verifyIdToken(FirebaseAuthService.TEST_HOOK_GOOGLE_ID_TOKEN);

        assertThat(user.email()).isEqualTo("google-hook-test@histar.vn");
        assertThat(user.uid()).isEqualTo("histar-test-hook-firebase-uid");
    }
}
