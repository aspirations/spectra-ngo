package com.dertz.spectra.config;

import com.dertz.spectra.security.TenantContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;

import java.util.Optional;

@Configuration
public class AuditConfig {

	@Bean
	public AuditorAware<Long> applicationAuditAware() {
		return () -> {
			TenantContext.Snapshot snapshot = TenantContext.get();
			if (snapshot == null || snapshot.userId() == null) {
				return Optional.empty();
			}
			return Optional.of(snapshot.userId());
		};
	}
}
