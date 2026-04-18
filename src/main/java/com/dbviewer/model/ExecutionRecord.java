package com.dbviewer.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "execution_records")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecutionRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Integer nodes;

    @Column(name = "total_txn", nullable = false)
    private Integer totalTxn;

    @Column(name = "execution_date", nullable = false)
    private String executionDate;

    @Column(name = "avg_time", nullable = false)
    private Double avgTime;
}
