package com.bookman;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class BookmanApplication {

    public static void main(String[] args) {
        SpringApplication.run(BookmanApplication.class, args);
    }
}
