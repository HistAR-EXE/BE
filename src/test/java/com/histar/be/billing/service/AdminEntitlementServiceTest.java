package com.histar.be.billing.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.histar.be.billing.entity.B2cSubscription;
import com.histar.be.billing.repository.B2cSubscriptionRepository;
import com.histar.be.profile.entity.Profile;
import com.histar.be.profile.entity.UserTier;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class AdminEntitlementServiceTest {
    private final B2cSubscriptionRepository subscriptions = mock(B2cSubscriptionRepository.class);
    private final AdminEntitlementService service = new AdminEntitlementService(subscriptions);

    @Test
    void premiumGrantCreatesAZeroRevenueExpiringSubscription() {
        Profile user = Profile.builder().id(UUID.randomUUID()).tier("FREE").build();
        service.changeTier(user, UserTier.PREMIUM);

        ArgumentCaptor<B2cSubscription> saved = ArgumentCaptor.forClass(B2cSubscription.class);
        verify(subscriptions).save(saved.capture());
        assertThat(saved.getValue().getPaymentMethod()).isEqualTo("ADMIN_GRANT");
        assertThat(saved.getValue().getPriceVnd()).isZero();
        assertThat(saved.getValue().getEndDate()).isEqualTo(saved.getValue().getStartDate().plusMonths(1));
        assertThat(user.getTier()).isEqualTo("PREMIUM");
    }

    @Test
    void downgradeRevokesTheActiveSubscription() {
        Profile user = Profile.builder().id(UUID.randomUUID()).tier("PREMIUM").build();
        B2cSubscription active = B2cSubscription.builder().isActive(true).build();
        when(subscriptions.findByUserIdAndIsActiveTrue(user.getId())).thenReturn(Optional.of(active));

        service.changeTier(user, UserTier.FREE);

        assertThat(active.getIsActive()).isFalse();
        verify(subscriptions).saveAndFlush(active);
        assertThat(user.getTier()).isEqualTo("FREE");
    }

    @Test
    void personalGrantCannotOverrideAnOrganizationPlan() {
        Profile user = Profile.builder().id(UUID.randomUUID()).orgId(UUID.randomUUID()).build();
        assertThatThrownBy(() -> service.changeTier(user, UserTier.PREMIUM))
                .isInstanceOf(com.histar.be.common.exception.BusinessRuleException.class);
        verifyNoInteractions(subscriptions);
    }
}
