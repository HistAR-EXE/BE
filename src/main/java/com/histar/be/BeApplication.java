package com.histar.be;

import com.histar.be.config.ChatProperties;
import com.histar.be.config.DemoProperties;
import com.histar.be.config.TestHookProperties;
import com.histar.be.config.EnvFileLoader;
import com.histar.be.config.GamificationProperties;
import com.histar.be.config.HistarAppProperties;
import com.histar.be.config.HistarFirebaseProperties;
import com.histar.be.config.HistarMailProperties;
import com.histar.be.config.MinioProperties;
import com.histar.be.config.BrevoProperties;
import com.histar.be.config.PresenceProperties;
import com.histar.be.config.ResendProperties;
import com.histar.be.config.ViralProperties;
import com.histar.be.config.VisitSessionProperties;
import com.histar.be.rag.config.RagProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableAsync
@EnableConfigurationProperties({
    GamificationProperties.class,
    ChatProperties.class,
    MinioProperties.class,
    ViralProperties.class,
    DemoProperties.class,
    TestHookProperties.class,
    VisitSessionProperties.class,
    HistarAppProperties.class,
    HistarMailProperties.class,
    BrevoProperties.class,
    ResendProperties.class,
    HistarFirebaseProperties.class,
    PresenceProperties.class,
    RagProperties.class
})
public class BeApplication {

    public static void main(String[] args) {
        EnvFileLoader.loadIfPresent();
        SpringApplication.run(BeApplication.class, args);
    }

}
