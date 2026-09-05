package com.dertz.spectra.request;

import com.dertz.spectra.Enum.EntityStatus;
import com.dertz.spectra.Enum.Role;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class UpdateUserRequest {

	private String fullName;
	private String aadhaarNo;
	private String homeAddress;
	private String familyDetails;
	private String email;
	private String password;
	private Long branchId;
	private List<Long> branchIds;
	private BigDecimal creditLimit;
	private BigDecimal baseMonthlySalary;
	private BigDecimal otherFixedDeductions;
	private EntityStatus status;
	private Role role;
}
