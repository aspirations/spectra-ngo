package com.dertz.spectra.request;

import com.dertz.spectra.Enum.Role;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
public class PoSettingsRequest {

	private Role poApproverRole;
	private Long poApproverUserId;
	private BigDecimal poQtyTolerancePct;
	private BigDecimal poCostTolerancePct;
	private Boolean poNotifyOnSubmit;
	private Boolean poNotifyOnApprove;
	private Boolean poNotifyOnVariance;
}
