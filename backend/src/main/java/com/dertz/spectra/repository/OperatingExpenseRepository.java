package com.dertz.spectra.repository;

import com.dertz.spectra.Enum.PaymentSource;
import com.dertz.spectra.model.OperatingExpense;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OperatingExpenseRepository extends JpaRepository<OperatingExpense, Long> {

	List<OperatingExpense> findByBranchIdOrderByExpenseDateDesc(Long branchId);

	List<OperatingExpense> findByPaidByUserIdAndPaymentSourceAndReimbursedInPayrollItemIdIsNull(
			Long paidByUserId, PaymentSource paymentSource);

	List<OperatingExpense> findByReimbursedInPayrollItemId(Long payrollItemId);
}
