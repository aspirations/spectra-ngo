package com.dertz.spectra.model;

import com.dertz.spectra.Enum.EntityStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.TenantId;

@Entity
@Table(name = "branches")
@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class Branch extends Auditable {

	@TenantId
	@Column(name = "tenant_id", nullable = false)
	private Long tenantId;

	@Column(nullable = false)
	private String name;

	@Column(nullable = false)
	private String code;

	private String address;

	private String city;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private EntityStatus status;

	@ManyToOne(fetch = FetchType.LAZY)
	@jakarta.persistence.JoinColumn(name = "tenant_id", insertable = false, updatable = false)
	private Tenant tenant;
}
