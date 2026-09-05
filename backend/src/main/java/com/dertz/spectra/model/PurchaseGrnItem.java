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
@Table(name = "purchase_grn_items")
@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class PurchaseGrnItem extends Auditable {

	@TenantId
	@Column(name = "tenant_id", nullable = false)
	private Long tenantId;

	@Column(name = "grn_id", nullable = false)
	private Long grnId;

	@Column(name = "shelter_product_id", nullable = false)
	private Long shelterProductId;

	@Column(name = "purchase_order_item_id")
	private Long purchaseOrderItemId;

	@Column(nullable = false, precision = 14, scale = 3)
	private BigDecimal damagedQty;

	private String batchNumber;

	private LocalDate expiryDate;

	@Column(nullable = false, precision = 14, scale = 3)
	private BigDecimal quantity;

	@Column(nullable = false, precision = 14, scale = 4)
	private BigDecimal unitLandedCost;

	@Column(nullable = false, precision = 14, scale = 2)
	private BigDecimal lineTotal;
}
