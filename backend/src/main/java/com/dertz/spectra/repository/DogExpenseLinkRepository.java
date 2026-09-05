package com.dertz.spectra.repository;

import com.dertz.spectra.model.DogExpenseLink;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DogExpenseLinkRepository extends JpaRepository<DogExpenseLink, Long> {

	List<DogExpenseLink> findByBranchIdOrderByExpenseDateDesc(Long branchId);

	List<DogExpenseLink> findByResidentIdOrderByExpenseDateDesc(Long residentId);
}
