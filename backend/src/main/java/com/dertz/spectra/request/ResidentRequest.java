package com.dertz.spectra.request;

import com.dertz.spectra.Enum.CareSection;
import com.dertz.spectra.Enum.CareSex;
import com.dertz.spectra.Enum.CareStatus;
import com.dertz.spectra.Enum.IntakeSource;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
public class ResidentRequest {

	@NotBlank
	private String referenceId;

	@NotBlank
	private String name;

	private String photoUrl;

	private String collarNo;

	private String description;

	private CareSex sex;

	private String color;

	private String approxAge;

	private IntakeSource intakeSource;

	@NotNull
	private CareSection category;

	@NotNull
	private LocalDate intakeDate;

	private CareStatus status;
}
