package com.dertz.spectra.model;

import com.dertz.spectra.Enum.NoteType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.TenantId;

@Entity
@Table(name = "resident_notes")
@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class DogCaseNote extends Auditable {

	@TenantId
	@Column(name = "tenant_id", nullable = false)
	private Long tenantId;

	@Column(name = "resident_id", nullable = false)
	private Long residentId;

	@Column(nullable = false, columnDefinition = "TEXT")
	private String noteText;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private NoteType noteType;
}
