package com.dbviewer.repository;

import com.dbviewer.model.ExecutionRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
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
}