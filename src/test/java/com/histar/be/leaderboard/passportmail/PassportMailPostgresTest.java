package com.histar.be.leaderboard.passportmail;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.histar.be.config.*;
import com.histar.be.mail.HistarEmailService;
import com.histar.be.profile.entity.Profile;
import com.histar.be.profile.repository.ProfileRepository;
import jakarta.persistence.*;
import java.util.*;
import java.time.Instant;
import java.util.concurrent.*;
import java.io.ByteArrayOutputStream;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.*;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.mock.web.MockMultipartFile;

/** Opt-in real PostgreSQL proof using a disposable random schema; never sends mail. */
@EnabledIfEnvironmentVariable(named="RUN_PASSPORT_POSTGRES_IT",matches="true")
class PassportMailPostgresTest {
 @Test void concurrentReservationsSerializeOnProfileAndLeaveXpUntouched() throws Exception {
  String schema="passport_mail_test_"+UUID.randomUUID().toString().replace("-","");
  var ds=new DriverManagerDataSource(System.getenv("PASSPORT_TEST_DB_URL"),System.getenv("PASSPORT_TEST_DB_USER"),System.getenv("PASSPORT_TEST_DB_PASSWORD"));
  var jdbc=new JdbcTemplate(ds);jdbc.execute("CREATE SCHEMA "+schema);
  var factory=new LocalContainerEntityManagerFactoryBean();factory.setDataSource(ds);
  factory.setPackagesToScan("com.histar.be.profile.entity","com.histar.be.leaderboard.passportmail");
  factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
  factory.setJpaPropertyMap(Map.of("hibernate.hbm2ddl.auto","create","hibernate.default_schema",schema));
  ExecutorService executor=Executors.newFixedThreadPool(2);
  try {
   factory.afterPropertiesSet();
   // Keep the Hibernate-created PK/unique-only table: V82 must reconcile this real local-upgrade case.
   String migration=java.nio.file.Files.readString(java.nio.file.Path.of("src/main/resources/db/migration/V82__passport_mail_attempts.sql"));
   String reconciled=migration.replace("passport_mail_attempts",schema+".passport_mail_attempts").replace("REFERENCES profiles", "REFERENCES "+schema+".profiles");
   jdbc.execute(reconciled);
   jdbc.execute(reconciled); // Reconciliation is idempotent, including named checks and generated unique constraints.
   assertEquals(3,jdbc.queryForObject("SELECT count(*) FROM pg_constraint WHERE conrelid=?::regclass AND conname IN ('fk_passport_mail_profile','ck_passport_mail_image_hash','ck_passport_mail_state')",Integer.class,schema+".passport_mail_attempts"));
   jdbc.execute("DROP TABLE "+schema+".passport_mail_attempts");
   jdbc.execute(reconciled); // Also exercise first creation from the same unpublished migration.
   var emf=factory.getObject();var manager=new JpaTransactionManager(emf);var tx=new TransactionTemplate(manager);
   EntityManager em=SharedEntityManagerCreator.createSharedEntityManager(emf);
   UUID id=tx.execute(s->{var p=Profile.builder().email("no-mail@test.invalid").emailVerified(true).totalPoints(321).level(2).build();em.persist(p);return p.getId();});
   var profiles=mock(ProfileRepository.class);
   when(profiles.findById(id)).thenAnswer(i->Optional.ofNullable(em.find(Profile.class,id)));
   when(profiles.findByIdForUpdate(id)).thenAnswer(i->Optional.ofNullable(em.find(Profile.class,id,LockModeType.PESSIMISTIC_WRITE)));
   var attempts=new JpaRepositoryFactory(em).getRepository(PassportMailRepository.class);
   var sender=mock(HistarEmailService.class);var transportEntered=new CountDownLatch(1);var releaseTransport=new CountDownLatch(1);
   doAnswer(i->{transportEntered.countDown();assertTrue(releaseTransport.await(60,TimeUnit.SECONDS));return null;}).when(sender).sendAttachment(anyString(),anyString(),anyString(),anyString(),anyString(),any());
   var service=new PassportMailService(profiles,attempts,sender,new HistarMailProperties(),new TestHookProperties(),new HistarPassportMailProperties(),manager);
   org.springframework.test.util.ReflectionTestUtils.setField(service,"entityManager",em);
   var buffer=new ByteArrayOutputStream();ImageIO.write(new BufferedImage(1200,1500,BufferedImage.TYPE_INT_ARGB),"png",buffer);
   var image=new MockMultipartFile("image","x.png","image/png",buffer.toByteArray());var key=UUID.randomUUID();
   var first=executor.submit(()->service.send(id,image,key));
   // Cold PNG/JPA initialization can exceed ten seconds on a busy Windows host.
   if(!transportEntered.await(30,TimeUnit.SECONDS)) {
    if(first.isDone())first.get(); // Surface the actual worker failure instead of a misleading latch timeout.
    fail("First request did not reach the captured transport within 30 seconds");
   }
   var duplicate=executor.submit(()->assertThrows(PassportMailException.class,()->service.send(id,image,key)));
   assertEquals(409,duplicate.get(30,TimeUnit.SECONDS).getStatus());
   var competing=executor.submit(()->assertThrows(PassportMailException.class,()->service.send(id,image,UUID.randomUUID())));
   assertEquals(429,competing.get(30,TimeUnit.SECONDS).getStatus());
   releaseTransport.countDown();assertEquals("ACCEPTED",first.get(30,TimeUnit.SECONDS).status());
   assertEquals("ACCEPTED",service.send(id,image,key).status());verify(sender,times(1)).sendAttachment(anyString(),anyString(),anyString(),anyString(),anyString(),any());
   assertEquals(1L,attempts.count());assertEquals(321,em.find(Profile.class,id).getTotalPoints());
   assertThrows(org.springframework.dao.DataIntegrityViolationException.class,()->jdbc.update("INSERT INTO "+schema+".passport_mail_attempts(id,user_id,request_id,image_sha256,created_at,state) VALUES(?,?,?, ?,now(),'ACCEPTED')",UUID.randomUUID(),id,UUID.randomUUID(),"invalid-hash"));
   tx.executeWithoutResult(s->em.remove(em.find(Profile.class,id)));assertEquals(0L,attempts.count());
  } finally {
   executor.shutdownNow();factory.destroy();
   // Generated fixed-prefix schema only; application tables are outside this schema.
   if(!schema.matches("passport_mail_test_[0-9a-f]{32}"))throw new IllegalStateException("Unexpected test schema");
   jdbc.execute("DROP SCHEMA "+schema+" CASCADE");
  }
 }
}





