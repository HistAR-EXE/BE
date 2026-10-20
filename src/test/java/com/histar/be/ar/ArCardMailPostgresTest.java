package com.histar.be.ar;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.histar.be.config.*;
import com.histar.be.mail.*;
import com.histar.be.profile.entity.Profile;
import com.histar.be.profile.repository.ProfileRepository;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.*;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import org.springframework.transaction.support.TransactionTemplate;

@EnabledIfEnvironmentVariable(named="RUN_AR_CARD_POSTGRES_IT",matches="true")
class ArCardMailPostgresTest {
    @Test void durableReservationsPreventConcurrentMailAndMigrationPreservesProfiles() throws Exception {
        String schema="ar_card_test_"+UUID.randomUUID().toString().replace("-","");
        var ds=new DriverManagerDataSource(System.getenv("AR_TEST_DB_URL"),System.getenv("AR_TEST_DB_USER"),System.getenv("AR_TEST_DB_PASSWORD"));
        var jdbc=new JdbcTemplate(ds);jdbc.execute("CREATE SCHEMA "+schema);
        var factory=new LocalContainerEntityManagerFactoryBean();factory.setDataSource(ds);
        factory.setPackagesToScan("com.histar.be.profile.entity","com.histar.be.ar");
        factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
        factory.setJpaPropertyMap(Map.of("hibernate.hbm2ddl.auto","create","hibernate.default_schema",schema));
        var pool=Executors.newFixedThreadPool(2);var entered=new CountDownLatch(1);var release=new CountDownLatch(1);
        try {
            factory.afterPropertiesSet();
            jdbc.execute("DROP TABLE "+schema+".ar_card_mail_attempts");
            var migration=java.nio.file.Files.readString(java.nio.file.Path.of("src/main/resources/db/migration/V89__ar_card_mail_attempts.sql"))
                .replace("ar_card_mail_attempts",schema+".ar_card_mail_attempts").replace("REFERENCES profiles","REFERENCES "+schema+".profiles");
            jdbc.execute(migration);jdbc.execute(migration);
            var selected=java.nio.file.Files.readString(java.nio.file.Path.of("src/main/resources/db/migration/V90__ar_card_mail_artifact.sql")).replace("ar_card_mail_attempts",schema+".ar_card_mail_attempts");
            jdbc.execute(selected);jdbc.execute(selected);
            var emf=factory.getObject();var manager=new JpaTransactionManager(emf);var tx=new TransactionTemplate(manager);
            EntityManager em=SharedEntityManagerCreator.createSharedEntityManager(emf);
            var id=tx.execute(s->{var profile=Profile.builder().email("capture@example.invalid").emailVerified(true).totalPoints(321).level(2).build();em.persist(profile);return profile.getId();});
            var profiles=mock(ProfileRepository.class);
            when(profiles.findById(id)).thenAnswer(call->Optional.ofNullable(em.find(Profile.class,id)));
            when(profiles.findByIdForUpdate(id)).thenAnswer(call->Optional.ofNullable(em.find(Profile.class,id,LockModeType.PESSIMISTIC_WRITE)));
            var attempts=new JpaRepositoryFactory(em).getRepository(ArCardMailRepository.class);
            var sender=mock(EmailSender.class);
            doAnswer(call->{entered.countDown();assertTrue(release.await(60,TimeUnit.SECONDS));return null;}).when(sender).sendHtmlBounded(any(),any(),any());
            var app=new HistarAppProperties();app.setFrontendUrl("https://www.timelens.asia");
            var service=new ArCardMailService(profiles,attempts,sender,new HistarMailProperties(),new TestHookProperties(),app,manager);
            org.springframework.test.util.ReflectionTestUtils.setField(service,"entityManager",em);
            var key=UUID.randomUUID();var first=pool.submit(()->service.send(id,"hat",key));
            if(!entered.await(45,TimeUnit.SECONDS)) { if(first.isDone())first.get();fail("First request did not reach captured sender"); }
            assertEquals(409,pool.submit(()->assertThrows(ArCardMailException.class,()->service.send(id,"hat",key))).get(30,TimeUnit.SECONDS).status());
            assertEquals(429,pool.submit(()->assertThrows(ArCardMailException.class,()->service.send(id,"hat",UUID.randomUUID()))).get(30,TimeUnit.SECONDS).status());
            release.countDown();assertEquals("ACCEPTED",first.get(30,TimeUnit.SECONDS).status());
            assertEquals("ACCEPTED",service.send(id,"hat",key).status());
            verify(sender,times(1)).sendHtmlBounded(any(),any(),any());assertEquals(1,attempts.count());
            assertEquals(321,em.find(Profile.class,id).getTotalPoints());
            assertEquals("hat",attempts.findByUserIdAndRequestId(id,key).orElseThrow().getArtifactId());
            assertEquals(409,assertThrows(ArCardMailException.class,()->service.send(id,"lamp",key)).status());
            jdbc.update("UPDATE "+schema+".ar_card_mail_attempts SET created_at=?",Instant.now().minusSeconds(120).atOffset(java.time.ZoneOffset.UTC));
            doThrow(new EmailDeliveryException("captured timeout")).when(sender).sendHtmlBounded(any(),any(),any());
            var failedKey=UUID.randomUUID();assertEquals(502,assertThrows(ArCardMailException.class,()->service.send(id,"hat",failedKey)).status());
            assertEquals(502,assertThrows(ArCardMailException.class,()->service.send(id,"hat",failedKey)).status());
            verify(sender,times(2)).sendHtmlBounded(any(),any(),any());
            assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM "+schema+".ar_card_mail_attempts WHERE state='UNKNOWN'",Integer.class));
            assertThrows(org.springframework.dao.DataIntegrityViolationException.class,()->jdbc.update("INSERT INTO "+schema+".ar_card_mail_attempts (id,user_id,request_id,created_at,state) VALUES(?,?,?,now(),'BAD')",UUID.randomUUID(),id,UUID.randomUUID()));
            tx.executeWithoutResult(s->em.remove(em.find(Profile.class,id)));assertEquals(0,attempts.count());
        } finally {
            release.countDown();pool.shutdownNow();factory.destroy();
            if(!schema.matches("ar_card_test_[0-9a-f]{32}"))throw new IllegalStateException("Unexpected test schema");
            jdbc.execute("DROP SCHEMA "+schema+" CASCADE");
        }
    }
}
