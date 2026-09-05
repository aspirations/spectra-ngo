package com.dertz.spectra.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
public class DogExpenseRequest {

	@NotNull
	private Long residentId;

	@NotBlank
	private String label;

	@NotNull
	private BigDecimal amount;

	private LocalDate expenseDate;

	private Long operatingExpenseId;

	private Long shelterProductId;
}
