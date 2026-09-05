package com.dertz.spectra.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class GlobalUserLookup {

	private final JdbcTemplate jdbcTemplate;

	public Optional<Long> findTenantIdByEmail(String email) {
		List<Long> ids = jdbcTemplate.query(
				"SELECT tenant_id FROM users WHERE lower(email) = lower(?) LIMIT 1",
				(rs, rowNum) -> rs.getLong(1),
				email);
		return ids.stream().findFirst();
	}

	public boolean emailTaken(String email) {
		Integer count = jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM users WHERE lower(email) = lower(?)",
				Integer.class,
				email);
		return count != null && count > 0;
	}
}
