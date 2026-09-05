package com.dertz.spectra.model;

import com.dertz.spectra.Enum.Role;
import com.dertz.spectra.Enum.WorkingDaysMode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.TenantId;

import java.math.BigDecimal;

@Entity
@Table(name = "tenant_settings")
@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class TenantSettings extends Auditable {

	@TenantId
	@Column(name = "tenant_id", nullable = false)
	private Long tenantId;

	@Column(nullable = false)
	private Integer adultFeedGramsPerDay;

	@Column(nullable = false)
	private Integer juvenileFeedGramsPerDay;

	@Column(nullable = false)
	private Integer postOpFeedGramsPerDay;

	@Column(nullable = false)
	private Integer isolationFeedGramsPerDay;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private WorkingDaysMode workingDaysMode;

	@Column(nullable = false)
	private Integer defaultVaccineIntervalDays;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private Role poApproverRole;

	@Column(name = "po_approver_user_id")
	private Long poApproverUserId;

	@Column(nullable = false, precision = 8, scale = 4)
	private BigDecimal poQtyTolerancePct;

	@Column(nullable = false, precision = 8, scale = 4)
	private BigDecimal poCostTolerancePct;

	@Column(nullable = false)
	private Boolean poNotifyOnSubmit;

	@Column(nullable = false)
	private Boolean poNotifyOnApprove;

	@Column(nullable = false)
	private Boolean poNotifyOnVariance;

	@PrePersist
	@PreUpdate
	void poDefaults() {
		if (poApproverRole == null) {
			poApproverRole = Role.BRANCH_ADMIN;
		}
		if (poQtyTolerancePct == null) {
			poQtyTolerancePct = BigDecimal.ZERO;
		}
		if (poCostTolerancePct == null) {
			poCostTolerancePct = BigDecimal.ZERO;
		}
		if (poNotifyOnSubmit == null) {
			poNotifyOnSubmit = true;
		}
		if (poNotifyOnApprove == null) {
			poNotifyOnApprove = true;
		}
		if (poNotifyOnVariance == null) {
			poNotifyOnVariance = true;
		}
	}
}
