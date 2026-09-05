package com.dertz.spectra.request;

import com.dertz.spectra.Enum.ExpenseCategory;
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
public class OperatingExpenseRequest {

	@NotNull
	private ExpenseCategory category;

	@NotBlank
	private String description;

	@NotNull
	private BigDecimal amount;

	@NotNull
	private PaymentSource paymentSource;

	private LocalDate expenseDate;

	private String receiptUrl;

	private Long paidByUserId;

	private String vendorName;

	private Long residentId;
}
