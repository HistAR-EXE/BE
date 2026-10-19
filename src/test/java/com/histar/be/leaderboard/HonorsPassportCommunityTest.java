package com.histar.be.leaderboard;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.histar.be.config.*;
import com.histar.be.leaderboard.repository.*;
import com.histar.be.leaderboard.service.*;
import com.histar.be.leaderboard.service.impl.LeaderboardServiceImpl;
import com.histar.be.profile.entity.Profile;
import com.histar.be.profile.repository.ProfileRepository;
import com.histar.be.group.repository.StudyGroupMemberRepository;
import com.histar.be.discovery.explore.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.core.*;
import org.springframework.jdbc.core.namedparam.*;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
class HonorsPassportCommunityTest {
 JdbcTemplate jdbc; HonorsFactsRepository repo; HonorsFactsService service;
 UUID viewer=UUID.randomUUID(),other=UUID.randomUUID(); ProfileRepository profiles; StudyGroupMemberRepository members;
 @BeforeEach void setup(){
  var ds=new DriverManagerDataSource("jdbc:h2:mem:"+UUID.randomUUID()+";MODE=PostgreSQL;DB_CLOSE_DELAY=-1","sa","");
  jdbc=new JdbcTemplate(ds);
  jdbc.execute("CREATE TABLE profiles(id UUID PRIMARY KEY,display_name VARCHAR,email VARCHAR,total_points INT,role VARCHAR,city VARCHAR,org_id UUID,password_hash VARCHAR,provider VARCHAR,account_status VARCHAR DEFAULT 'ACTIVE')");
  jdbc.execute("CREATE TABLE user_badges(user_id UUID,badge_id UUID,earned_at TIMESTAMP WITH TIME ZONE)");
  jdbc.execute("CREATE TABLE user_discoveries(user_id UUID,location_id UUID,discovery_key VARCHAR)");
  jdbc.execute("CREATE TABLE locations(id UUID)");jdbc.execute("CREATE TABLE study_group_members(user_id UUID,group_id UUID)");
  jdbc.update("INSERT INTO locations VALUES(?)",ExploreChallengeCatalog.LOCATION);
  profiles=mock(ProfileRepository.class);members=mock(StudyGroupMemberRepository.class);
  when(profiles.findById(viewer)).thenReturn(Optional.of(Profile.builder().id(viewer).displayName("Owner").totalPoints(350).build()));
  var access=new LeaderboardServiceImpl(profiles,members,new ViralProperties(),new GamificationProperties(),mock(LeaderboardReadRepository.class));
  repo=new HonorsFactsRepository(new NamedParameterJdbcTemplate(ds));service=new HonorsFactsService(access,repo,new GamificationProperties());
  user(viewer,350,"USER",null);user(other,0,"USER",null);
 }
 void user(UUID id,int xp,String role,String provider){jdbc.update("INSERT INTO profiles(id,display_name,email,total_points,role,provider,city) VALUES(?,'Explorer',?,?,?,?, 'Hue')",id,id+"@test.invalid",xp,role,provider);}
 void discovery(UUID id,UUID location,String key){jdbc.update("INSERT INTO user_discoveries VALUES(?,?,?)",id,location,key);}
 @Test void passportContainsOnlyPersistedOwnerFactsAndDistinctEarnedHeritageBadges(){
  UUID badge=ExploreBadgeRules.databaseId(0);
  jdbc.update("INSERT INTO user_badges VALUES(?,?,NULL)",viewer,badge);
  jdbc.update("INSERT INTO user_badges VALUES(?,?,NULL)",viewer,badge);
  jdbc.update("INSERT INTO user_badges VALUES(?,?,NULL)",viewer,UUID.randomUUID());
  jdbc.update("INSERT INTO user_badges VALUES(?,?,NULL)",other,ExploreBadgeRules.databaseId(1));
  var p=service.passport(viewer);
  assertThat(p.userId()).isEqualTo(viewer);assertThat(p.displayName()).isEqualTo("Owner");
  assertThat(p.totalPoints()).isEqualTo(350);assertThat(p.levelName()).isNotBlank();assertThat(p.badgeCount()).isEqualTo(2);
  assertThat(p.heritageBadges()).hasSize(1);assertThat(p.heritageBadges().get(0).id()).isEqualTo("badge_compass_pioneer");
  assertThat(p.heritageBadges().get(0).earnedAt()).isNull();
 }
 @Test void communityCountsDistinctEvidenceAndExcludesSyntheticAndAdmins(){
  for(String provider:List.of("ui-preview-honors","normal")){UUID id=UUID.randomUUID();user(id,999,"USER",provider);if(provider.equals("normal")) jdbc.update("UPDATE profiles SET role='ADMIN' WHERE id=?",id);discovery(id,ExploreChallengeCatalog.LOCATION,"cipher:hoang-cam");}
  UUID seed=UUID.fromString("a1000000-0000-4000-8000-000000000001");user(seed,999,"USER",null);jdbc.update("UPDATE profiles SET email='seed-analytics-001@histar.vn' WHERE id=?",seed);
  for(int i=0;i<2;i++)discovery(other,ExploreChallengeCatalog.LOCATION,"cipher:hoang-cam");
  discovery(other,ExploreChallengeCatalog.LOCATION,"unknown");discovery(other,UUID.randomUUID(),"cipher:sandals");
  UUID badge=UUID.randomUUID();for(int i=0;i<2;i++)jdbc.update("INSERT INTO user_badges VALUES(?,?,NULL)",other,badge);
  var c=service.community("all",null,viewer,null);
  assertThat(c.explorers()).isEqualTo(2);assertThat(c.completedChallenges()).isEqualTo(1);assertThat(c.discoveries()).isEqualTo(1);assertThat(c.badges()).isEqualTo(1);
  assertThat(service.community("city","missing",viewer,null).explorers()).isZero();
 }
 @Test void routesArePrivateNoStoreAndRejectAnonymousOrForeignGroups() throws Exception {
  var current=mock(com.histar.be.common.security.CurrentUserAccessor.class);
  var controller=new com.histar.be.leaderboard.controller.LeaderboardController(mock(LeaderboardService.class),current,mock(HonorsStoriesService.class),service);
  var mvc=org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup(controller).setControllerAdvice(new com.histar.be.common.exception.GlobalExceptionHandler()).build();
  when(current.getUserId()).thenReturn(Optional.empty());
  for(String route:List.of("passport","community"))mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/leaderboard/"+route)).andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isUnauthorized());
  when(current.getUserId()).thenReturn(Optional.of(viewer));
  for(String route:List.of("passport","community"))mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/leaderboard/"+route)).andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk()).andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header().string("Cache-Control","private, no-store"));
  mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/leaderboard/passport").param("userId",other.toString()))
   .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.data.userId").value(viewer.toString()))
   .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.data.avatarUrl").doesNotExist())
   .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.data.email").doesNotExist());
  mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/leaderboard/community").param("groupId",UUID.randomUUID().toString()))
   .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isForbidden());
  assertThatThrownBy(()->service.community("all",null,viewer,UUID.randomUUID())).hasMessageContaining("thành viên");
 }
 @Test void groupCountsNeverIncludeOutsidersAndReadsDoNotWrite(){
  UUID group=UUID.randomUUID();when(members.findByGroupIdAndUserId(group,viewer)).thenReturn(Optional.of(new com.histar.be.group.entity.StudyGroupMember()));
  jdbc.update("INSERT INTO study_group_members VALUES(?,?)",viewer,group);discovery(other,ExploreChallengeCatalog.LOCATION,"cipher:hoang-cam");
  var before=jdbc.queryForList("SELECT * FROM profiles");var c=service.community("all",null,viewer,group);
  assertThat(c.explorers()).isEqualTo(1);assertThat(c.completedChallenges()).isZero();assertThat(c.discoveries()).isZero();
  service.passport(viewer);assertThat(jdbc.queryForList("SELECT * FROM profiles")).isEqualTo(before);verify(profiles,never()).save(org.mockito.ArgumentMatchers.any());
 }
 @Test void zeroXpWithoutEvidenceIsExcludedButBadgeOnlyParticipantCounts(){
  var before=service.community("all",null,viewer,null);assertThat(before.explorers()).isEqualTo(1);
  jdbc.update("INSERT INTO user_badges VALUES(?,?,NULL)",other,UUID.randomUUID());
  var after=service.community("all",null,viewer,null);assertThat(after.explorers()).isEqualTo(2);assertThat(after.badges()).isEqualTo(1);
 }
 @Test void legacySeedsAreExcludedOnlyWhenBothEmailAndCredentialMatch(){
  UUID seed=UUID.randomUUID(),real=UUID.randomUUID();for(UUID id:List.of(seed,real))user(id,999,"USER",null);
  jdbc.update("UPDATE profiles SET email='user1@histar.vn',password_hash=? WHERE id=?","$2a$10$uQfV2m4K1S8mJjW9n8u9fezQ3L4kE0cM9Qz5bQ2QByC2eQ6HgG5Qe",seed);
  jdbc.update("UPDATE profiles SET email='user2@histar.vn' WHERE id=?",real);
  assertThat(service.community("all",null,viewer,null).explorers()).isEqualTo(2);
 }
 @Test void factsMethodsDeclareReadOnlyRepeatableRead() throws Exception {
  for(var method:HonorsFactsService.class.getDeclaredMethods()){
   var tx=method.getAnnotation(org.springframework.transaction.annotation.Transactional.class);
   assertThat(tx).isNotNull();assertThat(tx.readOnly()).isTrue();assertThat(tx.isolation()).isEqualTo(org.springframework.transaction.annotation.Isolation.REPEATABLE_READ);
  }
 }
}
