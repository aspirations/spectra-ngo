package com.dertz.spectra.model;

import com.dertz.spectra.Enum.CostCenter;
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

@Entity
@Table(name = "internal_consumption_items")
@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class InternalConsumptionItem extends Auditable {

	@TenantId
	@Column(name = "tenant_id", nullable = false)
	private Long tenantId;

	@Column(name = "consumption_id", nullable = false)
	private Long consumptionId;

	@Column(name = "shelter_product_id", nullable = false)
	private Long shelterProductId;

	@Column(name = "batch_id")
	private Long batchId;

	@Column(nullable = false, precision = 14, scale = 3)
	private BigDecimal qty;

	@Column(nullable = false, precision = 14, scale = 4)
	private BigDecimal unitCost;

	@Enumerated(EnumType.STRING)
	@Column(name = "cost_center")
	private CostCenter costCenter;

	@Column(name = "taken_by_user_id")
	private Long takenByUserId;

	@Column(name = "resident_id")
	private Long residentId;

	@Transient
	private String productName;

	@Transient
	private String residentName;
}
