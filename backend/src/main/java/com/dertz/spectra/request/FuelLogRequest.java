package com.dertz.spectra.request;

import com.dertz.spectra.Enum.PaymentSource;
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
public class FuelLogRequest {

	@NotBlank
	private String vehicleLabel;

	@NotNull
	private BigDecimal odometerKm;

	@NotNull
	private BigDecimal litres;

	@NotNull
	private BigDecimal amount;

	private LocalDate filledAt;

	private PaymentSource paymentSource;

	private Long paidByUserId;

	private String receiptUrl;
}
