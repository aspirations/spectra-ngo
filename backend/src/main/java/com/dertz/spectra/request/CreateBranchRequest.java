package com.dertz.spectra.request;

import com.dertz.spectra.Enum.EntityStatus;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CreateBranchRequest {

	@NotBlank
	private String name;

	@NotBlank
	private String code;

	private String address;
	private String city;
	private EntityStatus status;
}
