package com.dertz.spectra.repository;

import com.dertz.spectra.model.AttendanceRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AttendanceRecordRepository extends JpaRepository<AttendanceRecord, Long> {

	Optional<AttendanceRecord> findByUserIdAndWorkDate(Long userId, LocalDate workDate);

	List<AttendanceRecord> findByBranchIdAndWorkDateBetween(Long branchId, LocalDate from, LocalDate to);

	List<AttendanceRecord> findByUserIdAndWorkDateBetween(Long userId, LocalDate from, LocalDate to);
}
