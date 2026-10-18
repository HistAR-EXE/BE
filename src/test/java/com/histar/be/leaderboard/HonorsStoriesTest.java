package com.histar.be.leaderboard;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.histar.be.leaderboard.repository.HonorsStoriesRepository;
import com.histar.be.leaderboard.service.HonorsStoriesService;
import com.histar.be.leaderboard.service.impl.LeaderboardServiceImpl;
import com.histar.be.profile.entity.Profile;
import com.histar.be.profile.repository.ProfileRepository;
import com.histar.be.group.repository.StudyGroupMemberRepository;
import com.histar.be.config.*;
import com.histar.be.leaderboard.repository.LeaderboardReadRepository;
import com.histar.be.discovery.explore.ExploreChallengeCatalog;
import java.util.*;
import java.sql.Timestamp;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

class HonorsStoriesTest {
 JdbcTemplate jdbc; HonorsStoriesRepository repo; HonorsStoriesService service; UUID viewer = UUID.randomUUID();
 ProfileRepository profiles; StudyGroupMemberRepository members;
 @BeforeEach void setup() {
  var source = new DriverManagerDataSource("jdbc:h2:mem:"+UUID.randomUUID()+";MODE=PostgreSQL;DB_CLOSE_DELAY=-1", "sa", "");
  jdbc = new JdbcTemplate(source);
  jdbc.execute("CREATE TABLE profiles(id UUID PRIMARY KEY, display_name VARCHAR, avatar_url VARCHAR, email VARCHAR, total_points INT, role VARCHAR, city VARCHAR, org_id UUID, password_hash VARCHAR, account_status VARCHAR DEFAULT 'ACTIVE')");
  jdbc.execute("CREATE TABLE user_badges(user_id UUID,badge_id UUID,earned_at TIMESTAMP WITH TIME ZONE)");
  jdbc.execute("CREATE TABLE badges(id UUID,name VARCHAR,description VARCHAR)");
  jdbc.execute("CREATE TABLE user_discoveries(user_id UUID,location_id UUID,discovery_key VARCHAR,discovered_at TIMESTAMP WITH TIME ZONE)");
  jdbc.execute("CREATE TABLE locations(id UUID)");
  jdbc.execute("CREATE TABLE study_group_members(user_id UUID,group_id UUID)");
  jdbc.update("INSERT INTO locations VALUES(?)", ExploreChallengeCatalog.LOCATION);
  repo = new HonorsStoriesRepository(new NamedParameterJdbcTemplate(source));
  profiles=mock(ProfileRepository.class); members=mock(StudyGroupMemberRepository.class);
  when(profiles.findById(viewer)).thenReturn(Optional.of(Profile.builder().id(viewer).totalPoints(300).build()));
  var access = new LeaderboardServiceImpl(profiles,members,new ViralProperties(),new GamificationProperties(),mock(LeaderboardReadRepository.class));
  service=new HonorsStoriesService(access,repo,new GamificationProperties());
  profile(viewer,"USER",300);
 }
 void profile(UUID id,String role,int points) {jdbc.update("INSERT INTO profiles(id,display_name,email,total_points,role,city) VALUES(?, 'Explorer', ?,?,?, 'TP.HCM')",id,id+"@test.invalid",points,role);}
 void discovery(UUID user,String key,String at) {jdbc.update("INSERT INTO user_discoveries VALUES(?,?,?,?)",user,ExploreChallengeCatalog.LOCATION,key,at==null?null:Timestamp.from(java.time.Instant.parse(at)));}
 @Test void personalTimelineKeepsRealDatesAndUnknownXpDateWithoutLeakingOthers() {
  discovery(viewer,"cipher:hoang-cam","2025-01-01T00:00:00Z");
  UUID other=UUID.randomUUID(); discovery(other,"cipher:sandals","2026-01-01T00:00:00Z");
  UUID badge=UUID.randomUUID(); jdbc.update("INSERT INTO badges VALUES(?, 'Người khám phá','Đã khám phá')",badge);
  jdbc.update("INSERT INTO user_badges VALUES(?,?,?)",viewer,badge,Timestamp.from(java.time.Instant.parse("2025-02-01T00:00:00Z")));
  var result=service.read("all",null,viewer,null);
  assertThat(result.timeline()).hasSize(4);
  assertThat(result.timeline().get(0).occurredAt()).isEqualTo(java.time.Instant.parse("2025-02-01T00:00:00Z"));
  assertThat(result.timeline().get(1).occurredAt()).isEqualTo(java.time.Instant.parse("2025-01-01T00:00:00Z"));
  assertThat(result.timeline().stream().filter(t->t.icon().equals("auto_awesome"))).allSatisfy(t->assertThat(t.occurredAt()).isNull());
  assertThat(service.read("all",null,viewer,null).timeline()).isEqualTo(result.timeline());
 }
 @Test void pathsUseExactCatalogAndFirstUnfinishedTargetAndFullCompletion() {
  discovery(viewer,"depth_quiz:surface",null); discovery(viewer,"depth_quiz:layer1",null);
  for(String key:ExploreChallengeCatalog.QUIZZES.keySet()) if(key.startsWith("cipher:")) discovery(viewer,key,null);
  discovery(viewer,"depth_quiz:bogus",null);
  var paths=service.read("all",null,viewer,null).paths();
  assertThat(paths).extracting(p->p.total()).containsExactly(4,3,5,6);
  assertThat(paths.get(0).href()).isEqualTo("/explore?honorsPath=depths&target=layer2");
  assertThat(paths.get(0).percent()).isEqualTo(50);
  assertThat(paths.get(1).complete()).isTrue(); assertThat(paths.get(1).href()).isEqualTo("/explore?honorsPath=ciphers");
 }
 @Test void spotlightsBoundLeadersShareRanksAndFilterScopeAndSyntheticAccounts() {
  List<UUID> others=new ArrayList<>();
  for(int i=0;i<4;i++){UUID id=UUID.randomUUID();others.add(id);profile(id,"USER",1);discovery(id,"cipher:hoang-cam",null);discovery(id,"cipher:two-wells",null);}
  discovery(viewer,"cipher:hoang-cam",null);
  UUID admin=UUID.randomUUID();profile(admin,"ADMIN",999);discovery(admin,"cipher:hoang-cam",null);
  var spot=service.read("all",null,viewer,null).spotlights().get(0);
  assertThat(spot.leaders()).hasSize(3);assertThat(spot.totalAchievers()).isEqualTo(5);
  assertThat(spot.viewerRank()).isEqualTo(5);assertThat(spot.viewerCount()).isEqualTo(1);
  assertThat(spot.leaders()).allSatisfy(l->assertThat(l.count()).isEqualTo(2));
  jdbc.update("UPDATE profiles SET city='Huế' WHERE id=?",viewer);
  var local=service.read("city","Huế",viewer,null).spotlights().get(0);
  assertThat(local.totalAchievers()).isEqualTo(1);assertThat(local.viewerRank()).isEqualTo(1);
  assertThat(service.read("city","x' OR 1=1 --",viewer,null).spotlights().get(0).totalAchievers()).isZero();
 }
 @Test void anonymousForeignGroupAndInvalidScopeNeverReadStories() {
  assertThatThrownBy(()->service.read("all",null,null,null)).hasMessageContaining("đăng nhập");
  assertThatThrownBy(()->service.read("all",null,viewer,UUID.randomUUID())).hasMessageContaining("thành viên");
  assertThatThrownBy(()->service.read("week",null,viewer,null)).hasMessageContaining("toàn hành trình");
  assertThatThrownBy(()->service.read("school",null,viewer,null)).hasMessageContaining("trường");
 }
 @Test void readsAreBoundedAndNeverChangeEvidence() {
  for(int i=0;i<150;i++)discovery(viewer,"unknown:"+i,"2025-01-01T00:00:00Z");
  var before=jdbc.queryForList("SELECT * FROM profiles");
  assertThat(service.read("all",null,viewer,null).timeline().size()).isLessThanOrEqualTo(60);
  assertThat(jdbc.queryForList("SELECT * FROM profiles")).isEqualTo(before);
  assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM user_discoveries",Integer.class)).isEqualTo(150);
 }
 @Test void groupAndSchoolScopesUseOnlyAuthorizedPeers() {
  UUID org=UUID.randomUUID(),group=UUID.randomUUID(),other=UUID.randomUUID();profile(other,"USER",2);
  when(profiles.findById(viewer)).thenReturn(Optional.of(Profile.builder().id(viewer).orgId(org).totalPoints(300).build()));
  when(members.findByGroupIdAndUserId(group,viewer)).thenReturn(Optional.of(new com.histar.be.group.entity.StudyGroupMember()));
  jdbc.update("UPDATE profiles SET org_id=? WHERE id=?",org,viewer);
  jdbc.update("INSERT INTO study_group_members VALUES(?,?)",viewer,group);
  discovery(viewer,"cipher:hoang-cam",null); discovery(other,"cipher:hoang-cam",null);
  assertThat(service.read("all",null,viewer,group).spotlights().get(0).totalAchievers()).isEqualTo(1);
  assertThat(service.read("school",null,viewer,null).spotlights().get(0).totalAchievers()).isEqualTo(1);
 }
 @Test void allFourCompleteCatalogSetsCountOnceAndForeignOrUnknownChallengesCannotCompletePaths() {
  for(String key:HonorsStoriesRepository.catalogKeys())discovery(viewer,key,null);
  var result=service.read("all",null,viewer,null);
  assertThat(result.paths()).allSatisfy(p->{assertThat(p.complete()).isTrue();assertThat(p.percent()).isEqualTo(100);assertThat(p.actionLabel()).isEqualTo("Đã hoàn thành");});
  assertThat(result.spotlights().get(2).viewerCount()).isEqualTo(4);
  jdbc.update("DELETE FROM user_discoveries WHERE discovery_key=?",ExploreChallengeCatalog.CONCLUSION);
  assertThat(service.read("all",null,viewer,null).paths().get(2).href()).endsWith("target=conclusion");
  assertThat(service.read("all",null,viewer,null).spotlights().get(2).viewerCount()).isEqualTo(3);
  jdbc.update("UPDATE user_discoveries SET location_id=?",UUID.randomUUID());
  var foreign=service.read("all",null,viewer,null);
  assertThat(foreign.paths()).allSatisfy(p->assertThat(p.completed()).isZero());
  assertThat(foreign.spotlights().get(1).viewerCount()).isZero();
 }
 @Test void realCompetitionRankSkipsTiedPositionsAndExcludesSeedCredentials() {
  UUID a=UUID.randomUUID(),b=UUID.randomUUID(),seed=UUID.randomUUID();
  for(UUID id:List.of(a,b,seed)){profile(id,"USER",1);discovery(id,"cipher:hoang-cam",null);discovery(id,"cipher:two-wells",null);}
  discovery(viewer,"cipher:hoang-cam",null);
  jdbc.update("UPDATE profiles SET email='user1@histar.vn',password_hash=? WHERE id=?","$2a$10$uQfV2m4K1S8mJjW9n8u9fezQ3L4kE0cM9Qz5bQ2QByC2eQ6HgG5Qe",seed);
  var spot=service.read("all",null,viewer,null).spotlights().get(0);
  assertThat(spot.totalAchievers()).isEqualTo(3);assertThat(spot.viewerRank()).isEqualTo(3);
  assertThat(repo.metrics(viewer,null,null,null).stream().filter(m->m.category().equals("ciphers")).map(m->m.rank())).containsExactly(1,1,3);
 }
 @Test void storiesControllerIsAuthenticatedPrivateAndIndependentFromBoard() throws Exception {
  var current=mock(com.histar.be.common.security.CurrentUserAccessor.class);
  var board=mock(com.histar.be.leaderboard.service.LeaderboardService.class);
  var controller=new com.histar.be.leaderboard.controller.LeaderboardController(board,current,service,mock(com.histar.be.leaderboard.service.HonorsFactsService.class));
  var mvc=org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup(controller)
   .setControllerAdvice(new com.histar.be.common.exception.GlobalExceptionHandler()).build();
  when(current.getUserId()).thenReturn(Optional.empty());
  mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/leaderboard/stories"))
   .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isUnauthorized());
  when(current.getUserId()).thenReturn(Optional.of(viewer));
  mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/leaderboard/stories"))
   .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
   .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header().string("Cache-Control","private, no-store"))
   .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.data.userId").value(viewer.toString()));
  verifyNoInteractions(board);
 }
 @Test void serviceDeclaresReadOnlyRepeatableReadAndUsesNoPersistenceWrites() throws Exception {
  var tx=HonorsStoriesService.class.getMethod("read",String.class,String.class,UUID.class,UUID.class)
   .getAnnotation(org.springframework.transaction.annotation.Transactional.class);
  assertThat(tx.readOnly()).isTrue();assertThat(tx.isolation()).isEqualTo(org.springframework.transaction.annotation.Isolation.REPEATABLE_READ);
  service.read("all",null,viewer,null);verify(profiles,never()).save(org.mockito.ArgumentMatchers.any());
 }
 @Test void completedPathHasLatestRealEvidenceDateAndNeverInventsMissingDates() {
  discovery(viewer,"cipher:hoang-cam","2025-01-01T00:00:00Z");
  discovery(viewer,"cipher:two-wells","2025-01-02T00:00:00Z");
  discovery(viewer,"cipher:sandals","2025-02-01T00:00:00Z");
  assertThat(service.read("all",null,viewer,null).timeline()).anySatisfy(t->{
   assertThat(t.id()).isEqualTo("path:ciphers");assertThat(t.occurredAt()).isEqualTo(java.time.Instant.parse("2025-02-01T00:00:00Z"));
  });
  jdbc.update("UPDATE user_discoveries SET discovered_at=NULL WHERE discovery_key='cipher:two-wells'");
  assertThat(service.read("all",null,viewer,null).timeline()).anySatisfy(t->{
   assertThat(t.id()).isEqualTo("path:ciphers");assertThat(t.occurredAt()).isNull();
  });
 }
}

