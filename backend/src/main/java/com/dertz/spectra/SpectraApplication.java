package com.dertz.spectra;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing(auditorAwareRef = "applicationAuditAware")
public class SpectraApplication {

	public static void main(String[] args) {
		SpringApplication.run(SpectraApplication.class, args);
	}
}
