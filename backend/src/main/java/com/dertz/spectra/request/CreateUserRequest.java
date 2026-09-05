package com.dertz.spectra.request;

import com.dertz.spectra.Enum.EntityStatus;
import com.dertz.spectra.Enum.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class CreateUserRequest {

	@NotBlank
	@Email
	private String email;

	@NotBlank
	private String password;

	@NotBlank
	private String fullName;

	private String aadhaarNo;

	private String homeAddress;

	private String familyDetails;

	@NotNull
	private Role role;

	private Long branchId;
	private List<Long> branchIds;
	private BigDecimal creditLimit;
	private BigDecimal baseMonthlySalary;
	private BigDecimal otherFixedDeductions;
	private EntityStatus status;
}
