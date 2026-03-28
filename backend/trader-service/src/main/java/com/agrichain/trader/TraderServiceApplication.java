package com.agrichain.trader;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"com.agrichain.trader", "com.agrichain.common"})
public class TraderServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(TraderServiceApplication.class, args);
    }
}
