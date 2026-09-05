package com.dertz.spectra.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class RejectPurchaseOrderRequest {

	@NotBlank
	private String reason;
}
