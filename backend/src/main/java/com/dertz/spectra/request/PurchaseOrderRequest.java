package com.dertz.spectra.request;

import jakarta.validation.Valid;
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
public class PurchaseOrderRequest {

	@NotNull
	private Long supplierId;

	private String notes;

	private Long approverUserId;

	@NotEmpty
	@Valid
	private List<Item> items;

	@Getter
	@Setter
	@NoArgsConstructor
	public static class Item {
		@NotNull
		private Long shelterProductId;
		@NotNull
		private BigDecimal qtyOrdered;
		@NotNull
		private BigDecimal unitCost;
		private String notes;
	}
}
