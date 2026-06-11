package com.petrecovery;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class PetRecoveryApplication {

    public static void main(String[] args) {
        SpringApplication.run(PetRecoveryApplication.class, args);
    }
}
