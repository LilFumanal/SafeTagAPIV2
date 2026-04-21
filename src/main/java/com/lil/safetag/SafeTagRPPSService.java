package com.lil.safetag;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class SafeTagRPPSService {

    public static void main(String[] args) {
        SpringApplication.run(SafeTagRPPSService.class, args);
    }


}
