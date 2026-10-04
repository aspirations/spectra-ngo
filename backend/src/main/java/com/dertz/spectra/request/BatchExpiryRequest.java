package com.dertz.spectra.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
public class BatchExpiryRequest {

	@NotNull
	private LocalDate expiryDate;
}
