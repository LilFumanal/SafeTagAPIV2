package com.lil.safetagv2rppsservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableCaching
@EnableScheduling
public class SafeTagV2RPPSServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(SafeTagV2RPPSServiceApplication.class, args);
    }


}
