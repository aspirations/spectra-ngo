package com.dertz.spectra.config;

import com.dertz.spectra.Enum.CareKind;
import com.dertz.spectra.Enum.CareSection;
import com.dertz.spectra.Enum.CareStatus;
import com.dertz.spectra.Enum.EntityStatus;
import com.dertz.spectra.Enum.PosOrderStatus;
import com.dertz.spectra.Enum.PosTender;
import com.dertz.spectra.Enum.Role;
import com.dertz.spectra.Enum.ShelterProductCategory;
import com.dertz.spectra.Enum.UnitOfMeasure;
import com.dertz.spectra.Enum.WorkingDaysMode;
import com.dertz.spectra.model.Branch;
import com.dertz.spectra.model.Resident;
import com.dertz.spectra.model.ShelterProduct;
import com.dertz.spectra.model.StaffPosOrder;
import com.dertz.spectra.model.StaffPosOrderItem;
import com.dertz.spectra.model.Supplier;
import com.dertz.spectra.model.Tenant;
import com.dertz.spectra.model.TenantSettings;
import com.dertz.spectra.model.User;
import com.dertz.spectra.model.UserBranch;
import com.dertz.spectra.repository.BranchRepository;
import com.dertz.spectra.repository.ResidentRepository;
import com.dertz.spectra.repository.ShelterProductRepository;
import com.dertz.spectra.repository.StaffPosOrderItemRepository;
import com.dertz.spectra.repository.StaffPosOrderRepository;
import com.dertz.spectra.repository.SupplierRepository;
import com.dertz.spectra.repository.TenantRepository;
import com.dertz.spectra.repository.TenantSettingsRepository;
import com.dertz.spectra.repository.UserBranchRepository;
import com.dertz.spectra.repository.UserRepository;
import com.dertz.spectra.request.GrnRequest;
import com.dertz.spectra.security.TenantContext;
import com.dertz.spectra.service.InventoryService;
import com.dertz.spectra.service.OrgService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder implements ApplicationRunner {

	private final Environment environment;
	private final TenantRepository tenantRepository;
	private final TenantSettingsRepository tenantSettingsRepository;
	private final BranchRepository branchRepository;
	private final UserRepository userRepository;
	private final UserBranchRepository userBranchRepository;
	private final OrgService orgService;
	private final PasswordEncoder passwordEncoder;
	private final ShelterProductRepository shelterProductRepository;
	private final SupplierRepository supplierRepository;
	private final InventoryService inventoryService;
	private final ResidentRepository residentRepository;
	private final StaffPosOrderRepository staffPosOrderRepository;
	private final StaffPosOrderItemRepository staffPosOrderItemRepository;
	private final PlatformTransactionManager transactionManager;

	@Override
	public void run(ApplicationArguments args) {
		if (!environment.getProperty("application.seed.enabled", Boolean.class, true)) {
			return;
		}
		seedPlatform();
		if (tenantRepository.findByCodeIgnoreCase("SPECTRA").isPresent()) {
			return;
		}
		TransactionTemplate tx = new TransactionTemplate(transactionManager);
		Long tenantId = tx.execute(status -> tenantRepository.save(Tenant.builder()
				.name("Spectra NGO")
				.code("SPECTRA")
				.timezone("Asia/Kolkata")
				.currency("INR")
				.status(EntityStatus.ACTIVE)
				.build()).getId());
		if (tenantId == null) {
			throw new IllegalStateException("Failed to create demo tenant");
		}
		TenantContext.set(new TenantContext.Snapshot(tenantId, null, 0L, Role.NGO_ADMIN, "seed", "SPECTRA", "Seeder"));
		try {
			tx.executeWithoutResult(status -> seed(tenantId));
		} finally {
			TenantContext.clear();
		}
	}

	private void seedPlatform() {
		if (tenantRepository.findByCodeIgnoreCase("PLATFORM").isPresent()) {
			return;
		}
		TransactionTemplate tx = new TransactionTemplate(transactionManager);
		Long tenantId = tx.execute(status -> tenantRepository.save(Tenant.builder()
				.name("Spectra Platform")
				.code("PLATFORM")
				.timezone("Asia/Kolkata")
				.currency("INR")
				.status(EntityStatus.ACTIVE)
				.build()).getId());
		if (tenantId == null) {
			throw new IllegalStateException("Failed to create platform tenant");
		}
		TenantContext.set(new TenantContext.Snapshot(tenantId, null, 0L, Role.PLATFORM_ADMIN, "seed", "PLATFORM",
				"Seeder"));
		try {
			tx.executeWithoutResult(status -> {
				String password = passwordEncoder.encode("Spectra@123");
				saveUser(tenantId, null, "platform@spectra.org", password, "Platform Admin", Role.PLATFORM_ADMIN,
						BigDecimal.ZERO, BigDecimal.ZERO);
			});
			log.info("Seeded platform admin platform@spectra.org password Spectra@123");
		} finally {
			TenantContext.clear();
		}
	}

	private void seed(Long tenantId) {
		tenantSettingsRepository.save(TenantSettings.builder()
					.tenantId(tenantId)
					.adultFeedGramsPerDay(400)
					.juvenileFeedGramsPerDay(200)
					.postOpFeedGramsPerDay(400)
					.isolationFeedGramsPerDay(400)
					.workingDaysMode(WorkingDaysMode.EXCLUDE_SUNDAYS)
					.defaultVaccineIntervalDays(365)
					.build());
			Branch branch = branchRepository.save(Branch.builder()
					.tenantId(tenantId)
					.name("Main Centre")
					.code("MAIN")
					.address("12 Rescue Road")
					.city("Pune")
					.status(EntityStatus.ACTIVE)
					.build());
			orgService.provisionWarehouses(tenantId, branch);
			String password = passwordEncoder.encode("Spectra@123");
			User admin = saveUser(tenantId, null, "admin@spectra.org", password, "Asha Super", Role.NGO_ADMIN,
					new BigDecimal("0"), new BigDecimal("0"));
			userBranchRepository.save(UserBranch.builder()
					.tenantId(tenantId)
					.userId(admin.getId())
					.branchId(branch.getId())
					.build());
			User branchAdmin = saveUser(tenantId, branch.getId(), "branch@spectra.org", password, "Bharat Branch",
					Role.BRANCH_ADMIN, new BigDecimal("5000"), new BigDecimal("45000"));
			User inventory = saveUser(tenantId, branch.getId(), "inventory@spectra.org", password, "Ina Inventory",
					Role.INVENTORY_MANAGER, new BigDecimal("3000"), new BigDecimal("28000"));
			saveUser(tenantId, branch.getId(), "vet@spectra.org", password, "Vikram Vet", Role.VET_TECH_EMPLOYEE,
					new BigDecimal("2000"), new BigDecimal("22000"));
			User employee = saveUser(tenantId, branch.getId(), "employee@spectra.org", password, "Esha Employee",
					Role.EMPLOYEE, new BigDecimal("2500"), new BigDecimal("18000"));

			ShelterProduct kibble = shelterProductRepository.save(ShelterProduct.builder()
					.tenantId(tenantId).sku("FOOD-KIBBLE-20").name("Adult Kibble 20kg")
					.unit(UnitOfMeasure.KG).category(ShelterProductCategory.FOOD).barcode("890100000001")
					.reorderLevel(new BigDecimal("40")).lotTracked(true).qtyOnHand(BigDecimal.ZERO)
					.unitPrice(BigDecimal.ZERO).unitCost(BigDecimal.ZERO).staffSale(false).clinicalUse(true).active(true).build());
			ShelterProduct juvenileFood = shelterProductRepository.save(ShelterProduct.builder()
					.tenantId(tenantId).sku("FOOD-JUV-10").name("Juvenile mash 10kg")
					.unit(UnitOfMeasure.KG).category(ShelterProductCategory.FOOD).barcode("890100000002")
					.reorderLevel(new BigDecimal("20")).lotTracked(true).qtyOnHand(BigDecimal.ZERO)
					.unitPrice(BigDecimal.ZERO).unitCost(BigDecimal.ZERO).staffSale(false).clinicalUse(true).active(true).build());
			ShelterProduct rabies = shelterProductRepository.save(ShelterProduct.builder()
					.tenantId(tenantId).sku("VAX-RABIES").name("Rabies Vaccine")
					.unit(UnitOfMeasure.VIAL).category(ShelterProductCategory.VACCINE).barcode("890100000003")
					.vaccineIntervalDays(365).reorderLevel(new BigDecimal("10")).lotTracked(true).qtyOnHand(BigDecimal.ZERO)
					.unitPrice(BigDecimal.ZERO).unitCost(BigDecimal.ZERO).staffSale(false).clinicalUse(true).active(true).build());
			shelterProductRepository.save(ShelterProduct.builder()
					.tenantId(tenantId).sku("MED-DEWORM").name("Dewormer Tablet")
					.unit(UnitOfMeasure.TABLET).category(ShelterProductCategory.MEDICINE).barcode("890100000004")
					.vaccineIntervalDays(90).reorderLevel(new BigDecimal("50")).lotTracked(true).qtyOnHand(BigDecimal.ZERO)
					.unitPrice(BigDecimal.ZERO).unitCost(BigDecimal.ZERO).staffSale(false).clinicalUse(true).active(true).build());

			Supplier supplier = supplierRepository.save(Supplier.builder()
					.tenantId(tenantId).branchId(branch.getId()).name("Paws Wholesale")
					.contactPhone("9876543210").contactEmail("paws@supplier.test")
					.payableBalance(BigDecimal.ZERO).build());

			TenantContext.set(new TenantContext.Snapshot(tenantId, branch.getId(), inventory.getId(),
					Role.INVENTORY_MANAGER, inventory.getEmail(), "SPECTRA", inventory.getFullName()));
			GrnRequest grn = new GrnRequest();
			grn.setSupplierId(supplier.getId());
			grn.setNotes("Opening stock");
			GrnRequest.Item kibbleLine = new GrnRequest.Item();
			kibbleLine.setShelterProductId(kibble.getId());
			kibbleLine.setBatchNumber("KB-2401");
			kibbleLine.setExpiryDate(LocalDate.now().plusMonths(8));
			kibbleLine.setQuantity(new BigDecimal("200"));
			kibbleLine.setUnitLandedCost(new BigDecimal("85.50"));
			GrnRequest.Item juvenileLine = new GrnRequest.Item();
			juvenileLine.setShelterProductId(juvenileFood.getId());
			juvenileLine.setBatchNumber("JF-2401");
			juvenileLine.setExpiryDate(LocalDate.now().plusMonths(6));
			juvenileLine.setQuantity(new BigDecimal("80"));
			juvenileLine.setUnitLandedCost(new BigDecimal("92.00"));
			GrnRequest.Item vaxLine = new GrnRequest.Item();
			vaxLine.setShelterProductId(rabies.getId());
			vaxLine.setBatchNumber("RB-2401");
			vaxLine.setExpiryDate(LocalDate.now().plusMonths(14));
			vaxLine.setQuantity(new BigDecimal("40"));
			vaxLine.setUnitLandedCost(new BigDecimal("120.00"));
			grn.setItems(List.of(kibbleLine, juvenileLine, vaxLine));
			inventoryService.createGrn(grn);

			residentRepository.save(Resident.builder().tenantId(tenantId).branchId(branch.getId())
					.referenceId("DOG-1001").name("Moti").photoUrl(null)
					.kind(CareKind.DOG).category(CareSection.ADULT).intakeDate(LocalDate.now().minusMonths(4))
					.status(CareStatus.ACTIVE).build());
			residentRepository.save(Resident.builder().tenantId(tenantId).branchId(branch.getId())
					.referenceId("CAT-1002").name("Luna").photoUrl(null)
					.kind(CareKind.CAT).category(CareSection.JUVENILE).intakeDate(LocalDate.now().minusWeeks(3))
					.status(CareStatus.ACTIVE).build());
			residentRepository.save(Resident.builder().tenantId(tenantId).branchId(branch.getId())
					.referenceId("COW-1003").name("Gauri").photoUrl(null)
					.kind(CareKind.COW).category(CareSection.POST_OP).intakeDate(LocalDate.now().minusDays(10))
					.status(CareStatus.ACTIVE).build());
			residentRepository.save(Resident.builder().tenantId(tenantId).branchId(branch.getId())
					.referenceId("ELD-1004").name("Kamala").photoUrl(null)
					.kind(CareKind.ELDERLY).category(CareSection.CRITICAL).intakeDate(LocalDate.now().minusDays(2))
					.status(CareStatus.ACTIVE).build());

			ShelterProduct rice = shelterProductRepository.save(ShelterProduct.builder()
					.tenantId(tenantId).sku("RET-RICE").name("Rice 5kg").unit(UnitOfMeasure.PIECE)
					.category(ShelterProductCategory.STAFF_RETAIL).barcode("890200000001")
					.reorderLevel(BigDecimal.ZERO).lotTracked(false).qtyOnHand(new BigDecimal("40"))
					.unitPrice(new BigDecimal("280.00")).unitCost(new BigDecimal("220.00"))
					.staffSale(true).clinicalUse(false).active(true).build());
			shelterProductRepository.save(ShelterProduct.builder()
					.tenantId(tenantId).sku("RET-SOAP").name("Bath Soap").unit(UnitOfMeasure.PIECE)
					.category(ShelterProductCategory.STAFF_RETAIL).barcode("890200000002")
					.reorderLevel(BigDecimal.ZERO).lotTracked(false).qtyOnHand(new BigDecimal("80"))
					.unitPrice(new BigDecimal("45.00")).unitCost(new BigDecimal("32.00"))
					.staffSale(true).clinicalUse(false).active(true).build());
			ShelterProduct snack = shelterProductRepository.save(ShelterProduct.builder()
					.tenantId(tenantId).sku("RET-SNACK").name("Namkeen Pack").unit(UnitOfMeasure.PIECE)
					.category(ShelterProductCategory.STAFF_RETAIL).barcode("890200000003")
					.reorderLevel(BigDecimal.ZERO).lotTracked(false).qtyOnHand(new BigDecimal("100"))
					.unitPrice(new BigDecimal("30.00")).unitCost(new BigDecimal("18.00"))
					.staffSale(true).clinicalUse(false).active(true).build());

			StaffPosOrder order = staffPosOrderRepository.save(StaffPosOrder.builder()
					.tenantId(tenantId).branchId(branch.getId())
					.employeeId(employee.getId()).cashierId(inventory.getId())
					.orderNumber("POS-000001").tender(PosTender.PAYROLL_CREDIT)
					.status(PosOrderStatus.COMPLETED).total(new BigDecimal("310.00"))
					.orderAt(Instant.now().minusSeconds(3600)).build());
			staffPosOrderItemRepository.save(StaffPosOrderItem.builder()
					.tenantId(tenantId).orderId(order.getId()).productId(rice.getId())
					.qty(BigDecimal.ONE).unitPrice(rice.getUnitPrice()).lineTotal(rice.getUnitPrice())
					.unitCost(rice.getUnitCost()).lineCost(rice.getUnitCost()).build());
			staffPosOrderItemRepository.save(StaffPosOrderItem.builder()
					.tenantId(tenantId).orderId(order.getId()).productId(snack.getId())
					.qty(BigDecimal.ONE).unitPrice(snack.getUnitPrice()).lineTotal(snack.getUnitPrice())
					.unitCost(snack.getUnitCost()).lineCost(snack.getUnitCost()).build());
			rice.setQtyOnHand(rice.getQtyOnHand().subtract(BigDecimal.ONE));
			snack.setQtyOnHand(snack.getQtyOnHand().subtract(BigDecimal.ONE));
			shelterProductRepository.save(rice);
			shelterProductRepository.save(snack);

			log.info("Seeded Spectra NGO demo tenant. Users: admin@spectra.org / branch@spectra.org / inventory@spectra.org / vet@spectra.org / employee@spectra.org password Spectra@123");
			log.info("Seeded NGO admin id {}", admin.getId());
			log.info("Seeded branch admin id {}", branchAdmin.getId());
	}

	private User saveUser(Long tenantId, Long branchId, String email, String hash, String name, Role role,
			BigDecimal credit, BigDecimal salary) {
		User user = userRepository.save(User.builder()
				.tenantId(tenantId)
				.branchId(branchId)
				.email(email)
				.passwordHash(hash)
				.fullName(name)
				.role(role)
				.creditLimit(credit)
				.baseMonthlySalary(salary)
				.otherFixedDeductions(BigDecimal.ZERO)
				.rolledOverStoreDebt(BigDecimal.ZERO)
				.status(EntityStatus.ACTIVE)
				.build());
		if (branchId != null) {
			userBranchRepository.save(UserBranch.builder()
					.tenantId(tenantId)
					.userId(user.getId())
					.branchId(branchId)
					.build());
		}
		return user;
	}
}
