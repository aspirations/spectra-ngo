package com.dertz.spectra.model;

import com.dertz.spectra.Enum.UnitOfMeasure;
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

@Entity
@Table(name = "staff_store_products")
@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class StaffStoreProduct extends Auditable {

	@TenantId
	@Column(name = "tenant_id", nullable = false)
	private Long tenantId;

	@Column(name = "branch_id", nullable = false)
	private Long branchId;

	@Column(name = "warehouse_id", nullable = false)
	private Long warehouseId;

	@Column(nullable = false)
	private String sku;

	@Column(nullable = false)
	private String name;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private UnitOfMeasure unit;

	@Column(nullable = false, precision = 12, scale = 2)
	private BigDecimal unitPrice;

	@Column(nullable = false, precision = 12, scale = 2)
	private BigDecimal unitCost;

	private String barcode;

	@Column(nullable = false, precision = 14, scale = 3)
	private BigDecimal qtyOnHand;

	@Column(nullable = false)
	private Boolean active;
}
