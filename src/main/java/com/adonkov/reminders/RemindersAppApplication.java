package com.adonkov.reminders;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class RemindersAppApplication {

    public static void main(String[] args) {
        SpringApplication.run(RemindersAppApplication.class, args);
    }
}
