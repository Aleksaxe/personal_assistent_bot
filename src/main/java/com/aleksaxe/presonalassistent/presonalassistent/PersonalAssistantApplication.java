package com.aleksaxe.presonalassistent.presonalassistent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class PersonalAssistantApplication {

    public static void main(String[] args) {
        SpringApplication.run(PersonalAssistantApplication.class, args);
    }

}
