package com.dertz.spectra.request;

import com.dertz.spectra.Enum.AttendanceStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
public class AttendanceRequest {

	@NotNull
	private Long userId;

	@NotNull
	private LocalDate workDate;

	@NotNull
	private AttendanceStatus status;
}
