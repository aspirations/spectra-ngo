package com.dertz.spectra.model;

import com.dertz.spectra.Enum.ExpenseCategory;
import com.dertz.spectra.Enum.PaymentSource;
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
@Table(name = "operating_expenses")
@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class OperatingExpense extends Auditable {

	@TenantId
	@Column(name = "tenant_id", nullable = false)
	private Long tenantId;

	@Column(name = "branch_id", nullable = false)
	private Long branchId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private ExpenseCategory category;

	@Column(nullable = false, length = 500)
	private String description;

	@Column(nullable = false, precision = 14, scale = 2)
	private BigDecimal amount;

	@Enumerated(EnumType.STRING)
	@Column(name = "payment_source", nullable = false)
	private PaymentSource paymentSource;

	@Column(name = "expense_date", nullable = false)
	private LocalDate expenseDate;

	@Column(name = "receipt_url")
	private String receiptUrl;

	@Column(name = "paid_by_user_id")
	private Long paidByUserId;

	@Column(name = "vendor_name")
	private String vendorName;

	@Column(name = "reimbursed_in_payroll_item_id")
	private Long reimbursedInPayrollItemId;
}
