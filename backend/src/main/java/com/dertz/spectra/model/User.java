package com.dertz.spectra.model;

import com.dertz.spectra.Enum.EntityStatus;
import com.dertz.spectra.Enum.Role;
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
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;

@Entity
@Table(name = "users")
@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class User extends Auditable implements UserDetails {

	@TenantId
	@Column(name = "tenant_id", nullable = false)
	private Long tenantId;

	@Column(name = "branch_id")
	private Long branchId;

	@Column(nullable = false)
	private String email;

	@Column(name = "password_hash", nullable = false)
	private String passwordHash;

	@Column(nullable = false)
	private String fullName;

	private String aadhaarNo;

	@Column(columnDefinition = "TEXT")
	private String homeAddress;

	@Column(columnDefinition = "TEXT")
	private String familyDetails;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private Role role;

	@Column(nullable = false, precision = 12, scale = 2)
	private BigDecimal creditLimit;

	@Column(nullable = false, precision = 12, scale = 2)
	private BigDecimal baseMonthlySalary;

	@Column(nullable = false, precision = 12, scale = 2)
	private BigDecimal otherFixedDeductions;

	@Column(nullable = false, precision = 12, scale = 2)
	private BigDecimal rolledOverStoreDebt;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private EntityStatus status;

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
	}

	@Override
	public String getPassword() {
		return passwordHash;
	}

	@Override
	public String getUsername() {
		return email;
	}

	@Override
	public boolean isAccountNonExpired() {
		return status == EntityStatus.ACTIVE;
	}

	@Override
	public boolean isAccountNonLocked() {
		return status == EntityStatus.ACTIVE;
	}

	@Override
	public boolean isCredentialsNonExpired() {
		return true;
	}

	@Override
	public boolean isEnabled() {
		return status == EntityStatus.ACTIVE;
	}
}
