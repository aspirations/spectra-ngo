package com.dertz.spectra.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.TenantId;

import java.math.BigDecimal;

@Entity
@Table(name = "payroll_items")
@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class PayrollItem extends Auditable {

	@TenantId
	@Column(name = "tenant_id", nullable = false)
	private Long tenantId;

	@Column(name = "payroll_run_id", nullable = false)
	private Long payrollRunId;

	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Column(nullable = false, precision = 12, scale = 2)
	private BigDecimal baseSalary;

	@Column(nullable = false)
	private Integer workingDays;

	@Column(nullable = false, precision = 6, scale = 1)
	private BigDecimal unpaidLeaveDays;

	@Column(nullable = false, precision = 12, scale = 2)
	private BigDecimal lopDeduction;

	@Column(nullable = false, precision = 12, scale = 2)
	private BigDecimal storeDues;

	@Column(nullable = false, precision = 12, scale = 2)
	private BigDecimal otherDeductions;

	@Column(name = "advance_deduction", nullable = false, precision = 12, scale = 2)
	private BigDecimal advanceDeduction;

	@Column(name = "reimbursement_credit", nullable = false, precision = 12, scale = 2)
	private BigDecimal reimbursementCredit;

	@Column(nullable = false, precision = 12, scale = 2)
	private BigDecimal netPayout;

	@Column(nullable = false, precision = 12, scale = 2)
	private BigDecimal rolledOverStoreDebt;

	@Column(columnDefinition = "TEXT")
	private String breakdownJson;
}
