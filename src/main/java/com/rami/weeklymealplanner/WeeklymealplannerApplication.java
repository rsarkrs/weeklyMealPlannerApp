package com.rami.weeklymealplanner;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import com.rami.weeklymealplanner.config.KrogerProperties;
import com.rami.weeklymealplanner.config.OpenAiProperties;

@SpringBootApplication
@EnableConfigurationProperties({KrogerProperties.class, OpenAiProperties.class})
public class WeeklymealplannerApplication {
    public static void main(String[] args) {
        SpringApplication.run(WeeklymealplannerApplication.class, args);
    }
}
