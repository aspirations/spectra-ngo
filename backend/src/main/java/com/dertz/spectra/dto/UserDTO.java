package com.dertz.spectra.dto;

import com.dertz.spectra.Enum.EntityStatus;
import com.dertz.spectra.Enum.Role;
import com.dertz.spectra.model.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDTO {

	private Long id;
	private Long tenantId;
	private Long branchId;
	private List<Long> branchIds;
	private String email;
	private String fullName;
	private String aadhaarNo;
	private String homeAddress;
	private String familyDetails;
	private Role role;
	private BigDecimal creditLimit;
	private BigDecimal baseMonthlySalary;
	private BigDecimal otherFixedDeductions;
	private BigDecimal rolledOverStoreDebt;
	private EntityStatus status;
	private String tenantCode;
	private String tenantName;

	public static UserDTO from(User user) {
		return UserDTO.builder()
				.id(user.getId())
				.tenantId(user.getTenantId())
				.branchId(user.getBranchId())
				.email(user.getEmail())
				.fullName(user.getFullName())
				.aadhaarNo(user.getAadhaarNo())
				.homeAddress(user.getHomeAddress())
				.familyDetails(user.getFamilyDetails())
				.role(user.getRole())
				.creditLimit(user.getCreditLimit())
				.baseMonthlySalary(user.getBaseMonthlySalary())
				.otherFixedDeductions(user.getOtherFixedDeductions())
				.rolledOverStoreDebt(user.getRolledOverStoreDebt())
				.status(user.getStatus())
				.build();
	}
}
