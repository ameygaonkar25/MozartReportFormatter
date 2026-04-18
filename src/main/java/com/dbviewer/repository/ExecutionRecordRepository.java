package com.dbviewer.repository;

import com.dbviewer.model.ExecutionRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExecutionRecordRepository extends JpaRepository<ExecutionRecord, Long> {

    // Find all distinct execution dates (for dropdown / date selection)
    @Query("SELECT DISTINCT e.executionDate FROM ExecutionRecord e ORDER BY e.executionDate")
    List<String> findAllDistinctDates();

    // Find a specific record by nodes + totalTxn + date (used for placeholder lookup)
    Optional<ExecutionRecord> findByNodesAndTotalTxnAndExecutionDate(
            Integer nodes, Integer totalTxn, String executionDate
    );

    // Find all records for a specific date
    List<ExecutionRecord> findByExecutionDateOrderByNodesAscTotalTxnAsc(String executionDate);

    // Custom query: find avg time for specific combination
    @Query("SELECT e.avgTime FROM ExecutionRecord e " +
           "WHERE e.nodes = :nodes AND e.totalTxn = :totalTxn AND e.executionDate = :date")
    Optional<Double> findAvgTimeByNodesAndTxnAndDate(
            @Param("nodes") Integer nodes,
            @Param("totalTxn") Integer totalTxn,
            @Param("date") String date
    );
}
