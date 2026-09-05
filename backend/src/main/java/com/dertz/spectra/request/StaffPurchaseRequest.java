package com.dertz.spectra.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class StaffPurchaseRequest {

	private String supplierName;
	private String notes;

	@NotEmpty
	@Valid
	private List<Item> items;

	@Getter
	@Setter
	@NoArgsConstructor
	public static class Item {
		@NotNull
		private Long productId;

		@NotNull
		@DecimalMin("0.001")
		private BigDecimal qty;

		@NotNull
		@DecimalMin("0.00")
		private BigDecimal unitCost;
	}
}
