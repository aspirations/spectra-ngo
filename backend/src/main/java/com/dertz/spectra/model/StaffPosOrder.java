package com.dertz.spectra.model;

import com.dertz.spectra.Enum.PosOrderStatus;
import com.dertz.spectra.Enum.PosTender;
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
import java.time.Instant;

@Entity
@Table(name = "staff_pos_orders")
@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class StaffPosOrder extends Auditable {

	@TenantId
	@Column(name = "tenant_id", nullable = false)
	private Long tenantId;

	@Column(name = "branch_id", nullable = false)
	private Long branchId;

	@Column(name = "employee_id", nullable = false)
	private Long employeeId;

	@Column(name = "cashier_id", nullable = false)
	private Long cashierId;

	@Column(nullable = false)
	private String orderNumber;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private PosTender tender;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private PosOrderStatus status;

	@Column(nullable = false, precision = 12, scale = 2)
	private BigDecimal total;

	@Column(name = "payroll_run_id")
	private Long payrollRunId;

	@Column(nullable = false)
	private Instant orderAt;
}
