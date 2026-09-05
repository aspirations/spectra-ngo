package com.dertz.spectra.request;

import com.dertz.spectra.Enum.ShelterProductCategory;
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
public class ShelterProductRequest {

	@NotBlank
	private String sku;

	@NotBlank
	private String name;

	@NotNull
	private UnitOfMeasure unit;

	@NotNull
	private ShelterProductCategory category;

	private String barcode;
	private Integer vaccineIntervalDays;
	private BigDecimal reorderLevel;
	private Boolean lotTracked;
	private Boolean staffSale;
	private Boolean clinicalUse;
	private BigDecimal unitPrice;
	private BigDecimal unitCost;
	private BigDecimal qtyOnHand;
}
