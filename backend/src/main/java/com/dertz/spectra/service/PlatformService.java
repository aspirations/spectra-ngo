package com.dertz.spectra.service;

import com.dertz.spectra.Enum.EntityStatus;
import com.dertz.spectra.Enum.Role;
import com.dertz.spectra.Enum.WorkingDaysMode;
import com.dertz.spectra.dto.NgoDTO;
import com.dertz.spectra.exception.BusinessException;
import com.dertz.spectra.exception.ResourceNotFoundException;
import com.dertz.spectra.model.Branch;
import com.dertz.spectra.model.Tenant;
import com.dertz.spectra.model.TenantSettings;
import com.dertz.spectra.model.User;
import com.dertz.spectra.model.UserBranch;
import com.dertz.spectra.repository.BranchRepository;
import com.dertz.spectra.repository.GlobalUserLookup;
import com.dertz.spectra.repository.TenantRepository;
import com.dertz.spectra.repository.TenantSettingsRepository;
import com.dertz.spectra.repository.UserBranchRepository;
import com.dertz.spectra.repository.UserRepository;
import com.dertz.spectra.request.CreateNgoRequest;
import com.dertz.spectra.security.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class PlatformService {

	public static final String PLATFORM_CODE = "PLATFORM";

	private final TenantRepository tenantRepository;
	private final TenantSettingsRepository tenantSettingsRepository;
	private final BranchRepository branchRepository;
	private final UserRepository userRepository;
	private final UserBranchRepository userBranchRepository;
	private final GlobalUserLookup globalUserLookup;
	private final PasswordEncoder passwordEncoder;
	private final OrgService orgService;
	private final PlatformTransactionManager transactionManager;

	@Transactional(readOnly = true)
	public List<NgoDTO> listNgos() {
		assertPlatform();
		return tenantRepository.findAll().stream()
				.filter(t -> !PLATFORM_CODE.equalsIgnoreCase(t.getCode()))
				.map(NgoDTO::from)
				.toList();
	}

	public NgoDTO createNgo(CreateNgoRequest request) {
		assertPlatform();
		String code = request.getCode().trim().toUpperCase(Locale.ROOT);
		if (PLATFORM_CODE.equals(code)) {
			throw new BusinessException("Reserved organisation code", "DUPLICATE_TENANT");
		}
		tenantRepository.findByCodeIgnoreCase(code).ifPresent(existing -> {
			throw new BusinessException("Organisation code already exists", "DUPLICATE_TENANT");
		});
		if (globalUserLookup.emailTaken(request.getAdminEmail())) {
			throw new BusinessException("Email already in use", "DUPLICATE_EMAIL");
		}
		TransactionTemplate tx = new TransactionTemplate(transactionManager);
		Long tenantId = tx.execute(status -> tenantRepository.save(Tenant.builder()
				.name(request.getName().trim())
				.code(code)
				.timezone(blankTo(request.getTimezone(), "Asia/Kolkata"))
				.currency(blankTo(request.getCurrency(), "INR"))
				.status(EntityStatus.ACTIVE)
				.build()).getId());
		if (tenantId == null) {
			throw new IllegalStateException("Failed to create organisation");
		}
		TenantContext.Snapshot previous = TenantContext.require();
		String adminEmail = request.getAdminEmail().trim().toLowerCase(Locale.ROOT);
		TransactionTemplate inner = new TransactionTemplate(transactionManager);
		inner.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
		TenantContext.set(new TenantContext.Snapshot(tenantId, null, previous.userId(), Role.NGO_ADMIN, adminEmail,
				code, request.getAdminName().trim()));
		try {
			inner.executeWithoutResult(status -> provisionNgo(tenantId, request, adminEmail));
		} catch (RuntimeException ex) {
			TenantContext.set(previous);
			tenantRepository.findById(tenantId).ifPresent(tenantRepository::delete);
			throw ex;
		} finally {
			TenantContext.set(previous);
		}
		Tenant tenant = tenantRepository.findById(tenantId).orElseThrow();
		return NgoDTO.from(tenant, adminEmail);
	}

	@Transactional
	public NgoDTO setStatus(Long id, EntityStatus status) {
		assertPlatform();
		Tenant tenant = tenantRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Organisation not found"));
		if (PLATFORM_CODE.equalsIgnoreCase(tenant.getCode())) {
			throw new BusinessException("The platform tenant cannot be changed", "FORBIDDEN", HttpStatus.FORBIDDEN);
		}
		tenant.setStatus(status);
		return NgoDTO.from(tenantRepository.save(tenant));
	}

	private void provisionNgo(Long tenantId, CreateNgoRequest request, String adminEmail) {
		tenantSettingsRepository.save(TenantSettings.builder()
				.tenantId(tenantId)
				.adultFeedGramsPerDay(400)
				.juvenileFeedGramsPerDay(200)
				.postOpFeedGramsPerDay(400)
				.isolationFeedGramsPerDay(400)
				.workingDaysMode(WorkingDaysMode.EXCLUDE_SUNDAYS)
				.defaultVaccineIntervalDays(365)
				.build());
		String branchName = blankTo(request.getFirstBranchName(), "Main Centre");
		String branchCode = blankTo(request.getFirstBranchCode(), "MAIN").toUpperCase(Locale.ROOT);
		Branch branch = branchRepository.save(Branch.builder()
				.tenantId(tenantId)
				.name(branchName)
				.code(branchCode)
				.status(EntityStatus.ACTIVE)
				.build());
		orgService.provisionWarehouses(tenantId, branch);
		User admin = userRepository.save(User.builder()
				.tenantId(tenantId)
				.branchId(null)
				.email(adminEmail)
				.passwordHash(passwordEncoder.encode(request.getAdminPassword()))
				.fullName(request.getAdminName().trim())
				.role(Role.NGO_ADMIN)
				.creditLimit(BigDecimal.ZERO)
				.baseMonthlySalary(BigDecimal.ZERO)
				.otherFixedDeductions(BigDecimal.ZERO)
				.rolledOverStoreDebt(BigDecimal.ZERO)
				.status(EntityStatus.ACTIVE)
				.build());
		userBranchRepository.save(UserBranch.builder()
				.tenantId(tenantId)
				.userId(admin.getId())
				.branchId(branch.getId())
				.build());
	}

	private static void assertPlatform() {
		if (TenantContext.require().role() != Role.PLATFORM_ADMIN) {
			throw new BusinessException("Only the platform admin can manage organisations", "FORBIDDEN",
					HttpStatus.FORBIDDEN);
		}
	}

	private static String blankTo(String value, String fallback) {
		return value == null || value.isBlank() ? fallback : value.trim();
	}
}
