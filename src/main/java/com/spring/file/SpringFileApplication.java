package com.spring.file;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@ConfigurationPropertiesScan
@SpringBootApplication
public class SpringFileApplication {

  public static void main(String[] args) {
    SpringApplication.run(SpringFileApplication.class, args);
  }

}
