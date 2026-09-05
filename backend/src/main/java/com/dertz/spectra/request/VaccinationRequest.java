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
public class VaccinationRequest {

	@NotNull
	private Long shelterProductId;

	private BigDecimal quantity;

	private String notes;

	private LocalDate nextDueDate;
}
