package com.metrobank.uploadITR;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class UploadItrApplication {

	public static void main(String[] args) {
		SpringApplication.run(UploadItrApplication.class, args);
	}

}

