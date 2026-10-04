package com.dertz.spectra.service;

import com.dertz.spectra.Enum.EntityStatus;
import com.dertz.spectra.Enum.Role;
import com.dertz.spectra.Enum.WarehouseType;
import com.dertz.spectra.dto.BranchDTO;
import com.dertz.spectra.dto.UserDTO;
import com.dertz.spectra.exception.BusinessException;
import com.dertz.spectra.exception.ResourceNotFoundException;
import com.dertz.spectra.model.Branch;
import com.dertz.spectra.model.TenantSettings;
import com.dertz.spectra.model.User;
import com.dertz.spectra.model.UserBranch;
import com.dertz.spectra.model.Warehouse;
import com.dertz.spectra.repository.BranchRepository;
import com.dertz.spectra.repository.GlobalUserLookup;
import com.dertz.spectra.repository.TenantSettingsRepository;
import com.dertz.spectra.repository.UserBranchRepository;
import com.dertz.spectra.repository.UserRepository;
import com.dertz.spectra.repository.WarehouseRepository;
import com.dertz.spectra.request.CreateBranchRequest;
import com.dertz.spectra.request.CreateUserRequest;
import com.dertz.spectra.request.UpdateUserRequest;
import com.dertz.spectra.security.BranchScope;
import com.dertz.spectra.security.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrgService {

	private final BranchRepository branchRepository;
	private final WarehouseRepository warehouseRepository;
	private final UserRepository userRepository;
	private final UserBranchRepository userBranchRepository;
	private final GlobalUserLookup globalUserLookup;
	private final TenantSettingsRepository tenantSettingsRepository;
	private final PasswordEncoder passwordEncoder;

	@Transactional
	public BranchDTO createBranch(CreateBranchRequest request) {
		Long tenantId = TenantContext.require().tenantId();
		branchRepository.findByCodeIgnoreCase(request.getCode()).ifPresent(existing -> {
			throw new BusinessException("Branch code already exists", "DUPLICATE_BRANCH");
		});
		Branch branch = Branch.builder()
				.tenantId(tenantId)
				.name(request.getName())
				.code(request.getCode().toUpperCase())
				.address(request.getAddress())
				.city(request.getCity())
				.status(request.getStatus() == null ? EntityStatus.ACTIVE : request.getStatus())
				.build();
		branch = branchRepository.save(branch);
		provisionWarehouses(tenantId, branch);
		Long branchId = branch.getId();
		for (User admin : userRepository.findByRole(Role.NGO_ADMIN)) {
			if (userBranchRepository.findByUserId(admin.getId()).stream().noneMatch(ub -> ub.getBranchId().equals(branchId))) {
				userBranchRepository.save(UserBranch.builder()
						.tenantId(tenantId)
						.userId(admin.getId())
						.branchId(branchId)
						.build());
			}
		}
		return toDto(branch);
	}

	@Transactional
	public void provisionWarehouses(Long tenantId, Branch branch) {
		warehouseRepository.save(Warehouse.builder()
				.tenantId(tenantId)
				.branchId(branch.getId())
				.type(WarehouseType.SHELTER_STORE)
				.name(branch.getName() + " Shelter Store")
				.build());
		warehouseRepository.save(Warehouse.builder()
				.tenantId(tenantId)
				.branchId(branch.getId())
				.type(WarehouseType.STAFF_RETAIL_STORE)
				.name(branch.getName() + " Staff Store")
				.build());
	}

	@Transactional(readOnly = true)
	public List<BranchDTO> listBranches() {
		if (TenantContext.require().role() == Role.PLATFORM_ADMIN) {
			return List.of();
		}
		if (TenantContext.require().role() == Role.NGO_ADMIN) {
			return branchRepository.findAll().stream().map(this::toDto).toList();
		}
		Set<Long> allowed = assignedBranchIds(TenantContext.require().userId());
		return branchRepository.findAll().stream()
				.filter(b -> allowed.contains(b.getId()))
				.map(this::toDto)
				.toList();
	}

	@Transactional
	public BranchDTO updateBranch(Long id, CreateBranchRequest request) {
		Branch branch = branchRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Branch not found"));
		branchRepository.findByCodeIgnoreCase(request.getCode()).ifPresent(existing -> {
			if (!existing.getId().equals(id)) {
				throw new BusinessException("Branch code already exists", "DUPLICATE_BRANCH");
			}
		});
		branch.setName(request.getName());
		branch.setCode(request.getCode().toUpperCase());
		branch.setAddress(request.getAddress());
		branch.setCity(request.getCity());
		if (request.getStatus() != null) {
			branch.setStatus(request.getStatus());
		}
		return toDto(branchRepository.save(branch));
	}

	@Transactional(readOnly = true)
	public BranchDTO getBranch(Long id) {
		Branch branch = branchRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Branch not found"));
		if (TenantContext.require().role() != Role.NGO_ADMIN
				&& !assignedBranchIds(TenantContext.require().userId()).contains(id)) {
			throw new BusinessException("No access to this centre", "FORBIDDEN", HttpStatus.FORBIDDEN);
		}
		return toDto(branch);
	}

	@Transactional
	public UserDTO createUser(CreateUserRequest request) {
		assertCanAssign(TenantContext.require().role(), request.getRole());
		Long tenantId = TenantContext.require().tenantId();
		if (globalUserLookup.emailTaken(request.getEmail())) {
			throw new BusinessException("Email already in use", "DUPLICATE_EMAIL");
		}
		List<Long> branchIds = resolveBranchIds(request.getRole(), request.getBranchIds(), request.getBranchId());
		assertActorCanGrant(branchIds);
		Long home = request.getRole() == Role.NGO_ADMIN ? null : (branchIds.isEmpty() ? null : branchIds.get(0));
		User user = User.builder()
				.tenantId(tenantId)
				.branchId(home)
				.email(request.getEmail().toLowerCase())
				.passwordHash(passwordEncoder.encode(request.getPassword()))
				.fullName(request.getFullName())
				.aadhaarNo(blankToNull(request.getAadhaarNo()))
				.homeAddress(blankToNull(request.getHomeAddress()))
				.familyDetails(blankToNull(request.getFamilyDetails()))
				.role(request.getRole())
				.creditLimit(nz(request.getCreditLimit()))
				.baseMonthlySalary(nz(request.getBaseMonthlySalary()))
				.otherFixedDeductions(nz(request.getOtherFixedDeductions()))
				.rolledOverStoreDebt(BigDecimal.ZERO)
				.status(request.getStatus() == null ? EntityStatus.ACTIVE : request.getStatus())
				.build();
		user = userRepository.save(user);
		replaceAssignments(user, branchIds);
		return toUserDto(user);
	}

	@Transactional(readOnly = true)
	public UserDTO getUser(Long id) {
		return toUserDto(requireVisibleUser(id));
	}

	@Transactional
	public UserDTO updateUser(Long id, UpdateUserRequest request) {
		User user = requireVisibleUser(id);
		if (request.getFullName() != null) {
			user.setFullName(request.getFullName());
		}
		if (request.getAadhaarNo() != null) {
			user.setAadhaarNo(blankToNull(request.getAadhaarNo()));
		}
		if (request.getHomeAddress() != null) {
			user.setHomeAddress(blankToNull(request.getHomeAddress()));
		}
		if (request.getFamilyDetails() != null) {
			user.setFamilyDetails(blankToNull(request.getFamilyDetails()));
		}
		if (request.getEmail() != null && !request.getEmail().isBlank()) {
			String email = request.getEmail().trim().toLowerCase();
			if (!email.equalsIgnoreCase(user.getEmail()) && globalUserLookup.emailTaken(email)) {
				throw new BusinessException("Email already in use", "DUPLICATE_EMAIL");
			}
			user.setEmail(email);
		}
		if (request.getPassword() != null && !request.getPassword().isBlank()) {
			user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
		}
		if (request.getCreditLimit() != null) {
			user.setCreditLimit(request.getCreditLimit());
		}
		if (request.getBaseMonthlySalary() != null) {
			user.setBaseMonthlySalary(request.getBaseMonthlySalary());
		}
		if (request.getOtherFixedDeductions() != null) {
			user.setOtherFixedDeductions(request.getOtherFixedDeductions());
		}
		if (request.getStatus() != null) {
			if (request.getStatus() == EntityStatus.INACTIVE && id.equals(TenantContext.require().userId())) {
				throw new BusinessException("You cannot deactivate your own login", "FORBIDDEN", HttpStatus.FORBIDDEN);
			}
			user.setStatus(request.getStatus());
		}
		if (request.getRole() != null) {
			assertCanAssign(TenantContext.require().role(), request.getRole());
			user.setRole(request.getRole());
		}
		List<Long> branchIds = request.getBranchIds();
		if (branchIds == null && request.getBranchId() != null) {
			branchIds = List.of(request.getBranchId());
		}
		if (user.getRole() == Role.NGO_ADMIN) {
			branchIds = allTenantBranchIds();
			user.setBranchId(null);
		} else if (branchIds != null) {
			assertActorCanGrant(branchIds);
			user.setBranchId(branchIds.isEmpty() ? user.getBranchId() : branchIds.get(0));
		} else if (request.getRole() != null && user.getBranchId() == null) {
			Long current = BranchScope.requireBranchId();
			user.setBranchId(current);
			branchIds = List.of(current);
		}
		user = userRepository.save(user);
		if (branchIds != null) {
			replaceAssignments(user, branchIds);
		}
		return toUserDto(user);
	}

	@Transactional(readOnly = true)
	public List<UserDTO> listUsers() {
		if (TenantContext.require().role() == Role.NGO_ADMIN) {
			return userRepository.findAll().stream().map(this::toUserDto).toList();
		}
		Long branchId = BranchScope.requireBranchId();
		Set<Long> userIds = userBranchRepository.findByBranchId(branchId).stream()
				.map(UserBranch::getUserId)
				.collect(Collectors.toSet());
		return userRepository.findAllById(userIds).stream().map(this::toUserDto).toList();
	}

	@Transactional(readOnly = true)
	public TenantSettings settings() {
		Long tenantId = TenantContext.require().tenantId();
		return tenantSettingsRepository.findByTenantId(tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Settings not found"));
	}

	@Transactional(readOnly = true)
	public UserDTO toUserDto(User user) {
		UserDTO dto = UserDTO.from(user);
		dto.setBranchIds(user.getRole() == Role.NGO_ADMIN ? allTenantBranchIds() : new ArrayList<>(assignedBranchIds(user.getId())));
		return dto;
	}

	private void replaceAssignments(User user, List<Long> branchIds) {
		userBranchRepository.deleteByUserId(user.getId());
		userBranchRepository.flush();
		for (Long branchId : new LinkedHashSet<>(branchIds)) {
			branchRepository.findById(branchId).orElseThrow(() -> new ResourceNotFoundException("Branch not found"));
			userBranchRepository.save(UserBranch.builder()
					.tenantId(user.getTenantId())
					.userId(user.getId())
					.branchId(branchId)
					.build());
		}
	}

	private List<Long> resolveBranchIds(Role role, List<Long> requested, Long single) {
		if (role == Role.NGO_ADMIN) {
			return allTenantBranchIds();
		}
		LinkedHashSet<Long> ids = new LinkedHashSet<>();
		if (requested != null) {
			ids.addAll(requested);
		}
		if (single != null) {
			ids.add(single);
		}
		if (ids.isEmpty()) {
			ids.add(BranchScope.requireBranchId());
		}
		return new ArrayList<>(ids);
	}

	private void assertActorCanGrant(List<Long> branchIds) {
		if (TenantContext.require().role() == Role.NGO_ADMIN) {
			return;
		}
		Set<Long> mine = assignedBranchIds(TenantContext.require().userId());
		if (!mine.containsAll(branchIds)) {
			throw new BusinessException("You can only grant centres you already have access to", "FORBIDDEN",
					HttpStatus.FORBIDDEN);
		}
	}

	private List<Long> allTenantBranchIds() {
		return branchRepository.findAll().stream().map(Branch::getId).toList();
	}

	private User requireVisibleUser(Long id) {
		User user = userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("User not found"));
		if (TenantContext.require().role() == Role.NGO_ADMIN) {
			return user;
		}
		Long branchId = BranchScope.requireBranchId();
		boolean visible = userBranchRepository.findByBranchId(branchId).stream()
				.anyMatch(ub -> ub.getUserId().equals(id));
		if (!visible) {
			throw new BusinessException("No access to this person", "FORBIDDEN", HttpStatus.FORBIDDEN);
		}
		return user;
	}

	private Set<Long> assignedBranchIds(Long userId) {
		return userBranchRepository.findByUserId(userId).stream()
				.map(UserBranch::getBranchId)
				.collect(Collectors.toSet());
	}

	private BranchDTO toDto(Branch branch) {
		return BranchDTO.from(branch, warehouseRepository.findByBranchId(branch.getId()));
	}

	private static void assertCanAssign(Role actor, Role target) {
		if (target == Role.PLATFORM_ADMIN) {
			throw new BusinessException("Platform admin cannot be assigned from an NGO", "FORBIDDEN", HttpStatus.FORBIDDEN);
		}
		if (actor == Role.BRANCH_ADMIN && target == Role.NGO_ADMIN) {
			throw new BusinessException("Only the NGO admin can create or promote an NGO admin", "FORBIDDEN",
					HttpStatus.FORBIDDEN);
		}
		if (actor == Role.INVENTORY_MANAGER && (target == Role.NGO_ADMIN || target == Role.PLATFORM_ADMIN)) {
			throw new BusinessException("Inventory manager cannot create NGO or platform admins", "FORBIDDEN",
					HttpStatus.FORBIDDEN);
		}
	}

	private static BigDecimal nz(BigDecimal value) {
		return value == null ? BigDecimal.ZERO : value;
	}

	private static String blankToNull(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}
}
