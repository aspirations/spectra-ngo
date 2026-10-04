package com.dertz.spectra.config;

import com.dertz.spectra.Enum.AnalyticsAlertType;
import com.dertz.spectra.Enum.CareKind;
import com.dertz.spectra.Enum.CareSection;
import com.dertz.spectra.Enum.CareStatus;
import com.dertz.spectra.Enum.EntityStatus;
import com.dertz.spectra.Enum.ExpenseCategory;
import com.dertz.spectra.Enum.LeaveStatus;
import com.dertz.spectra.Enum.LeaveType;
import com.dertz.spectra.Enum.PaymentSource;
import com.dertz.spectra.Enum.NoteType;
import com.dertz.spectra.Enum.PosOrderStatus;
import com.dertz.spectra.Enum.PosTender;
import com.dertz.spectra.Enum.Role;
import com.dertz.spectra.Enum.ShelterProductCategory;
import com.dertz.spectra.Enum.UnitOfMeasure;
import com.dertz.spectra.Enum.WorkingDaysMode;
import com.dertz.spectra.model.AnalyticsAlert;
import com.dertz.spectra.model.Branch;
import com.dertz.spectra.model.DogCaseNote;
import com.dertz.spectra.model.DogExpenseLink;
import com.dertz.spectra.model.DogVaccination;
import com.dertz.spectra.model.FuelLog;
import com.dertz.spectra.model.LeaveRequest;
import com.dertz.spectra.model.OperatingExpense;
import com.dertz.spectra.model.Resident;
import com.dertz.spectra.model.ShelterProduct;
import com.dertz.spectra.model.StaffPosOrder;
import com.dertz.spectra.model.StaffPosOrderItem;
import com.dertz.spectra.model.Supplier;
import com.dertz.spectra.model.Tenant;
import com.dertz.spectra.model.TenantSettings;
import com.dertz.spectra.model.User;
import com.dertz.spectra.model.UserBranch;
import com.dertz.spectra.repository.AnalyticsAlertRepository;
import com.dertz.spectra.repository.BranchRepository;
import com.dertz.spectra.repository.DogCaseNoteRepository;
import com.dertz.spectra.repository.DogExpenseLinkRepository;
import com.dertz.spectra.repository.DogVaccinationRepository;
import com.dertz.spectra.repository.FuelLogRepository;
import com.dertz.spectra.repository.LeaveRequestRepository;
import com.dertz.spectra.repository.OperatingExpenseRepository;
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
import org.springframework.core.annotation.Order;
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
@Order(20)
@RequiredArgsConstructor
public class DogCareDataSeeder implements ApplicationRunner {

	public static final String TENANT_CODE = "DOGCARE";

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
	private final DogCaseNoteRepository dogCaseNoteRepository;
	private final DogVaccinationRepository dogVaccinationRepository;
	private final StaffPosOrderRepository staffPosOrderRepository;
	private final StaffPosOrderItemRepository staffPosOrderItemRepository;
	private final LeaveRequestRepository leaveRequestRepository;
	private final OperatingExpenseRepository operatingExpenseRepository;
	private final DogExpenseLinkRepository dogExpenseLinkRepository;
	private final FuelLogRepository fuelLogRepository;
	private final AnalyticsAlertRepository analyticsAlertRepository;
	private final PlatformTransactionManager transactionManager;

	@Override
	public void run(ApplicationArguments args) {
		if (!environment.getProperty("application.seed.enabled", Boolean.class, true)) {
			return;
		}
		if (tenantRepository.findByCodeIgnoreCase(TENANT_CODE).isPresent()) {
			return;
		}
		TransactionTemplate tx = new TransactionTemplate(transactionManager);
		Long tenantId = tx.execute(status -> tenantRepository.save(Tenant.builder()
				.name("DOG Care")
				.code(TENANT_CODE)
				.timezone("Asia/Kolkata")
				.currency("INR")
				.status(EntityStatus.ACTIVE)
				.build()).getId());
		if (tenantId == null) {
			throw new IllegalStateException("Failed to create DOG Care tenant");
		}
		TenantContext.set(new TenantContext.Snapshot(tenantId, null, 0L, Role.NGO_ADMIN, "seed", TENANT_CODE, "Seeder"));
		try {
			tx.executeWithoutResult(status -> seed(tenantId));
			log.info("Seeded DOG Care. Logins: admin@dogcare.org / inventory@dogcare.org password Spectra@123");
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
				.name("Pune Kennel")
				.code("PUNE")
				.address("14 Rescue Lane")
				.city("Pune")
				.status(EntityStatus.ACTIVE)
				.build());
		orgService.provisionWarehouses(tenantId, branch);

		String password = passwordEncoder.encode("Spectra@123");
		User admin = saveUser(tenantId, null, "admin@dogcare.org", password, "DOG Care Admin", Role.NGO_ADMIN,
				BigDecimal.ZERO, BigDecimal.ZERO);
		userBranchRepository.save(UserBranch.builder().tenantId(tenantId).userId(admin.getId()).branchId(branch.getId()).build());

		User inventory = saveUser(tenantId, branch.getId(), "inventory@dogcare.org", password, "Ira Inventory",
				Role.INVENTORY_MANAGER, new BigDecimal("4000"), new BigDecimal("32000"));
		User vet = saveUser(tenantId, branch.getId(), "vet@dogcare.org", password, "Dr. Vani", Role.VET_TECH_EMPLOYEE,
				new BigDecimal("2000"), new BigDecimal("28000"));
		User caretaker = saveUser(tenantId, branch.getId(), "care@dogcare.org", password, "Karan Caretaker", Role.EMPLOYEE,
				new BigDecimal("1500"), new BigDecimal("16000"));

		ShelterProduct kibble = shelterProductRepository.save(ShelterProduct.builder()
				.tenantId(tenantId).sku("DOG-KIBBLE-20").name("Adult Dog Kibble 20kg")
				.unit(UnitOfMeasure.KG).category(ShelterProductCategory.FOOD).barcode("891000000001")
				.reorderLevel(new BigDecimal("50")).lotTracked(true).qtyOnHand(BigDecimal.ZERO)
				.unitPrice(BigDecimal.ZERO).unitCost(BigDecimal.ZERO).staffSale(false).clinicalUse(true).active(true).build());
		ShelterProduct pupFood = shelterProductRepository.save(ShelterProduct.builder()
				.tenantId(tenantId).sku("DOG-PUP-10").name("Puppy Mash 10kg")
				.unit(UnitOfMeasure.KG).category(ShelterProductCategory.FOOD).barcode("891000000002")
				.reorderLevel(new BigDecimal("20")).lotTracked(true).qtyOnHand(BigDecimal.ZERO)
				.unitPrice(BigDecimal.ZERO).unitCost(BigDecimal.ZERO).staffSale(false).clinicalUse(true).active(true).build());
		ShelterProduct rabies = shelterProductRepository.save(ShelterProduct.builder()
				.tenantId(tenantId).sku("DOG-VAX-RABIES").name("Rabies Vaccine")
				.unit(UnitOfMeasure.VIAL).category(ShelterProductCategory.VACCINE).barcode("891000000003")
				.vaccineIntervalDays(365).reorderLevel(new BigDecimal("15")).lotTracked(true).qtyOnHand(BigDecimal.ZERO)
				.unitPrice(BigDecimal.ZERO).unitCost(BigDecimal.ZERO).staffSale(false).clinicalUse(true).active(true).build());
		ShelterProduct deworm = shelterProductRepository.save(ShelterProduct.builder()
				.tenantId(tenantId).sku("DOG-MED-DEWORM").name("Dewormer Tablet")
				.unit(UnitOfMeasure.TABLET).category(ShelterProductCategory.MEDICINE).barcode("891000000004")
				.vaccineIntervalDays(90).reorderLevel(new BigDecimal("40")).lotTracked(true).qtyOnHand(BigDecimal.ZERO)
				.unitPrice(BigDecimal.ZERO).unitCost(BigDecimal.ZERO).staffSale(false).clinicalUse(true).active(true).build());
		ShelterProduct rice = shelterProductRepository.save(ShelterProduct.builder()
				.tenantId(tenantId).sku("STAFF-RICE").name("Rice 5kg")
				.unit(UnitOfMeasure.PIECE).category(ShelterProductCategory.STAFF_RETAIL).barcode("892000000001")
				.reorderLevel(BigDecimal.ZERO).lotTracked(false).qtyOnHand(BigDecimal.ZERO)
				.unitPrice(new BigDecimal("280.00")).unitCost(new BigDecimal("220.00")).staffSale(true).clinicalUse(false)
				.active(true).build());
		ShelterProduct tea = shelterProductRepository.save(ShelterProduct.builder()
				.tenantId(tenantId).sku("STAFF-TEA").name("Tea Pack")
				.unit(UnitOfMeasure.PIECE).category(ShelterProductCategory.STAFF_RETAIL).barcode("892000000002")
				.reorderLevel(BigDecimal.ZERO).lotTracked(false).qtyOnHand(new BigDecimal("40"))
				.unitPrice(new BigDecimal("90.00")).unitCost(new BigDecimal("55.00")).staffSale(true).clinicalUse(false)
				.active(true).build());

		inventoryService.seedSimpleQty(branch.getId(), tea, new BigDecimal("40"));

		Supplier supplier = supplierRepository.save(Supplier.builder()
				.tenantId(tenantId).branchId(branch.getId()).name("Canine Supplies Co")
				.contactPhone("9876501234").contactEmail("orders@caninesupplies.test")
				.payableBalance(BigDecimal.ZERO).build());

		TenantContext.set(new TenantContext.Snapshot(tenantId, branch.getId(), inventory.getId(),
				Role.INVENTORY_MANAGER, inventory.getEmail(), TENANT_CODE, inventory.getFullName()));

		GrnRequest grn = new GrnRequest();
		grn.setSupplierId(supplier.getId());
		grn.setNotes("DOG Care opening stock");
		GrnRequest.Item kibbleLine = line(kibble.getId(), "DK-2501", 8, "250", "88.00");
		GrnRequest.Item pupLine = line(pupFood.getId(), "DP-2501", 6, "60", "95.00");
		GrnRequest.Item vaxLine = line(rabies.getId(), "RV-2501", 12, "30", "135.00");
		GrnRequest.Item medLine = line(deworm.getId(), "DW-2501", 10, "100", "12.50");
		grn.setItems(List.of(kibbleLine, pupLine, vaxLine, medLine, simpleLine(rice.getId(), "25", "220.00")));
		inventoryService.createGrn(grn);

		Resident moti = dog(tenantId, branch.getId(), "DOG-2001", "Moti", CareSection.ADULT, 120);
		Resident luna = dog(tenantId, branch.getId(), "DOG-2002", "Luna", CareSection.JUVENILE, 25);
		Resident raja = dog(tenantId, branch.getId(), "DOG-2003", "Raja", CareSection.POST_OP, 8);
		Resident chiku = dog(tenantId, branch.getId(), "DOG-2004", "Chiku", CareSection.CRITICAL, 3);
		Resident bruno = dog(tenantId, branch.getId(), "DOG-2005", "Bruno", CareSection.ADULT, 60);

		note(tenantId, moti.getId(), "Stable adult — good appetite", NoteType.DIET);
		note(tenantId, luna.getId(), "Puppy — started deworming schedule", NoteType.GENERAL);
		note(tenantId, raja.getId(), "Post-op recovery after limb fracture repair", NoteType.SURGERY_RECOVERY);
		note(tenantId, chiku.getId(), "Critical — IV fluids overnight", NoteType.GENERAL);

		treatment(tenantId, moti.getId(), rabies.getId(), LocalDate.now().plusMonths(10));
		treatment(tenantId, luna.getId(), deworm.getId(), LocalDate.now().plusMonths(2));
		treatment(tenantId, bruno.getId(), rabies.getId(), LocalDate.now().plusDays(5));

		StaffPosOrder order = staffPosOrderRepository.save(StaffPosOrder.builder()
				.tenantId(tenantId).branchId(branch.getId())
				.employeeId(caretaker.getId()).cashierId(inventory.getId())
				.orderNumber("POS-DC-0001").tender(PosTender.PAYROLL_CREDIT)
				.status(PosOrderStatus.COMPLETED).total(rice.getUnitPrice())
				.orderAt(Instant.now().minusSeconds(7200)).build());
		staffPosOrderItemRepository.save(StaffPosOrderItem.builder()
				.tenantId(tenantId).orderId(order.getId()).productId(rice.getId())
				.qty(BigDecimal.ONE).unitPrice(rice.getUnitPrice()).lineTotal(rice.getUnitPrice())
				.unitCost(rice.getUnitCost()).lineCost(rice.getUnitCost()).build());
		inventoryService.seedSimpleQty(branch.getId(), rice, new BigDecimal("24"));

		LocalDate today = LocalDate.now();
		LocalDate lopDay = today.minusDays(2);
		if (lopDay.getDayOfWeek().getValue() == 7) {
			lopDay = today.minusDays(3);
		}
		leaveRequestRepository.save(LeaveRequest.builder()
				.tenantId(tenantId).branchId(branch.getId()).userId(caretaker.getId())
				.type(LeaveType.UNPAID_LEAVE_LOP).fromDate(lopDay).toDate(lopDay)
				.days(BigDecimal.ONE).status(LeaveStatus.APPROVED)
				.reason("Seeded LOP — unmarked days count as present")
				.reviewedBy(admin.getId()).build());

		OperatingExpense fuelExp = operatingExpenseRepository.save(OperatingExpense.builder()
				.tenantId(tenantId).branchId(branch.getId()).category(ExpenseCategory.FUEL)
				.description("Ambulance diesel fill").amount(new BigDecimal("3200.00"))
				.paymentSource(PaymentSource.NGO_CASH).expenseDate(today.minusDays(2))
				.vendorName("HP Pump Baner").paidByUserId(inventory.getId()).build());
		OperatingExpense vetFee = operatingExpenseRepository.save(OperatingExpense.builder()
				.tenantId(tenantId).branchId(branch.getId()).category(ExpenseCategory.VET_FEE)
				.description("External specialist consult for Chiku").amount(new BigDecimal("1800.00"))
				.paymentSource(PaymentSource.DONOR_FUND).expenseDate(today.minusDays(1))
				.vendorName("City Vet Clinic").paidByUserId(vet.getId()).build());
		OperatingExpense maint = operatingExpenseRepository.save(OperatingExpense.builder()
				.tenantId(tenantId).branchId(branch.getId()).category(ExpenseCategory.MAINTENANCE)
				.description("Kennel drain repair").amount(new BigDecimal("4500.00"))
				.paymentSource(PaymentSource.NGO_CASH).expenseDate(today.minusDays(4))
				.vendorName("Local plumber").paidByUserId(admin.getId()).build());
		OperatingExpense workerPaid = operatingExpenseRepository.save(OperatingExpense.builder()
				.tenantId(tenantId).branchId(branch.getId()).category(ExpenseCategory.MEDICATION)
				.description("Emergency antiseptic bought by caretaker").amount(new BigDecimal("350.00"))
				.paymentSource(PaymentSource.WORKER_PAID).expenseDate(today.minusDays(1))
				.vendorName("Medical store").paidByUserId(caretaker.getId()).build());

		fuelLogRepository.save(FuelLog.builder()
				.tenantId(tenantId).branchId(branch.getId()).vehicleLabel("Ambulance MH-12-DC-01")
				.odometerKm(new BigDecimal("48210.0")).litres(new BigDecimal("28.00"))
				.amount(fuelExp.getAmount()).filledAt(today.minusDays(2)).operatingExpenseId(fuelExp.getId()).build());
		fuelLogRepository.save(FuelLog.builder()
				.tenantId(tenantId).branchId(branch.getId()).vehicleLabel("Ambulance MH-12-DC-01")
				.odometerKm(new BigDecimal("48490.0")).litres(new BigDecimal("32.00"))
				.amount(new BigDecimal("3600.00")).filledAt(today).operatingExpenseId(null).build());

		dogExpenseLinkRepository.save(DogExpenseLink.builder()
				.tenantId(tenantId).branchId(branch.getId()).residentId(chiku.getId())
				.operatingExpenseId(vetFee.getId()).label("Specialist consult").amount(vetFee.getAmount())
				.expenseDate(vetFee.getExpenseDate()).build());
		dogExpenseLinkRepository.save(DogExpenseLink.builder()
				.tenantId(tenantId).branchId(branch.getId()).residentId(raja.getId())
				.shelterProductId(deworm.getId()).label("Post-op meds allocation").amount(new BigDecimal("125.00")
				).expenseDate(today.minusDays(3)).build());
		dogExpenseLinkRepository.save(DogExpenseLink.builder()
				.tenantId(tenantId).branchId(branch.getId()).residentId(luna.getId())
				.shelterProductId(pupFood.getId()).label("Puppy food share").amount(new BigDecimal("475.00"))
				.expenseDate(today.minusDays(2)).build());
		dogExpenseLinkRepository.save(DogExpenseLink.builder()
				.tenantId(tenantId).branchId(branch.getId()).residentId(bruno.getId())
				.operatingExpenseId(workerPaid.getId()).label("Emergency antiseptic").amount(workerPaid.getAmount())
				.expenseDate(workerPaid.getExpenseDate()).build());

		analyticsAlertRepository.save(AnalyticsAlert.builder()
				.tenantId(tenantId).branchId(branch.getId()).alertType(AnalyticsAlertType.BURN_RATE)
				.severity("HIGH").title("Feed burn rate 18% over theoretical")
				.detail("Actual kennel feed issue last 7 days exceeds census-based estimate by 18%.")
				.detectedAt(Instant.now().minusSeconds(3600)).acknowledged(false).build());
		analyticsAlertRepository.save(AnalyticsAlert.builder()
				.tenantId(tenantId).branchId(branch.getId()).alertType(AnalyticsAlertType.FUEL_MILEAGE)
				.severity("MEDIUM").title("Ambulance km/litre dropped")
				.detail("Latest fill: 280 km / 32 L ≈ 8.8 km/L vs prior 10.0 km/L.")
				.detectedAt(Instant.now().minusSeconds(1800)).acknowledged(false).build());
		analyticsAlertRepository.save(AnalyticsAlert.builder()
				.tenantId(tenantId).branchId(branch.getId()).alertType(AnalyticsAlertType.GHOST_WORKER)
				.severity("HIGH").title("Caretaker unpaid leave pattern")
				.detail("Karan Caretaker marked UNPAID_LEAVE_LOP mid-week while POS purchase still posted same week.")
				.detectedAt(Instant.now().minusSeconds(900)).acknowledged(false).build());
		analyticsAlertRepository.save(AnalyticsAlert.builder()
				.tenantId(tenantId).branchId(branch.getId()).alertType(AnalyticsAlertType.ANOMALY)
				.severity("LOW").title("Maintenance spike")
				.detail("Kennel maintenance " + maint.getAmount() + " INR above 30-day rolling average.")
				.detectedAt(Instant.now().minusSeconds(600)).acknowledged(true).build());
	}

	private static GrnRequest.Item simpleLine(Long productId, String qty, String cost) {
		GrnRequest.Item item = new GrnRequest.Item();
		item.setShelterProductId(productId);
		item.setQuantity(new BigDecimal(qty));
		item.setUnitLandedCost(new BigDecimal(cost));
		return item;
	}

	private static GrnRequest.Item line(Long productId, String batch, int months, String qty, String cost) {
		GrnRequest.Item item = new GrnRequest.Item();
		item.setShelterProductId(productId);
		item.setBatchNumber(batch);
		item.setExpiryDate(LocalDate.now().plusMonths(months));
		item.setQuantity(new BigDecimal(qty));
		item.setUnitLandedCost(new BigDecimal(cost));
		return item;
	}

	private Resident dog(Long tenantId, Long branchId, String ref, String name, CareSection section, int daysAgo) {
		return residentRepository.save(Resident.builder()
				.tenantId(tenantId).branchId(branchId).referenceId(ref).name(name)
				.kind(CareKind.DOG).category(section).intakeDate(LocalDate.now().minusDays(daysAgo))
				.status(CareStatus.ACTIVE).build());
	}

	private void note(Long tenantId, Long residentId, String body, NoteType type) {
		dogCaseNoteRepository.save(DogCaseNote.builder()
				.tenantId(tenantId).residentId(residentId).noteText(body).noteType(type).build());
	}

	private void treatment(Long tenantId, Long residentId, Long productId, LocalDate nextDue) {
		dogVaccinationRepository.save(DogVaccination.builder()
				.tenantId(tenantId).residentId(residentId).shelterProductId(productId)
				.status(com.dertz.spectra.Enum.TreatmentStatus.ISSUED)
				.administeredAt(Instant.now().minusSeconds(172800)).nextDueDate(nextDue)
				.quantity(BigDecimal.ONE).notes("Seeded treatment").build());
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
					.tenantId(tenantId).userId(user.getId()).branchId(branchId).build());
		}
		return user;
	}
}
