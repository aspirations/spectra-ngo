package com.dertz.spectra.repository;

import com.dertz.spectra.model.UserBranch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;

import java.util.List;

public interface UserBranchRepository extends JpaRepository<UserBranch, Long> {

	List<UserBranch> findByUserId(Long userId);

	List<UserBranch> findByBranchId(Long branchId);

	@Modifying
	void deleteByUserId(Long userId);
}
