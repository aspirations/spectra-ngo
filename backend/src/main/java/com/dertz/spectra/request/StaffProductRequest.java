package com.dertz.spectra.request;

import com.dertz.spectra.Enum.UnitOfMeasure;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
public class StaffProductRequest {

	@NotBlank
	private String sku;

	@NotBlank
	private String name;

	private UnitOfMeasure unit;

	@NotNull
	private BigDecimal unitPrice;

	private BigDecimal unitCost;
	private String barcode;
	private BigDecimal qtyOnHand;
}
