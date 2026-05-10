package com.agrichain.subsidy;

import com.agrichain.common.logging.CorrelationIdFilter;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"com.agrichain.subsidy", "com.agrichain.common"})
public class SubsidyServiceApplication {

    @Bean
    public CorrelationIdFilter correlationIdFilter() {
        return new CorrelationIdFilter();
    }

    public static void main(String[] args) {
        SpringApplication.run(SubsidyServiceApplication.class, args);
    }
}
