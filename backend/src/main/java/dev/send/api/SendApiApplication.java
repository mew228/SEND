package dev.send.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class SendApiApplication {

  public static void main(String[] args) {
    SpringApplication.run(SendApiApplication.class, args);
  }
}
