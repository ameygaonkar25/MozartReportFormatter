package com.dbviewer.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Entity
@Table(name = "MOZART_UNSORTABLE_JOB_RESULT")   // ← your Oracle table name
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecutionRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "exec_rec_seq")
    @SequenceGenerator(name = "exec_rec_seq", sequenceName = "EXECUTION_RECORDS_SEQ", allocationSize = 1)
    @Column(name = "ID")
    private Long id;

    @Column(name = "NODES", nullable = false)
    private Integer nodes;

    @Column(name = "TOTAL_TXN", nullable = false)
    private Integer totalTxn;

    // Map Oracle DATE column as LocalDate — avoids timestamp format issues
    @Column(name = "TEST_DATE", nullable = false)
    private LocalDate testDate;

    @Column(name = "AVERAGE_TIME", nullable = false)
    private Double avgTime;

    // Formatted date string used throughout the app — "04-Apr-26" style
    private static final DateTimeFormatter DISPLAY_FMT = DateTimeFormatter.ofPattern("dd-MMM-yy");

    public String getExecutionDate() {
        return testDate != null ? testDate.format(DISPLAY_FMT) : null;
    }
}