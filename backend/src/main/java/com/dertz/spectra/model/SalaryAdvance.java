package com.dertz.spectra.model;

import com.dertz.spectra.Enum.AdvanceStatus;
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

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "salary_advances")
@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class SalaryAdvance extends Auditable {

	@TenantId
	@Column(name = "tenant_id", nullable = false)
	private Long tenantId;

	@Column(name = "branch_id", nullable = false)
	private Long branchId;

	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Column(nullable = false, precision = 12, scale = 2)
	private BigDecimal amount;

	@Column(name = "advanced_at", nullable = false)
	private LocalDate advancedAt;

	private String notes;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private AdvanceStatus status;

	@Column(name = "payroll_run_id")
	private Long payrollRunId;
}
