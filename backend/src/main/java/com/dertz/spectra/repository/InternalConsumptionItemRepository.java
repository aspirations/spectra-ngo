package com.dertz.spectra.repository;

import com.dertz.spectra.model.InternalConsumptionItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InternalConsumptionItemRepository extends JpaRepository<InternalConsumptionItem, Long> {

	List<InternalConsumptionItem> findByConsumptionId(Long consumptionId);
}
