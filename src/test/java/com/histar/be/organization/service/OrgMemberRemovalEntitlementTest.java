package com.histar.be.organization.service;

import com.histar.be.common.exception.BusinessRuleException;
import com.histar.be.organization.entity.*;
import com.histar.be.organization.repository.*;
import com.histar.be.profile.entity.Profile;
import com.histar.be.profile.repository.ProfileRepository;
import jakarta.persistence.EntityManager;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class OrgMemberRemovalEntitlementTest {
 final OrganizationRepository organizations=mock(OrganizationRepository.class);
 final OrganizationMemberRepository members=mock(OrganizationMemberRepository.class);
 final ProfileRepository profiles=mock(ProfileRepository.class);
 final EntityManager entityManager=mock(EntityManager.class);
 final OrganizationEntitlementService entitlements=new OrganizationEntitlementService(organizations,members,profiles);
 final OrgMembershipService service=new OrgMembershipService(organizations,members,profiles,entityManager,entitlements,mock(com.histar.be.billing.service.OrgPendingSeatLimitService.class));
 final UUID orgId=UUID.randomUUID(), managerId=UUID.randomUUID(), studentId=UUID.randomUUID();
 void manager(boolean verified,boolean active){
  when(profiles.findById(managerId)).thenReturn(Optional.of(Profile.builder().id(managerId).orgId(orgId).role("TEACHER").emailVerified(verified).build()));
  when(organizations.findById(orgId)).thenReturn(Optional.of(Organization.builder().id(orgId).planType("MICRO").status("ACTIVE")
   .planEndDate(LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh")).plusDays(active ? 30 : -1)).build()));
  when(members.findByOrganizationIdAndUserId(orgId,managerId)).thenReturn(Optional.of(OrganizationMember.builder()
   .organizationId(orgId).userId(managerId).orgRole("owner").acceptedAt(Instant.now()).build()));
 }
 @Test void expiredManagerCannotRemoveStudents(){
  manager(true,false);
  assertThatThrownBy(()->service.removeMember(managerId,studentId)).isInstanceOf(BusinessRuleException.class).hasMessageContaining("manageMembers");
  verify(profiles,never()).findByIdForUpdate(any());verify(members,never()).delete(any());
 }
 @Test void unverifiedManagerCannotRemoveStudents(){
  manager(false,true);
  assertThatThrownBy(()->service.removeMember(managerId,studentId)).isInstanceOf(BusinessRuleException.class).hasMessageContaining("manageMembers");
  verify(profiles,never()).findByIdForUpdate(any());verify(members,never()).delete(any());
 }
 @Test void authorizedMicroManagerCanRemoveStudentWithoutChangingPurchasedTier(){
  manager(true,true);
  var student=Profile.builder().id(studentId).orgId(orgId).role("ORG_MEMBER").tier("PREMIUM").build();
  var member=OrganizationMember.builder().organizationId(orgId).userId(studentId).orgRole("student").build();
  when(profiles.findByIdForUpdate(studentId)).thenReturn(Optional.of(student));
  var organization = organizations.findById(orgId);
  when(organizations.findByIdForUpdate(orgId)).thenReturn(organization);
  when(members.findByOrganizationIdAndUserId(orgId,studentId)).thenReturn(Optional.of(member));
  service.removeMember(managerId,studentId);
  verify(members).delete(member);assertThat(student.getOrgId()).isNull();assertThat(student.getTier()).isEqualTo("PREMIUM");
 }
}
