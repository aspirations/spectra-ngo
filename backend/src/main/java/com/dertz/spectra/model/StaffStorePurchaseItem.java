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

@Entity
@Table(name = "staff_store_purchase_items")
@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class StaffStorePurchaseItem extends Auditable {

	@TenantId
	@Column(name = "tenant_id", nullable = false)
	private Long tenantId;

	@Column(name = "purchase_id", nullable = false)
	private Long purchaseId;

	@Column(name = "product_id", nullable = false)
	private Long productId;

	@Column(nullable = false, precision = 12, scale = 3)
	private BigDecimal qty;

	@Column(nullable = false, precision = 12, scale = 2)
	private BigDecimal unitCost;

	@Column(nullable = false, precision = 12, scale = 2)
	private BigDecimal lineTotal;
}
