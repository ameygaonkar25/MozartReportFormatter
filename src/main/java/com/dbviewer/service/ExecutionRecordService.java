package com.dbviewer.service;

import com.dbviewer.model.ExecutionRecord;
import com.dbviewer.model.PlaceholderRequest;
import com.dbviewer.model.PlaceholderRow;
import com.dbviewer.repository.ExecutionRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExecutionRecordService {

    private final ExecutionRecordRepository repository;

    // Static 16-row structure: Nodes x TotalTxn combinations
    private static final List<int[]> STATIC_ROWS = Arrays.asList(
        new int[]{1,  10000},
        new int[]{1,  100000},
        new int[]{1,  500000},
        new int[]{1,  100000},
        new int[]{2,  10000},
        new int[]{2,  100000},
        new int[]{2,  500000},
        new int[]{2,  100000},
        new int[]{5,  10000},
        new int[]{5,  100000},
        new int[]{5,  500000},
        new int[]{5,  100000},
        new int[]{10, 10000},
        new int[]{10, 100000},
        new int[]{10, 500000},
        new int[]{10, 100000}
    );

    // ── Fetch all source records ──────────────────────────────────────────────
    public List<ExecutionRecord> getAllRecords() {
        return repository.findAll();
    }

    // ── Fetch all unique execution dates ─────────────────────────────────────
    public List<String> getAvailableDates() {
        return repository.findAllDistinctDates();
    }

    // ── Build the 16-row placeholder table ───────────────────────────────────
    public List<PlaceholderRow> buildPlaceholderTable(PlaceholderRequest request) {
        log.debug("Building placeholder table for dates: col1={}, col2={}, col3={}",
                request.getDate1(), request.getDate2(), request.getDate3());

        List<PlaceholderRow> rows = new ArrayList<>();

        for (int[] combo : STATIC_ROWS) {
            int nodes   = combo[0];
            int totalTxn = combo[1];

            Double avg1 = lookupAvg(nodes, totalTxn, request.getDate1());
            Double avg2 = lookupAvg(nodes, totalTxn, request.getDate2());
            Double avg3 = lookupAvg(nodes, totalTxn, request.getDate3());

            // Improvement = 2nd col avg - 1st col avg (in seconds)
            Double improvement = null;
            if (avg1 != null && avg2 != null) {
                improvement = avg2 - avg1;
            }

            rows.add(PlaceholderRow.builder()
                    .noOfJobs(totalTxn)
                    .nodes(nodes)
                    .dateOf3rdCol(avg3)
                    .dateOf2ndCol(avg2)
                    .dateOf1stCol(avg1)
                    .improvement(improvement)
                    .build());
        }

        return rows;
    }

    // ── Helper: safe lookup ───────────────────────────────────────────────────
    private Double lookupAvg(int nodes, int totalTxn, String date) {
        if (date == null || date.isBlank()) return null;
        return repository
                .findAvgTimeByNodesAndTxnAndDate(nodes, totalTxn, date)
                .orElse(null);
    }
}
