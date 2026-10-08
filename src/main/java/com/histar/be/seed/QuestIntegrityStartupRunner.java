package com.histar.be.seed;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

/** Runs the runtime quest integrity gate and terminates the process for CI/deploy scripts. */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "histar.quest-integrity.run-on-startup", havingValue = "true")
public class QuestIntegrityStartupRunner implements ApplicationRunner {

    private final SeedConsistencyValidator seedConsistencyValidator;
    private final ConfigurableApplicationContext applicationContext;

    @Override
    public void run(ApplicationArguments args) {
        seedConsistencyValidator.validateAll();
        log.info("Quest runtime integrity validation passed");
        applicationContext.close();
    }
}
