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
@Table(name = "branch_stock")
@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class BranchStock extends Auditable {

	@TenantId
	@Column(name = "tenant_id", nullable = false)
	private Long tenantId;

	@Column(name = "warehouse_id", nullable = false)
	private Long warehouseId;

	@Column(name = "shelter_product_id", nullable = false)
	private Long shelterProductId;

	@Column(nullable = false, precision = 14, scale = 3)
	private BigDecimal qtyOnHand;
}
