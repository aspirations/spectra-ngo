package com.dertz.spectra.request;

import com.dertz.spectra.Enum.LeaveType;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
public class LeaveRequestDto {

	@NotNull
	private LeaveType type;

	@NotNull
	private LocalDate fromDate;

	@NotNull
	private LocalDate toDate;

	private String reason;
	private Long userId;
}
