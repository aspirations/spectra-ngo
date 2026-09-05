package com.dertz.spectra.model;

import com.dertz.spectra.Enum.ConsumptionStatus;
import com.dertz.spectra.Enum.ConsumptionType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.TenantId;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "internal_consumptions")
@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class InternalConsumption extends Auditable {

	@TenantId
	@Column(name = "tenant_id", nullable = false)
	private Long tenantId;

	@Column(name = "branch_id", nullable = false)
	private Long branchId;

	@Column(name = "warehouse_id", nullable = false)
	private Long warehouseId;

	@Column(nullable = false)
	private LocalDate consumptionDate;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private ConsumptionType type;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private ConsumptionStatus status;

	@Column(name = "requested_by_user_id")
	private Long requestedByUserId;

	@Column(name = "issued_by_user_id")
	private Long issuedByUserId;

	private Instant issuedAt;

	@Column(precision = 14, scale = 3)
	private BigDecimal theoreticalQty;

	@Column(precision = 14, scale = 3)
	private BigDecimal actualQty;

	@Column(precision = 10, scale = 4)
	private BigDecimal variancePct;

	@Column(nullable = false)
	private Boolean varianceAlert;

	@Column(columnDefinition = "TEXT")
	private String notes;

	@Column(name = "taken_by_user_id")
	private Long takenByUserId;

	@Transient
	private String takenByName;

	@Transient
	private String requestedByName;

	@Transient
	private String issuedByName;

	@Transient
	private List<InternalConsumptionItem> lines;
}
