package com.agrichain.crop;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"com.agrichain.crop", "com.agrichain.common"})
public class CropServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(CropServiceApplication.class, args);
    }
}
