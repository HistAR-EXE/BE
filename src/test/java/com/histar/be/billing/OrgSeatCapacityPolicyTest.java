package com.histar.be.billing;
import com.histar.be.billing.policy.OrgSeatCapacityPolicy;
import com.histar.be.organization.entity.OrgSubscription;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
class OrgSeatCapacityPolicyTest {
 @Test void lowerPlanRequiresExistingRosterToFitIncludingTeachersAndOwners(){
  assertThatThrownBy(()->OrgSeatCapacityPolicy.requireFits(101,OrgSubscription.MICRO)).hasMessageContaining("101").hasMessageContaining("100");
  assertThatCode(()->OrgSeatCapacityPolicy.requireFits(100,OrgSubscription.MICRO)).doesNotThrowAnyException();
  assertThatCode(()->OrgSeatCapacityPolicy.requireFits(400,OrgSubscription.STANDARD)).doesNotThrowAnyException();
  assertThatThrownBy(()->OrgSeatCapacityPolicy.requireFits(401,OrgSubscription.STANDARD)).isInstanceOf(RuntimeException.class);
 }
}
