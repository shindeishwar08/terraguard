package com.terraguard.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class TerraGuardApplication {

	public static void main(String[] args) {
		SpringApplication.run(TerraGuardApplication.class, args);
	}

}
