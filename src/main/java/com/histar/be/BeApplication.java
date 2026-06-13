package com.histar.be;

import com.histar.be.config.ChatProperties;
import com.histar.be.config.DemoProperties;
import com.histar.be.config.EnvFileLoader;
import com.histar.be.config.GamificationProperties;
import com.histar.be.config.MinioProperties;
import com.histar.be.config.ViralProperties;
import com.histar.be.config.VisitSessionProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties({
    GamificationProperties.class,
    ChatProperties.class,
    MinioProperties.class,
    ViralProperties.class,
    DemoProperties.class,
    VisitSessionProperties.class
})
public class BeApplication {

    public static void main(String[] args) {
        EnvFileLoader.loadIfPresent();
        SpringApplication.run(BeApplication.class, args);
    }

}
