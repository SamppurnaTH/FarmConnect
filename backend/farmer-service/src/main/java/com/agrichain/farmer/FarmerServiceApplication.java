package com.agrichain.farmer;

import com.agrichain.common.logging.CorrelationIdFilter;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"com.agrichain.farmer", "com.agrichain.common"})
public class FarmerServiceApplication {

    @Bean
    public CorrelationIdFilter correlationIdFilter() {
        return new CorrelationIdFilter();
    }

    public static void main(String[] args) {
        SpringApplication.run(FarmerServiceApplication.class, args);
    }
}
