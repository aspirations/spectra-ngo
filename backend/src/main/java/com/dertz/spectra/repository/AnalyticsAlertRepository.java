package com.dertz.spectra.repository;

import com.dertz.spectra.Enum.AnalyticsAlertType;
import com.dertz.spectra.model.AnalyticsAlert;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AnalyticsAlertRepository extends JpaRepository<AnalyticsAlert, Long> {

	List<AnalyticsAlert> findByBranchIdOrderByDetectedAtDesc(Long branchId);

	boolean existsByBranchIdAndAlertTypeAndTitleAndAcknowledgedFalse(
			Long branchId, AnalyticsAlertType alertType, String title);
}
