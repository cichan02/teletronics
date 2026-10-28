package by.piskunou.teletronics;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan("by.piskunou.teletronics.config.property")
public class TeletronicsApp {
    static void main(String[] args) {
        SpringApplication.run(TeletronicsApp.class, args);
    }
}
