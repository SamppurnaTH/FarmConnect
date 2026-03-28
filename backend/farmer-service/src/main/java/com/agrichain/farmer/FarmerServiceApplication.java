package com.agrichain.farmer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"com.agrichain.farmer", "com.agrichain.common"})
public class FarmerServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(FarmerServiceApplication.class, args);
    }
}
