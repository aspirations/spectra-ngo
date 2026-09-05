package com.dertz.spectra.model;

import com.dertz.spectra.Enum.LeaveStatus;
import com.dertz.spectra.Enum.LeaveType;
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
import java.time.LocalDate;

@Entity
@Table(name = "leave_requests")
@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class LeaveRequest extends Auditable {

	@TenantId
	@Column(name = "tenant_id", nullable = false)
	private Long tenantId;

	@Column(name = "branch_id", nullable = false)
	private Long branchId;

	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private LeaveType type;

	@Column(nullable = false)
	private LocalDate fromDate;

	@Column(nullable = false)
	private LocalDate toDate;

	@Column(nullable = false, precision = 6, scale = 1)
	private BigDecimal days;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private LeaveStatus status;

	@Column(columnDefinition = "TEXT")
	private String reason;

	private Long reviewedBy;
}
