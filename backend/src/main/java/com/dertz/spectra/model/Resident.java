package com.dertz.spectra.model;

import com.dertz.spectra.Enum.CareKind;
import com.dertz.spectra.Enum.CareSection;
import com.dertz.spectra.Enum.CareSex;
import com.dertz.spectra.Enum.CareStatus;
import com.dertz.spectra.Enum.IntakeSource;
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

import java.time.LocalDate;

@Entity
@Table(name = "residents")
@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class Resident extends Auditable {

	@TenantId
	@Column(name = "tenant_id", nullable = false)
	private Long tenantId;

	@Column(name = "branch_id", nullable = false)
	private Long branchId;

	@Column(nullable = false)
	private String referenceId;

	@Column(nullable = false)
	private String name;

	private String photoUrl;

	private String collarNo;

	@Column(columnDefinition = "TEXT")
	private String description;

	@Enumerated(EnumType.STRING)
	private CareSex sex;

	private String color;

	private String approxAge;

	@Enumerated(EnumType.STRING)
	private IntakeSource intakeSource;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private CareKind kind;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private CareSection category;

	@Column(nullable = false)
	private LocalDate intakeDate;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private CareStatus status;
}
