package com.dertz.spectra.repository;

import com.dertz.spectra.model.DogCaseNote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DogCaseNoteRepository extends JpaRepository<DogCaseNote, Long> {

	List<DogCaseNote> findByResidentIdOrderByCreatedDateDesc(Long residentId);

	List<DogCaseNote> findByCreatedBy(Long createdBy);
}
