package com.dertz.spectra.dto;

import com.dertz.spectra.Enum.CareSection;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Map;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CensusDTO {

	private long totalActive;
	private Map<CareSection, Long> byCategory;
	private BigDecimal theoreticalFeedKg;
	private Integer adultGrams;
	private Integer juvenileGrams;
	private Integer postOpGrams;
	private Integer isolationGrams;
}
