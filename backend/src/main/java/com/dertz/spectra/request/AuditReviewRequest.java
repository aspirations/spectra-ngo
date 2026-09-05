package com.dertz.spectra.request;

import com.dertz.spectra.Enum.AuditStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class AuditReviewRequest {

	@NotNull
	private AuditStatus status;

	private String notes;
}
