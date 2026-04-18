package com.dbviewer.repository;

import com.dbviewer.model.ExecutionRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExecutionRecordRepository extends JpaRepository<ExecutionRecord, Long> {

    // All distinct execution dates sorted
    @Query("SELECT DISTINCT e.executionDate FROM ExecutionRecord e ORDER BY e.executionDate")
    List<String> findAllDistinctDates();

    // All records sorted by ID (stable order for positional lookup)
    List<ExecutionRecord> findAllByOrderByIdAsc();

    // All records for a specific date, ordered by ID
    List<ExecutionRecord> findByExecutionDateOrderByIdAsc(String executionDate);

    // Filter: records where executionDate >= fromDate
    @Query("SELECT e FROM ExecutionRecord e WHERE e.executionDate >= :fromDate ORDER BY e.id ASC")
    List<ExecutionRecord> findByDateGreaterThanEqual(@Param("fromDate") String fromDate);

    // Filter: records where executionDate <= toDate
    @Query("SELECT e FROM ExecutionRecord e WHERE e.executionDate <= :toDate ORDER BY e.id ASC")
    List<ExecutionRecord> findByDateLessThanEqual(@Param("toDate") String toDate);

    // Filter: records where executionDate between fromDate and toDate (inclusive)
    @Query("SELECT e FROM ExecutionRecord e WHERE e.executionDate >= :fromDate AND e.executionDate <= :toDate ORDER BY e.id ASC")
    List<ExecutionRecord> findByDateBetween(@Param("fromDate") String fromDate, @Param("toDate") String toDate);
}