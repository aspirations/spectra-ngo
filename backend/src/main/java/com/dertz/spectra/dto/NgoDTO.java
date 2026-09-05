package com.dertz.spectra.dto;

import com.dertz.spectra.Enum.EntityStatus;
import com.dertz.spectra.model.Tenant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NgoDTO {

	private Long id;
	private String name;
	private String code;
	private String timezone;
	private String currency;
	private EntityStatus status;
	private String adminEmail;

	public static NgoDTO from(Tenant tenant) {
		return from(tenant, null);
	}

	public static NgoDTO from(Tenant tenant, String adminEmail) {
		return NgoDTO.builder()
				.id(tenant.getId())
				.name(tenant.getName())
				.code(tenant.getCode())
				.timezone(tenant.getTimezone())
				.currency(tenant.getCurrency())
				.status(tenant.getStatus())
				.adminEmail(adminEmail)
				.build();
	}
}
