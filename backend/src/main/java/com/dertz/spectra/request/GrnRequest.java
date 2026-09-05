package com.dertz.spectra.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class GrnRequest {

	@NotNull
	private Long supplierId;

	private Long purchaseOrderId;

	private String notes;

	@NotEmpty
	@Valid
	private List<Item> items;

	@Getter
	@Setter
	@NoArgsConstructor
	public static class Item {
		@NotNull
		private Long shelterProductId;
		private String batchNumber;
		private LocalDate expiryDate;
		private Long purchaseOrderItemId;
		@NotNull
		private BigDecimal quantity;
		@NotNull
		private BigDecimal unitLandedCost;
		private BigDecimal damagedQty;
	}
}
