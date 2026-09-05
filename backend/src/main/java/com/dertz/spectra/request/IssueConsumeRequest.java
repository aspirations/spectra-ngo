package com.dertz.spectra.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class IssueConsumeRequest {

	@NotNull
	private Long takenByUserId;
}
