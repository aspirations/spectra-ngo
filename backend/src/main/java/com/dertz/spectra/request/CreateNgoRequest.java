package com.dertz.spectra.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CreateNgoRequest {

	@NotBlank
	private String name;

	@NotBlank
	private String code;

	private String timezone;
	private String currency;

	@NotBlank
	private String adminName;

	@NotBlank
	@Email
	private String adminEmail;

	@NotBlank
	private String adminPassword;

	private String firstBranchName;
	private String firstBranchCode;
}
