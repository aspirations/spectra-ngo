package com.dertz.spectra.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.TenantId;

import java.math.BigDecimal;

@Entity
@Table(name = "purchase_order_items")
@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class PurchaseOrderItem extends Auditable {

	@TenantId
	@Column(name = "tenant_id", nullable = false)
	private Long tenantId;

	@Column(name = "purchase_order_id", nullable = false)
	private Long purchaseOrderId;

	@Column(name = "shelter_product_id", nullable = false)
	private Long shelterProductId;

	@Column(nullable = false, precision = 14, scale = 3)
	private BigDecimal qtyOrdered;

	@Column(nullable = false, precision = 14, scale = 4)
	private BigDecimal unitCost;

	@Column(nullable = false, precision = 14, scale = 3)
	private BigDecimal qtyReceived;

	@Column(nullable = false, precision = 14, scale = 3)
	private BigDecimal damagedQty;

	@Column(columnDefinition = "TEXT")
	private String notes;

	@Transient
	private String productName;

	@Transient
	private String sku;

	@Transient
	private Boolean lotTracked;
}
