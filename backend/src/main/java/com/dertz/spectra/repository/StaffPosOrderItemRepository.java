package com.dertz.spectra.repository;

import com.dertz.spectra.model.StaffPosOrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StaffPosOrderItemRepository extends JpaRepository<StaffPosOrderItem, Long> {

	List<StaffPosOrderItem> findByOrderId(Long orderId);
}
