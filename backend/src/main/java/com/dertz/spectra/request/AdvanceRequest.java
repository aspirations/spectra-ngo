package com.dertz.spectra.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
public class AdvanceRequest {

	@NotNull
	private Long userId;

	@NotNull
	private BigDecimal amount;

	private LocalDate advancedAt;

	private String notes;
}
