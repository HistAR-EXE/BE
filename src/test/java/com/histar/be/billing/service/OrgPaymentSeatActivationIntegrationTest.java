package com.histar.be.billing.service;

import com.histar.be.billing.dto.*;
import com.histar.be.billing.entity.OrgPaymentTransaction;
import com.histar.be.billing.repository.*;
import com.histar.be.common.exception.BusinessRuleException;
import com.histar.be.organization.entity.*;
import com.histar.be.organization.repository.*;
import com.histar.be.organization.service.OrgMembershipService;
import com.histar.be.profile.entity.Profile;
import com.histar.be.profile.repository.ProfileRepository;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:org_seat_payment;MODE=PostgreSQL;DB_CLOSE_DELAY=-1", "payment.sepay.enabled=true","payment.sepay.bank-code=TEST","payment.sepay.account-number=123","payment.sepay.account-name=Fixture"})
@ActiveProfiles("test")
@org.springframework.test.annotation.DirtiesContext(classMode=org.springframework.test.annotation.DirtiesContext.ClassMode.AFTER_CLASS)
class OrgPaymentSeatActivationIntegrationTest {
 @Autowired OrganizationRepository organizations;
 @Autowired OrganizationMemberRepository members;
 @Autowired ProfileRepository profiles;
 @Autowired OrgPaymentTransactionRepository payments;
 @Autowired OrgBillingSubscriptionRepository subscriptions;
 @Autowired SepayOrgPaymentService sepay;
 @Autowired OrgMembershipService memberships;
 UUID owner,org;
 Profile profile(){return profiles.save(Profile.builder().email(UUID.randomUUID()+"@seat.test").role("USER").tier("FREE").emailVerified(true).displayName("Seat fixture").totalPoints(0).level(1).build());}
 @BeforeEach void fixture(){
  var p=profile();owner=p.getId();var o=organizations.save(Organization.builder().name("Seat test").planType("STANDARD").status("ACTIVE").maxVerifiedAccounts(400).maxCcu(40).maxAiQueriesPerMonth(30000).planEndDate(LocalDate.now().plusMonths(1)).build());org=o.getId();
  p.setOrgId(org);p.setRole("TEACHER");profiles.save(p);member(owner,"owner");
 }
 void member(UUID user,String role){members.save(OrganizationMember.builder().organizationId(org).userId(user).orgRole(role).invitedAt(Instant.now()).acceptedAt(Instant.now()).build());}
 void fill(int total){while(members.countByOrganizationId(org)<total)member(profile().getId(),"student");}
 OrgPaymentTransaction pending(){return payments.save(OrgPaymentTransaction.builder().requesterUserId(owner).organizationId(org).orgName("Seat test").contactEmail("owner@seat.test").planType("MICRO").provider("SEPAY").orderCode("SEAT"+UUID.randomUUID()).transferContent("seat fixture").amountVnd(8000000L).licenseCount(1).grossAmountVnd(8000000L).discountAmountVnd(0L).status("PENDING").expiresAt(Instant.now().plusSeconds(600)).createdAt(Instant.now()).updatedAt(Instant.now()).build());}
 @Test void downgradePaymentIsBlockedBeforeCollectingMoneyWhenRosterExceedsNewLimit(){
  fill(101);
  assertThatThrownBy(()->sepay.createPayment(owner,new OrgCreatePaymentRequest("Seat test","MICRO","owner@seat.test",org,null,1))).isInstanceOf(BusinessRuleException.class).hasMessageContaining("100");
  assertThat(payments.findAll().stream().filter(p->owner.equals(p.getRequesterUserId()))).isEmpty();
 }
 @Test void pendingLowerPlanPreventsNewEnrollmentBeyondTheReservedCap(){
  fill(100);pending();var newcomer=profile();
  assertThatThrownBy(()->memberships.enrollUserInOrganization(newcomer.getId(),org)).isInstanceOf(BusinessRuleException.class).hasMessageContaining("100");
 }
 @Test void paidReceiptSurvivesCapacityFailureAndOwnerCanActivateAfterRemovingOverage(){
  fill(101);var tx=pending();
  var receipt=new SepayWebhookPayload(987654321L,"fixture",null,null,null,tx.getOrderCode(),tx.getTransferContent(),"in",null,8000000L,null,"receipt");
  sepay.handleVerifiedWebhook(receipt);
  var paid=payments.findById(tx.getId()).orElseThrow();
  assertThat(paid.getStatus()).isEqualTo("PAID");assertThat(paid.getProviderTransactionId()).isEqualTo(987654321L);assertThat(paid.getSubscriptionId()).isNull();
  var waiting=sepay.getPaymentStatus(owner,tx.getOrderCode());
  assertThat(waiting.activated()).isFalse();assertThat(waiting.activationMessage()).contains("100");
  var removable=members.findByOrganizationId(org).stream().filter(m->!owner.equals(m.getUserId())).findFirst().orElseThrow();members.delete(removable);
  var activated=sepay.getPaymentStatus(owner,tx.getOrderCode());
  assertThat(activated.activated()).isTrue();assertThat(organizations.findById(org).orElseThrow().getPlanType()).isEqualTo("MICRO");
  var link=payments.findById(tx.getId()).orElseThrow().getSubscriptionId();assertThat(link).isNotNull();
  sepay.handleVerifiedWebhook(receipt);sepay.getPaymentStatus(owner,tx.getOrderCode());
  assertThat(subscriptions.findAllByOrganizationIdOrderByCreatedAtDesc(org)).hasSize(1);
 }
}
