package com.dertz.spectra.repository;

import com.dertz.spectra.Enum.EntityStatus;
import com.dertz.spectra.model.Branch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BranchRepository extends JpaRepository<Branch, Long> {

	List<Branch> findAllByStatus(EntityStatus status);

	Optional<Branch> findByCodeIgnoreCase(String code);
}
