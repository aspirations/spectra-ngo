package com.dertz.spectra.service;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SequenceService {

	private final JdbcTemplate jdbcTemplate;

	public String nextGrnNumber() {
		Long n = jdbcTemplate.queryForObject("SELECT nextval('spectra_grn_seq')", Long.class);
		return "GRN-" + String.format("%06d", n);
	}

	public String nextPoNumber() {
		Long n = jdbcTemplate.queryForObject("SELECT nextval('spectra_po_seq')", Long.class);
		return "PO-" + String.format("%06d", n);
	}

	public String nextPosNumber() {
		Long n = jdbcTemplate.queryForObject("SELECT nextval('spectra_pos_seq')", Long.class);
		return "POS-" + String.format("%06d", n);
	}

	public String nextStaffPurchaseNumber() {
		Long n = jdbcTemplate.queryForObject("SELECT nextval('spectra_staff_purchase_seq')", Long.class);
		return "STP-" + String.format("%06d", n);
	}
}
