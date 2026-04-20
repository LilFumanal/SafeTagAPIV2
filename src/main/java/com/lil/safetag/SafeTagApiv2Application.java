package com.lil.safetag;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class SafeTagApiv2Application {

    public static void main(String[] args) {
        SpringApplication.run(SafeTagApiv2Application.class, args);
    }


}
