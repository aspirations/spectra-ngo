package com.dertz.spectra.repository;

import com.dertz.spectra.model.DogVaccination;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface DogVaccinationRepository extends JpaRepository<DogVaccination, Long> {

	List<DogVaccination> findByResidentIdOrderByIdDesc(Long residentId);

	List<DogVaccination> findByNextDueDateLessThanEqual(LocalDate date);

	List<DogVaccination> findByConsumptionId(Long consumptionId);
}
