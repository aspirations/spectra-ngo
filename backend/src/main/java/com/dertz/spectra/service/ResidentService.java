package com.dertz.spectra.service;

import com.dertz.spectra.Enum.CareKind;
import com.dertz.spectra.Enum.CareSection;
import com.dertz.spectra.Enum.CareStatus;
import com.dertz.spectra.Enum.ConsumptionType;
import com.dertz.spectra.Enum.CostCenter;
import com.dertz.spectra.Enum.TreatmentStatus;
import com.dertz.spectra.dto.CensusDTO;
import com.dertz.spectra.exception.BusinessException;
import com.dertz.spectra.exception.ResourceNotFoundException;
import com.dertz.spectra.model.DogCaseNote;
import com.dertz.spectra.model.DogExpenseLink;
import com.dertz.spectra.model.DogVaccination;
import com.dertz.spectra.model.InternalConsumption;
import com.dertz.spectra.model.Resident;
import com.dertz.spectra.model.ShelterProduct;
import com.dertz.spectra.model.TenantSettings;
import com.dertz.spectra.repository.DogCaseNoteRepository;
import com.dertz.spectra.repository.DogExpenseLinkRepository;
import com.dertz.spectra.repository.DogVaccinationRepository;
import com.dertz.spectra.repository.ResidentRepository;
import com.dertz.spectra.repository.ShelterProductRepository;
import com.dertz.spectra.repository.TenantSettingsRepository;
import com.dertz.spectra.request.ConsumeRequest;
import com.dertz.spectra.request.NoteRequest;
import com.dertz.spectra.request.ResidentRequest;
import com.dertz.spectra.request.VaccinationRequest;
import com.dertz.spectra.security.BranchScope;
import com.dertz.spectra.security.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ResidentService {

	private final ResidentRepository residentRepository;
	private final DogCaseNoteRepository dogCaseNoteRepository;
	private final DogVaccinationRepository dogVaccinationRepository;
	private final DogExpenseLinkRepository dogExpenseLinkRepository;
	private final ShelterProductRepository shelterProductRepository;
	private final TenantSettingsRepository tenantSettingsRepository;
	private final InventoryService inventoryService;

	@Transactional(readOnly = true)
	public List<Resident> list() {
		return residentRepository.findByBranchIdOrderByNameAsc(BranchScope.requireBranchId());
	}

	@Transactional(readOnly = true)
	public Resident get(Long id) {
		return residentRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Resident not found"));
	}

	@Transactional
	public Resident create(ResidentRequest request) {
		Long tenantId = TenantContext.require().tenantId();
		Long branchId = BranchScope.requireBranchId();
		Resident resident = Resident.builder()
				.tenantId(tenantId)
				.branchId(branchId)
				.status(request.getStatus() == null ? CareStatus.ACTIVE : request.getStatus())
				.build();
		applyIntake(resident, request);
		return residentRepository.save(resident);
	}

	@Transactional
	public Resident update(Long id, ResidentRequest request) {
		Resident resident = get(id);
		applyIntake(resident, request);
		if (request.getStatus() != null) {
			resident.setStatus(request.getStatus());
		}
		return residentRepository.save(resident);
	}

	private static void applyIntake(Resident resident, ResidentRequest request) {
		resident.setReferenceId(request.getReferenceId().trim());
		resident.setName(request.getName().trim());
		resident.setPhotoUrl(blankToNull(request.getPhotoUrl()));
		resident.setCollarNo(blankToNull(request.getCollarNo()));
		resident.setDescription(blankToNull(request.getDescription()));
		resident.setSex(request.getSex());
		resident.setColor(blankToNull(request.getColor()));
		resident.setApproxAge(blankToNull(request.getApproxAge()));
		resident.setIntakeSource(request.getIntakeSource());
		resident.setKind(CareKind.DOG);
		resident.setCategory(request.getCategory());
		resident.setIntakeDate(request.getIntakeDate());
	}

	private static String blankToNull(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}

	@Transactional
	public DogCaseNote addNote(Long residentId, NoteRequest request) {
		get(residentId);
		DogCaseNote note = DogCaseNote.builder()
				.tenantId(TenantContext.require().tenantId())
				.residentId(residentId)
				.noteText(request.getNoteText())
				.noteType(request.getNoteType())
				.build();
		return dogCaseNoteRepository.save(note);
	}

	@Transactional(readOnly = true)
	public List<DogCaseNote> notes(Long residentId) {
		get(residentId);
		return dogCaseNoteRepository.findByResidentIdOrderByCreatedDateDesc(residentId);
	}

	@Transactional
	public DogVaccination administer(Long residentId, VaccinationRequest request) {
		Resident resident = get(residentId);
		ShelterProduct product = shelterProductRepository.findById(request.getShelterProductId())
				.orElseThrow(() -> new ResourceNotFoundException("Product not found"));
		BigDecimal qty = request.getQuantity() == null ? BigDecimal.ONE : request.getQuantity();
		ConsumeRequest consume = new ConsumeRequest();
		consume.setType(ConsumptionType.VACCINE_ADMIN);
		consume.setNotes("Treatment for " + resident.getName());
		ConsumeRequest.Item item = new ConsumeRequest.Item();
		item.setShelterProductId(product.getId());
		item.setQuantity(qty);
		item.setCostCenter(CostCenter.CLINICAL_TREATMENT);
		item.setResidentId(residentId);
		consume.setItems(List.of(item));
		InternalConsumption header = inventoryService.requestConsume(consume).consumption();
		DogVaccination treatment = DogVaccination.builder()
				.tenantId(TenantContext.require().tenantId())
				.residentId(residentId)
				.shelterProductId(product.getId())
				.consumptionId(header.getId())
				.status(TreatmentStatus.PENDING_ISSUE)
				.quantity(qty)
				.notes(request.getNotes())
				.nextDueDate(request.getNextDueDate())
				.build();
		return dogVaccinationRepository.save(treatment);
	}

	@Transactional
	public DogVaccination updateNextDue(Long residentId, Long treatmentId, LocalDate nextDueDate) {
		get(residentId);
		DogVaccination tx = dogVaccinationRepository.findById(treatmentId)
				.orElseThrow(() -> new ResourceNotFoundException("Treatment not found"));
		if (!tx.getResidentId().equals(residentId)) {
			throw new ResourceNotFoundException("Treatment not found");
		}
		if (tx.getStatus() == TreatmentStatus.CANCELLED) {
			throw new BusinessException("Cannot set next due on a cancelled treatment");
		}
		tx.setNextDueDate(nextDueDate);
		return dogVaccinationRepository.save(tx);
	}

	@Transactional(readOnly = true)
	public List<DogVaccination> treatments(Long residentId) {
		return dogVaccinationRepository.findByResidentIdOrderByIdDesc(residentId);
	}

	@Transactional(readOnly = true)
	public List<Map<String, Object>> timeline(Long residentId) {
		get(residentId);
		List<Map<String, Object>> rows = new java.util.ArrayList<>();
		for (DogCaseNote note : dogCaseNoteRepository.findByResidentIdOrderByCreatedDateDesc(residentId)) {
			Map<String, Object> row = new java.util.LinkedHashMap<>();
			row.put("kind", "NOTE");
			row.put("at", note.getCreatedDate() == null ? "" : note.getCreatedDate().toInstant().toString());
			row.put("title", note.getNoteType() == null ? "NOTE" : note.getNoteType().name());
			row.put("detail", note.getNoteText());
			row.put("amount", null);
			rows.add(row);
		}
		for (DogVaccination tx : dogVaccinationRepository.findByResidentIdOrderByIdDesc(residentId)) {
			ShelterProduct product = shelterProductRepository.findById(tx.getShelterProductId()).orElse(null);
			Map<String, Object> row = new java.util.LinkedHashMap<>();
			row.put("kind", "TREATMENT");
			row.put("at", tx.getAdministeredAt() == null
					? (tx.getCreatedDate() == null ? "" : tx.getCreatedDate().toInstant().toString())
					: tx.getAdministeredAt().toString());
			row.put("title", product == null ? "Treatment" : product.getName());
			String notes = tx.getNotes() == null ? "" : " · " + tx.getNotes();
			if (tx.getStatus() == TreatmentStatus.PENDING_ISSUE) {
				String due = tx.getNextDueDate() == null ? "" : " · next due " + tx.getNextDueDate();
				row.put("detail", "Waiting for stores" + due + notes);
			} else if (tx.getStatus() == TreatmentStatus.CANCELLED) {
				row.put("detail", "Cancelled" + notes);
			} else {
				String due = tx.getNextDueDate() == null ? "" : "Next due " + tx.getNextDueDate();
				row.put("detail", (due.isBlank() ? "Issued" : due) + notes);
			}
			row.put("amount", null);
			rows.add(row);
		}
		for (DogExpenseLink link : dogExpenseLinkRepository.findByResidentIdOrderByExpenseDateDesc(residentId)) {
			Map<String, Object> row = new java.util.LinkedHashMap<>();
			row.put("kind", "EXPENSE");
			row.put("at", link.getExpenseDate().toString());
			row.put("title", link.getLabel());
			row.put("detail", "Linked med/food cost");
			row.put("amount", link.getAmount());
			rows.add(row);
		}
		rows.sort((a, b) -> String.valueOf(b.get("at")).compareTo(String.valueOf(a.get("at"))));
		return rows;
	}

	@Transactional(readOnly = true)
	public List<Map<String, Object>> dueToday() {
		Long branchId = BranchScope.requireBranchId();
		LocalDate today = LocalDate.now();
		return dogVaccinationRepository.findByNextDueDateLessThanEqual(today).stream()
				.filter(v -> {
					if (v.getStatus() != TreatmentStatus.ISSUED || v.getNextDueDate() == null) {
						return false;
					}
					Resident resident = residentRepository.findById(v.getResidentId()).orElse(null);
					return resident != null && resident.getBranchId().equals(branchId)
							&& resident.getStatus() == CareStatus.ACTIVE;
				})
				.map(v -> {
					Resident resident = residentRepository.findById(v.getResidentId()).orElseThrow();
					ShelterProduct product = shelterProductRepository.findById(v.getShelterProductId()).orElse(null);
					return Map.<String, Object>of(
							"treatmentId", v.getId(),
							"residentId", resident.getId(),
							"residentName", resident.getName(),
							"referenceId", resident.getReferenceId(),
							"productName", product == null ? "" : product.getName(),
							"nextDueDate", v.getNextDueDate().toString());
				})
				.toList();
	}

	@Transactional(readOnly = true)
	public CensusDTO census() {
		return census(BranchScope.requireBranchId());
	}

	@Transactional(readOnly = true)
	public CensusDTO census(Long branchId) {
		List<Resident> active = residentRepository.findByBranchIdAndStatus(branchId, CareStatus.ACTIVE);
		Map<CareSection, Long> byCategory = new EnumMap<>(CareSection.class);
		for (CareSection section : CareSection.values()) {
			byCategory.put(section, 0L);
		}
		for (Resident resident : active) {
			byCategory.merge(resident.getCategory(), 1L, Long::sum);
		}
		TenantSettings settings = tenantSettingsRepository.findByTenantId(TenantContext.require().tenantId())
				.orElseThrow(() -> new ResourceNotFoundException("Settings not found"));
		long grams = byCategory.get(CareSection.ADULT) * settings.getAdultFeedGramsPerDay()
				+ byCategory.get(CareSection.JUVENILE) * settings.getJuvenileFeedGramsPerDay()
				+ byCategory.get(CareSection.POST_OP) * settings.getPostOpFeedGramsPerDay()
				+ byCategory.get(CareSection.CRITICAL) * settings.getIsolationFeedGramsPerDay();
		return CensusDTO.builder()
				.totalActive(active.size())
				.byCategory(byCategory)
				.theoreticalFeedKg(BigDecimal.valueOf(grams).divide(BigDecimal.valueOf(1000), 3, RoundingMode.HALF_UP))
				.adultGrams(settings.getAdultFeedGramsPerDay())
				.juvenileGrams(settings.getJuvenileFeedGramsPerDay())
				.postOpGrams(settings.getPostOpFeedGramsPerDay())
				.isolationGrams(settings.getIsolationFeedGramsPerDay())
				.build();
	}
}
