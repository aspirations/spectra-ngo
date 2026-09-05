package com.dertz.spectra.repository;

import com.dertz.spectra.Enum.Role;
import com.dertz.spectra.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

	Optional<User> findByEmailIgnoreCase(String email);

	List<User> findByBranchId(Long branchId);

	List<User> findByRole(Role role);
}
