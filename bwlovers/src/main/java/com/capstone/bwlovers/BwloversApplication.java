package com.capstone.bwlovers;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing
@SpringBootApplication
public class BwloversApplication {

	public static void main(String[] args) {
		SpringApplication.run(BwloversApplication.class, args);
	}

}
