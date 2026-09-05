package com.dertz.spectra.request;

import com.dertz.spectra.Enum.NoteType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class NoteRequest {

	@NotBlank
	private String noteText;

	@NotNull
	private NoteType noteType;
}
