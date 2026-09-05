package com.dertz.spectra.model;

import com.dertz.spectra.Enum.AnalyticsAlertType;
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

import java.time.Instant;

@Entity
@Table(name = "analytics_alerts")
@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class AnalyticsAlert extends Auditable {

	@TenantId
	@Column(name = "tenant_id", nullable = false)
	private Long tenantId;

	@Column(name = "branch_id", nullable = false)
	private Long branchId;

	@Enumerated(EnumType.STRING)
	@Column(name = "alert_type", nullable = false)
	private AnalyticsAlertType alertType;

	@Column(nullable = false)
	private String severity;

	@Column(nullable = false)
	private String title;

	@Column(nullable = false, length = 1000)
	private String detail;

	@Column(name = "detected_at", nullable = false)
	private Instant detectedAt;

	@Column(nullable = false)
	private boolean acknowledged;
}
