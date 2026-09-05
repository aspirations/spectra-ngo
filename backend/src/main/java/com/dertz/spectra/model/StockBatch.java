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
import java.time.LocalDate;

@Entity
@Table(name = "stock_batches")
@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class StockBatch extends Auditable {

	@TenantId
	@Column(name = "tenant_id", nullable = false)
	private Long tenantId;

	@Column(name = "warehouse_id", nullable = false)
	private Long warehouseId;

	@Column(name = "shelter_product_id", nullable = false)
	private Long shelterProductId;

	@Column(name = "grn_item_id")
	private Long grnItemId;

	@Column(nullable = false)
	private String batchNumber;

	@Column(nullable = false)
	private LocalDate expiryDate;

	@Column(nullable = false, precision = 14, scale = 3)
	private BigDecimal qtyOnHand;

	@Column(nullable = false, precision = 14, scale = 4)
	private BigDecimal unitLandedCost;

	@Column(nullable = false)
	private Instant receivedAt;
}
