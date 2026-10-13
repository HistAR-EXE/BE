package com.histar.be.config;

import static org.assertj.core.api.Assertions.*;

import com.histar.be.rag.config.RagProperties;
import org.junit.jupiter.api.Test;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.mock.env.MockEnvironment;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.slf4j.LoggerFactory;

class ChatRuntimeDiagnosticsTest {
    @Test void startupReportsEffectiveConfigAndBuildIdentityWithoutSecrets() throws Exception {
        var environment = new MockEnvironment().withProperty("chat.provider", "hybrid");
        environment.setActiveProfiles("local");
        var chat = new ChatProperties();
        chat.getRagAi().setBaseUrl("http://user:secret@127.0.0.1:8100/private?token=secret#secret");
        chat.getRagAi().setTimeoutSeconds(100);
        var rag = new RagProperties();
        rag.setEnabled(false);
        var appender = new ListAppender<ILoggingEvent>();
        appender.start();
        Logger logger = (Logger) LoggerFactory.getLogger("com.histar.be.config.ChatRuntimeDiagnostics");
        logger.addAppender(appender);
        try (var context = new AnnotationConfigApplicationContext()) {
            context.setEnvironment(environment);
            context.registerBean(ChatProperties.class, () -> chat);
            context.registerBean(RagProperties.class, () -> rag);
            // Select only this component: no DB, backend or unrelated configuration startup.
            var scanner = new org.springframework.context.annotation.ClassPathBeanDefinitionScanner(context, false);
            scanner.addIncludeFilter(new org.springframework.core.type.filter.RegexPatternTypeFilter(
                    java.util.regex.Pattern.compile("com\\.histar\\.be\\.config\\.ChatRuntimeDiagnostics")));
            scanner.scan("com.histar.be.config");
            context.refresh();
            var runner = context.getBeansOfType(ApplicationRunner.class).values();
            assertThat(runner).hasSize(1);
            runner.iterator().next().run(new org.springframework.boot.DefaultApplicationArguments());
            assertThat(appender.list).hasSize(1);
            assertThat(appender.list.get(0).getFormattedMessage())
                    .contains("ragTimeoutSeconds=100", "ragMaxAttempts=1", "semanticRagEnabled=false",
                            "sseTimeoutMillis=120000", "profiles=[local]", "ragOrigin=http://127.0.0.1:8100")
                    .containsPattern("chatBuildFingerprint=[a-f0-9]{64}")
                    .doesNotContain("secret", "token", "private", "user:");
        } finally { logger.detachAppender(appender); }
    }
}
