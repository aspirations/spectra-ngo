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
import java.time.LocalDate;

@Entity
@Table(name = "fuel_logs")
@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class FuelLog extends Auditable {

	@TenantId
	@Column(name = "tenant_id", nullable = false)
	private Long tenantId;

	@Column(name = "branch_id", nullable = false)
	private Long branchId;

	@Column(name = "vehicle_label", nullable = false)
	private String vehicleLabel;

	@Column(name = "odometer_km", nullable = false, precision = 12, scale = 1)
	private BigDecimal odometerKm;

	@Column(nullable = false, precision = 12, scale = 2)
	private BigDecimal litres;

	@Column(nullable = false, precision = 14, scale = 2)
	private BigDecimal amount;

	@Column(name = "filled_at", nullable = false)
	private LocalDate filledAt;

	@Column(name = "operating_expense_id")
	private Long operatingExpenseId;
}
