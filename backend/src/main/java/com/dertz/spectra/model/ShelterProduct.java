package com.dertz.spectra.model;

import com.dertz.spectra.Enum.ShelterProductCategory;
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
@Table(name = "shelter_products")
@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class ShelterProduct extends Auditable {

	@TenantId
	@Column(name = "tenant_id", nullable = false)
	private Long tenantId;

	@Column(nullable = false)
	private String sku;

	@Column(nullable = false)
	private String name;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private UnitOfMeasure unit;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private ShelterProductCategory category;

	private String barcode;

	private Integer vaccineIntervalDays;

	@Column(nullable = false, precision = 14, scale = 3)
	private BigDecimal reorderLevel;

	@Column(name = "lot_tracked", nullable = false)
	private Boolean lotTracked;

	@Column(name = "qty_on_hand", nullable = false, precision = 14, scale = 3)
	private BigDecimal qtyOnHand;

	@Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
	private BigDecimal unitPrice;

	@Column(name = "unit_cost", nullable = false, precision = 12, scale = 2)
	private BigDecimal unitCost;

	@Column(name = "staff_sale", nullable = false)
	private Boolean staffSale;

	@Column(name = "clinical_use", nullable = false)
	private Boolean clinicalUse;

	@Column(nullable = false)
	private Boolean active;
}
