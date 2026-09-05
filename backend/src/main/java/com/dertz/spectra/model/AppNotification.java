package com.dertz.spectra.model;

import com.dertz.spectra.Enum.NotificationEmailStatus;
import com.dertz.spectra.Enum.NotificationType;
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
@Table(name = "notifications")
@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class AppNotification extends Auditable {

	@TenantId
	@Column(name = "tenant_id", nullable = false)
	private Long tenantId;

	@Column(name = "branch_id", nullable = false)
	private Long branchId;

	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private NotificationType type;

	@Column(nullable = false)
	private String title;

	@Column(nullable = false, columnDefinition = "TEXT")
	private String body;

	private String entityType;

	private Long entityId;

	private Instant readAt;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private NotificationEmailStatus emailStatus;
}
