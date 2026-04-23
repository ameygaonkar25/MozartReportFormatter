package com.dbviewer.repository;

import com.dbviewer.model.ExecutionRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ExecutionRecordRepository extends JpaRepository<ExecutionRecord, Long> {

    // All distinct test dates sorted
    @Query("SELECT DISTINCT e.testDate FROM ExecutionRecord e ORDER BY e.testDate ASC")
    List<LocalDate> findAllDistinctDates();

    // All records ordered by ID
    List<ExecutionRecord> findAllByOrderByIdAsc();

    // Records for a specific date ordered by ID
    List<ExecutionRecord> findByTestDateOrderByIdAsc(LocalDate date);

    // Filter: on or after date
    @Query("SELECT e FROM ExecutionRecord e WHERE e.testDate >= :fromDate ORDER BY e.id ASC")
    List<ExecutionRecord> findByDateGreaterThanEqual(@Param("fromDate") LocalDate fromDate);

    // Filter: on or before date
    @Query("SELECT e FROM ExecutionRecord e WHERE e.testDate <= :toDate ORDER BY e.id ASC")
    List<ExecutionRecord> findByDateLessThanEqual(@Param("toDate") LocalDate toDate);

    // Filter: between dates
    @Query("SELECT e FROM ExecutionRecord e WHERE e.testDate >= :fromDate AND e.testDate <= :toDate ORDER BY e.id ASC")
    List<ExecutionRecord> findByDateBetween(@Param("fromDate") LocalDate fromDate, @Param("toDate") LocalDate toDate);
}