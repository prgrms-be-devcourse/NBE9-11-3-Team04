package com.back.devc;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class DevCApplication {

    public static void main(String[] args) {
        SpringApplication.run(DevCApplication.class, args);
    }

}
