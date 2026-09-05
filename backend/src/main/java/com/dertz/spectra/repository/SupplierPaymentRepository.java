package com.dertz.spectra.repository;

import com.dertz.spectra.model.SupplierPayment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SupplierPaymentRepository extends JpaRepository<SupplierPayment, Long> {

	List<SupplierPayment> findBySupplierIdOrderByPaidAtDesc(Long supplierId);
}
