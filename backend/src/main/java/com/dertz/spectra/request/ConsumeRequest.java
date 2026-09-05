package com.dertz.spectra.request;

import com.dertz.spectra.Enum.ConsumptionType;
import com.dertz.spectra.Enum.CostCenter;
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
public class ConsumeRequest {

	private ConsumptionType type;

	private String notes;

	private Long takenByUserId;

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
		private BigDecimal quantity;
		private CostCenter costCenter;
		private Long takenByUserId;
		private Long residentId;
	}
}
