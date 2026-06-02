package com.histar.be;

import com.histar.be.config.GamificationProperties;
import com.histar.be.config.MinioProperties;
import com.histar.be.config.ViralProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({GamificationProperties.class, MinioProperties.class, ViralProperties.class})
public class BeApplication {

    public static void main(String[] args) {
        SpringApplication.run(BeApplication.class, args);
    }

}
