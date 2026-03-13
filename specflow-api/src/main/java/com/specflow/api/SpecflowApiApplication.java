package com.specflow.api;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.specflow.api.modules.**.infrastructure.persistence")
public class SpecflowApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpecflowApiApplication.class, args);
    }
}
