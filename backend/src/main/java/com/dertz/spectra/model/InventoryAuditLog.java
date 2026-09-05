package com.dertz.spectra.model;

import com.dertz.spectra.Enum.AuditReason;
import com.dertz.spectra.Enum.AuditStatus;
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
@Table(name = "inventory_audit_logs")
@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class InventoryAuditLog extends Auditable {

	@TenantId
	@Column(name = "tenant_id", nullable = false)
	private Long tenantId;

	@Column(name = "branch_id", nullable = false)
	private Long branchId;

	@Column(name = "warehouse_id", nullable = false)
	private Long warehouseId;

	@Column(name = "shelter_product_id", nullable = false)
	private Long shelterProductId;

	@Column(name = "batch_id")
	private Long batchId;

	@Column(nullable = false, precision = 14, scale = 3)
	private BigDecimal systemQty;

	@Column(nullable = false, precision = 14, scale = 3)
	private BigDecimal physicalQty;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private AuditReason reason;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private AuditStatus status;

	@Column(columnDefinition = "TEXT")
	private String notes;

	private Long reviewedBy;

	private Instant reviewedAt;
}
