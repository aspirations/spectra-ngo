package com.dertz.spectra.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
public class SupplierPaymentRequest {

	@NotNull
	private BigDecimal amount;

	private String notes;
}
