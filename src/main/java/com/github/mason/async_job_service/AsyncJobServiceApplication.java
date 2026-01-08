package com.github.mason.async_job_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class AsyncJobServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(AsyncJobServiceApplication.class, args);
	}

}
