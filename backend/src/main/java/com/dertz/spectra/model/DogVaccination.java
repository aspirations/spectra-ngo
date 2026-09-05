package com.dertz.spectra.model;

import com.dertz.spectra.Enum.TreatmentStatus;
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
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "resident_treatments")
@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class DogVaccination extends Auditable {

	@TenantId
	@Column(name = "tenant_id", nullable = false)
	private Long tenantId;

	@Column(name = "resident_id", nullable = false)
	private Long residentId;

	@Column(name = "shelter_product_id", nullable = false)
	private Long shelterProductId;

	@Column(name = "consumption_item_id")
	private Long consumptionItemId;

	@Column(name = "consumption_id")
	private Long consumptionId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private TreatmentStatus status;

	private Instant administeredAt;

	private LocalDate nextDueDate;

	@Column(nullable = false, precision = 12, scale = 3)
	private BigDecimal quantity;

	@Column(columnDefinition = "TEXT")
	private String notes;
}
