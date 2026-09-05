package com.dertz.spectra.request;

import com.dertz.spectra.Enum.LeaveStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class LeaveReviewRequest {

	@NotNull
	private LeaveStatus status;
}
