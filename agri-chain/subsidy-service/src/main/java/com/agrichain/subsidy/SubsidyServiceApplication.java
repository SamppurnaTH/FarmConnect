package com.agrichain.subsidy;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"com.agrichain.subsidy", "com.agrichain.common"})
public class SubsidyServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(SubsidyServiceApplication.class, args);
    }
}
