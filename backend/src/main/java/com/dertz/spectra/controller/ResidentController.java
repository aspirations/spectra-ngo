package com.dertz.spectra.controller;

import com.dertz.spectra.dto.CensusDTO;
import com.dertz.spectra.model.DogCaseNote;
import com.dertz.spectra.model.DogVaccination;
import com.dertz.spectra.model.Resident;
import com.dertz.spectra.request.NoteRequest;
import com.dertz.spectra.request.ResidentRequest;
import com.dertz.spectra.request.TreatmentDueRequest;
import com.dertz.spectra.request.VaccinationRequest;
import com.dertz.spectra.response.ApiResponse;
import com.dertz.spectra.service.ResidentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/residents")
@RequiredArgsConstructor
public class ResidentController extends BaseController {

	private final ResidentService residentService;

	@GetMapping
	@PreAuthorize("hasAnyRole('NGO_ADMIN','BRANCH_ADMIN','VET_TECH_EMPLOYEE','INVENTORY_MANAGER')")
	public ResponseEntity<ApiResponse<List<Resident>>> list() {
		return ok(residentService.list());
	}

	@GetMapping("/census")
	public ResponseEntity<ApiResponse<CensusDTO>> census() {
		return ok(residentService.census());
	}

	@GetMapping("/treatments/due")
	@PreAuthorize("hasAnyRole('NGO_ADMIN','BRANCH_ADMIN','VET_TECH_EMPLOYEE')")
	public ResponseEntity<ApiResponse<List<Map<String, Object>>>> due() {
		return ok(residentService.dueToday());
	}

	@GetMapping("/{id}")
	@PreAuthorize("hasAnyRole('NGO_ADMIN','BRANCH_ADMIN','VET_TECH_EMPLOYEE','INVENTORY_MANAGER')")
	public ResponseEntity<ApiResponse<Resident>> get(@PathVariable Long id) {
		return ok(residentService.get(id));
	}

	@PostMapping
	@PreAuthorize("hasAnyRole('NGO_ADMIN','BRANCH_ADMIN','VET_TECH_EMPLOYEE')")
	public ResponseEntity<ApiResponse<Resident>> create(@Valid @RequestBody ResidentRequest request) {
		return ok(residentService.create(request), "Dog created");
	}

	@PutMapping("/{id}")
	@PreAuthorize("hasAnyRole('NGO_ADMIN','BRANCH_ADMIN','VET_TECH_EMPLOYEE')")
	public ResponseEntity<ApiResponse<Resident>> update(@PathVariable Long id, @Valid @RequestBody ResidentRequest request) {
		return ok(residentService.update(id, request), "Dog updated");
	}

	@GetMapping("/{id}/notes")
	@PreAuthorize("hasAnyRole('NGO_ADMIN','BRANCH_ADMIN','VET_TECH_EMPLOYEE')")
	public ResponseEntity<ApiResponse<List<DogCaseNote>>> notes(@PathVariable Long id) {
		return ok(residentService.notes(id));
	}

	@PostMapping("/{id}/notes")
	@PreAuthorize("hasAnyRole('NGO_ADMIN','BRANCH_ADMIN','VET_TECH_EMPLOYEE')")
	public ResponseEntity<ApiResponse<DogCaseNote>> addNote(@PathVariable Long id, @Valid @RequestBody NoteRequest request) {
		return ok(residentService.addNote(id, request), "Note added");
	}

	@GetMapping("/{id}/treatments")
	@PreAuthorize("hasAnyRole('NGO_ADMIN','BRANCH_ADMIN','VET_TECH_EMPLOYEE','INVENTORY_MANAGER')")
	public ResponseEntity<ApiResponse<List<DogVaccination>>> treatments(@PathVariable Long id) {
		return ok(residentService.treatments(id));
	}

	@GetMapping("/{id}/timeline")
	@PreAuthorize("hasAnyRole('NGO_ADMIN','BRANCH_ADMIN','VET_TECH_EMPLOYEE','INVENTORY_MANAGER')")
	public ResponseEntity<ApiResponse<List<Map<String, Object>>>> timeline(@PathVariable Long id) {
		return ok(residentService.timeline(id));
	}

	@PostMapping("/{id}/treatments")
	@PreAuthorize("hasAnyRole('NGO_ADMIN','BRANCH_ADMIN','VET_TECH_EMPLOYEE','INVENTORY_MANAGER')")
	public ResponseEntity<ApiResponse<DogVaccination>> administer(@PathVariable Long id,
			@Valid @RequestBody VaccinationRequest request) {
		return ok(residentService.administer(id, request), "Requested from stores");
	}

	@PatchMapping("/{id}/treatments/{treatmentId}")
	@PreAuthorize("hasAnyRole('NGO_ADMIN','BRANCH_ADMIN','VET_TECH_EMPLOYEE')")
	public ResponseEntity<ApiResponse<DogVaccination>> updateNextDue(@PathVariable Long id,
			@PathVariable Long treatmentId, @RequestBody TreatmentDueRequest request) {
		return ok(residentService.updateNextDue(id, treatmentId, request.getNextDueDate()), "Next due updated");
	}
}
