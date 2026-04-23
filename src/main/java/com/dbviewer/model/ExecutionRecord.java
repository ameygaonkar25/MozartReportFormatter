package com.dbviewer.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "EXECUTION_RECORDS")   // ← your Oracle table name
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

    @Column(name = "TEST_DATE", nullable = false)
    private String executionDate;

    @Column(name = "AVERAGE_TIME", nullable = false)
    private Double avgTime;
}