package com.dertz.spectra.model;

import com.dertz.spectra.Enum.PayrollStatus;
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

import java.time.Instant;

@Entity
@Table(name = "payroll_runs")
@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class PayrollRun extends Auditable {

	@TenantId
	@Column(name = "tenant_id", nullable = false)
	private Long tenantId;

	@Column(name = "branch_id", nullable = false)
	private Long branchId;

	@Column(nullable = false)
	private Integer periodYear;

	@Column(nullable = false)
	private Integer periodMonth;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private PayrollStatus status;

	private Long approvedBy;

	private Long disbursedBy;

	private Instant disbursedAt;
}
