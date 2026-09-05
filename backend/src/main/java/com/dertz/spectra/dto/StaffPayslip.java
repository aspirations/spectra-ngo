package com.dertz.spectra.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class StaffPayslip {

	private final Long userId;
	private final String fullName;
	private final String email;
	private final String role;
	private final int periodYear;
	private final int periodMonth;
	private final String periodLabel;
	private final boolean locked;
	private final String runStatus;
	private final BigDecimal baseSalary;
	private final int workingDays;
	private final BigDecimal unpaidLeaveDays;
	private final BigDecimal dailyWage;
	private final BigDecimal lopDeduction;
	private final BigDecimal storeDues;
	private final BigDecimal otherDeductions;
	private final BigDecimal advanceDeduction;
	private final BigDecimal reimbursementCredit;
	private final BigDecimal netPayout;
	private final BigDecimal rolledOverStoreDebt;
}
