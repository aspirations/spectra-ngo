package com.dertz.spectra.model;

import com.dertz.spectra.Enum.PurchaseOrderStatus;
import com.dertz.spectra.Enum.Role;
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
import java.util.List;

@Entity
@Table(name = "purchase_orders")
@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class PurchaseOrder extends Auditable {

	@TenantId
	@Column(name = "tenant_id", nullable = false)
	private Long tenantId;

	@Column(name = "branch_id", nullable = false)
	private Long branchId;

	@Column(name = "supplier_id", nullable = false)
	private Long supplierId;

	@Column(nullable = false)
	private String poNumber;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private PurchaseOrderStatus status;

	@Column(name = "requested_by_user_id", nullable = false)
	private Long requestedByUserId;

	private Instant submittedAt;

	@Enumerated(EnumType.STRING)
	private Role approverRole;

	@Column(name = "approver_user_id")
	private Long approverUserId;

	@Column(name = "approved_by_user_id")
	private Long approvedByUserId;

	private Instant approvedAt;

	@Column(columnDefinition = "TEXT")
	private String rejectReason;

	@Column(columnDefinition = "TEXT")
	private String notes;

	@Column(nullable = false, precision = 14, scale = 2)
	private BigDecimal expectedTotal;

	@Column(nullable = false, precision = 14, scale = 2)
	private BigDecimal receivedTotal;

	@Transient
	private String supplierName;

	@Transient
	private String requestedByName;

	@Transient
	private String approverName;

	@Transient
	private String approvedByName;

	@Transient
	private List<PurchaseOrderItem> lines;

	@Transient
	private boolean canApprove;

	@Transient
	private boolean canEdit;
}
