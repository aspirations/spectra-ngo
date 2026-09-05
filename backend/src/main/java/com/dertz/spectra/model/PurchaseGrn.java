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
import java.time.Instant;

@Entity
@Table(name = "purchase_grns")
@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class PurchaseGrn extends Auditable {

	@TenantId
	@Column(name = "tenant_id", nullable = false)
	private Long tenantId;

	@Column(name = "branch_id", nullable = false)
	private Long branchId;

	@Column(name = "warehouse_id", nullable = false)
	private Long warehouseId;

	@Column(name = "supplier_id", nullable = false)
	private Long supplierId;

	@Column(name = "purchase_order_id")
	private Long purchaseOrderId;

	@Column(nullable = false)
	private String grnNumber;

	@Column(nullable = false)
	private Instant receivedAt;

	@Column(nullable = false)
	private String status;

	@Column(nullable = false, precision = 14, scale = 2)
	private BigDecimal totalAmount;

	@Column(columnDefinition = "TEXT")
	private String notes;
}
