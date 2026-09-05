package com.dertz.spectra.request;

import com.dertz.spectra.Enum.AuditReason;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
public class AuditRequest {

	@NotNull
	private Long shelterProductId;

	private Long batchId;

	@NotNull
	private BigDecimal physicalQty;

	@NotNull
	private AuditReason reason;

	private String notes;
}
