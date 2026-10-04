package com.dertz.spectra.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
public class BatchExpiryRequest {

	@NotNull
	private LocalDate expiryDate;

	@PositiveOrZero
	private BigDecimal qtyOnHand;
}
