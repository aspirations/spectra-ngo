package com.dertz.spectra.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class SupplierRequest {

	@NotBlank
	private String name;

	private String contactPhone;
	private String contactEmail;
}
