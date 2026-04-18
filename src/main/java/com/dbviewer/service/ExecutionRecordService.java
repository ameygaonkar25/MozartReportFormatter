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

    // Static 16-row structure ordered by noOfJobs grouping:
    // 10000 (nodes 1,2,5,10), 100000 (nodes 1,2,5,10), 500000 (nodes 1,2,5,10), 100000-2nd (nodes 1,2,5,10)
    // position index matches seeding order (0-15 per date group)
    private static final List<int[]> STATIC_ROWS = Arrays.asList(
            // noOfJobs=10000  — nodes 1,2,5,10
            new int[]{1,  10000,  0},
            new int[]{2,  10000,  4},
            new int[]{5,  10000,  8},
            new int[]{10, 10000,  12},
            // noOfJobs=100000 (1st occurrence) — nodes 1,2,5,10
            new int[]{1,  100000, 1},
            new int[]{2,  100000, 5},
            new int[]{5,  100000, 9},
            new int[]{10, 100000, 13},
            // noOfJobs=500000 — nodes 1,2,5,10
            new int[]{1,  500000, 2},
            new int[]{2,  500000, 6},
            new int[]{5,  500000, 10},
            new int[]{10, 500000, 14},
            // noOfJobs=100000 (2nd occurrence) — nodes 1,2,5,10
            new int[]{1,  100000, 3},
            new int[]{2,  100000, 7},
            new int[]{5,  100000, 11},
            new int[]{10, 100000, 15}
    );

    // ── Fetch all source records ──────────────────────────────────────────────
    public List<ExecutionRecord> getAllRecords() {
        return repository.findAllByOrderByIdAsc();
    }

    // ── Fetch filtered records ────────────────────────────────────────────────
    public List<ExecutionRecord> getFilteredRecords(String operator, String date1, String date2) {
        log.debug("Filtering records: operator={}, date1={}, date2={}", operator, date1, date2);
        return switch (operator) {
            case ">=" -> repository.findByDateGreaterThanEqual(date1);
            case "<=" -> repository.findByDateLessThanEqual(date1);
            case "between" -> repository.findByDateBetween(date1, date2);
            default -> repository.findAllByOrderByIdAsc();
        };
    }

    // ── Fetch all unique execution dates ─────────────────────────────────────
    public List<String> getAvailableDates() {
        return repository.findAllDistinctDates();
    }

    // ── Build the 16-row placeholder table ───────────────────────────────────
    public List<PlaceholderRow> buildPlaceholderTable(PlaceholderRequest request) {
        log.debug("Building placeholder table for recordIds: col1={}, col2={}, col3={}",
                request.getRecordId1(), request.getRecordId2(), request.getRecordId3());

        ExecutionRecord anchor1 = resolveAnchor(request.getRecordId1());
        ExecutionRecord anchor2 = resolveAnchor(request.getRecordId2());
        ExecutionRecord anchor3 = resolveAnchor(request.getRecordId3());

        List<ExecutionRecord> allRecords = repository.findAllByOrderByIdAsc();
        List<PlaceholderRow> rows = new ArrayList<>();

        for (int[] combo : STATIC_ROWS) {
            int nodes    = combo[0];
            int totalTxn = combo[1];
            int position = combo[2];

            Double avg1 = lookupByAnchorAndPosition(allRecords, anchor1, position);
            Double avg2 = lookupByAnchorAndPosition(allRecords, anchor2, position);
            Double avg3 = lookupByAnchorAndPosition(allRecords, anchor3, position);

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

    private ExecutionRecord resolveAnchor(Long recordId) {
        if (recordId == null) return null;
        return repository.findById(recordId).orElse(null);
    }

    private Double lookupByAnchorAndPosition(List<ExecutionRecord> allRecords,
                                             ExecutionRecord anchor, int position) {
        if (anchor == null) return null;
        List<ExecutionRecord> sameDate = allRecords.stream()
                .filter(r -> r.getExecutionDate().equals(anchor.getExecutionDate()))
                .toList();
        if (position >= sameDate.size()) {
            log.warn("Position {} out of bounds for date group {} (size {})", position, anchor.getExecutionDate(), sameDate.size());
            return null;
        }
        return sameDate.get(position).getAvgTime();
    }
}