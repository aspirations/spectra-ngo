package com.dertz.spectra.repository;

import com.dertz.spectra.model.AppNotification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AppNotificationRepository extends JpaRepository<AppNotification, Long> {

	List<AppNotification> findByUserIdAndBranchIdOrderByCreatedDateDesc(Long userId, Long branchId);

	long countByUserIdAndBranchIdAndReadAtIsNull(Long userId, Long branchId);
}
