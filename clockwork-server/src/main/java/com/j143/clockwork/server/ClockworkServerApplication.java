package com.j143.clockwork.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "com.j143.clockwork")
@EnableScheduling
public class ClockworkServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(ClockworkServerApplication.class, args);
    }
}
