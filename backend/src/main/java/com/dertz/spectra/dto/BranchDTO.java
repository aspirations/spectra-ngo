package com.dertz.spectra.dto;

import com.dertz.spectra.Enum.EntityStatus;
import com.dertz.spectra.Enum.WarehouseType;
import com.dertz.spectra.model.Branch;
import com.dertz.spectra.model.Warehouse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BranchDTO {

	private Long id;
	private String name;
	private String code;
	private String address;
	private String city;
	private EntityStatus status;
	private List<WarehouseDTO> warehouses;

	public static BranchDTO from(Branch branch, List<Warehouse> warehouses) {
		return BranchDTO.builder()
				.id(branch.getId())
				.name(branch.getName())
				.code(branch.getCode())
				.address(branch.getAddress())
				.city(branch.getCity())
				.status(branch.getStatus())
				.warehouses(warehouses.stream().map(WarehouseDTO::from).toList())
				.build();
	}
}

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
class WarehouseDTO {

	private Long id;
	private WarehouseType type;
	private String name;

	static WarehouseDTO from(Warehouse warehouse) {
		return WarehouseDTO.builder()
				.id(warehouse.getId())
				.type(warehouse.getType())
				.name(warehouse.getName())
				.build();
	}
}
