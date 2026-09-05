package com.dertz.spectra.request;

import com.dertz.spectra.Enum.EntityStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class NgoStatusRequest {

	@NotNull
	private EntityStatus status;
}
