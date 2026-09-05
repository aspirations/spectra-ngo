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
@Table(name = "staff_pos_order_items")
@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class StaffPosOrderItem extends Auditable {

	@TenantId
	@Column(name = "tenant_id", nullable = false)
	private Long tenantId;

	@Column(name = "order_id", nullable = false)
	private Long orderId;

	@Column(name = "product_id", nullable = false)
	private Long productId;

	@Column(nullable = false, precision = 12, scale = 3)
	private BigDecimal qty;

	@Column(nullable = false, precision = 12, scale = 2)
	private BigDecimal unitPrice;

	@Column(nullable = false, precision = 12, scale = 2)
	private BigDecimal lineTotal;

	@Column(nullable = false, precision = 12, scale = 2)
	private BigDecimal unitCost;

	@Column(nullable = false, precision = 12, scale = 2)
	private BigDecimal lineCost;

	@Transient
	private String productName;
}
