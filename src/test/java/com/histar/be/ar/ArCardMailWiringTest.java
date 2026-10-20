package com.histar.be.ar;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.histar.be.config.*;
import com.histar.be.mail.EmailSender;
import com.histar.be.profile.repository.ProfileRepository;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.transaction.PlatformTransactionManager;

class ArCardMailWiringTest {
    @Test void serviceSelectsConfiguredProviderWhenConcreteProviderBeansAlsoExist() {
        try(var context=new AnnotationConfigApplicationContext()) {
            context.addBeanFactoryPostProcessor(new PropertySourcesPlaceholderConfigurer());
            for(String name:new String[]{"smtpEmailSender","brevoEmailSender","resendEmailSender","emailSender"})context.registerBean(name,EmailSender.class,()->mock(EmailSender.class));
            context.registerBean(ProfileRepository.class,()->mock(ProfileRepository.class));
            context.registerBean(ArCardMailRepository.class,()->mock(ArCardMailRepository.class));
            context.registerBean(HistarMailProperties.class,HistarMailProperties::new);
            context.registerBean(TestHookProperties.class,TestHookProperties::new);
            context.registerBean(HistarAppProperties.class,HistarAppProperties::new);
            context.registerBean(jakarta.persistence.EntityManagerFactory.class,()->mock(jakarta.persistence.EntityManagerFactory.class));
            context.registerBean(PlatformTransactionManager.class,()->mock(PlatformTransactionManager.class));
            context.registerBean(ArCardMailService.class);
            assertDoesNotThrow(context::refresh);
            assertSame(context.getBean("emailSender"),org.springframework.test.util.ReflectionTestUtils.getField(context.getBean(ArCardMailService.class),"sender"));
        }
    }
}
