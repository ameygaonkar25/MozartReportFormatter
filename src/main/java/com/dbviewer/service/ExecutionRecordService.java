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

    // Static 16-row structure: Nodes x TotalTxn combinations (position-aware)
    // Row index matches the seeding order so position-based lookup works correctly
    private static final List<int[]> STATIC_ROWS = Arrays.asList(
            new int[]{1,  10000,  0},   // index 0 within its date group
            new int[]{1,  100000, 1},
            new int[]{1,  500000, 2},
            new int[]{1,  100000, 3},   // duplicate txn, different position
            new int[]{2,  10000,  4},
            new int[]{2,  100000, 5},
            new int[]{2,  500000, 6},
            new int[]{2,  100000, 7},
            new int[]{5,  10000,  8},
            new int[]{5,  100000, 9},
            new int[]{5,  500000, 10},
            new int[]{5,  100000, 11},
            new int[]{10, 10000,  12},
            new int[]{10, 100000, 13},
            new int[]{10, 500000, 14},
            new int[]{10, 100000, 15}
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
    // Uses record IDs to anchor the date, then finds sibling rows by position offset
    public List<PlaceholderRow> buildPlaceholderTable(PlaceholderRequest request) {
        log.debug("Building placeholder table for recordIds: col1={}, col2={}, col3={}",
                request.getRecordId1(), request.getRecordId2(), request.getRecordId3());

        // Resolve anchor records (the ones the user selected)
        ExecutionRecord anchor1 = resolveAnchor(request.getRecordId1());
        ExecutionRecord anchor2 = resolveAnchor(request.getRecordId2());
        ExecutionRecord anchor3 = resolveAnchor(request.getRecordId3());

        // Fetch all records sorted by id so positional offset is stable
        List<ExecutionRecord> allRecords = repository.findAllByOrderByIdAsc();

        List<PlaceholderRow> rows = new ArrayList<>();

        for (int[] combo : STATIC_ROWS) {
            int nodes    = combo[0];
            int totalTxn = combo[1];
            int position = combo[2];  // 0-based position within a date group (16 rows per date)

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

    // ── Resolve anchor record from ID ─────────────────────────────────────────
    private ExecutionRecord resolveAnchor(Long recordId) {
        if (recordId == null) return null;
        return repository.findById(recordId).orElse(null);
    }

    // ── Find the avg time for a given position within the same date group ─────
    // Strategy: find all records with the same executionDate as the anchor,
    // ordered by id, then pick by position index (0-15).
    private Double lookupByAnchorAndPosition(List<ExecutionRecord> allRecords,
                                             ExecutionRecord anchor, int position) {
        if (anchor == null) return null;

        List<ExecutionRecord> sameDate = allRecords.stream()
                .filter(r -> r.getExecutionDate().equals(anchor.getExecutionDate()))
                .toList();

        if (position >= sameDate.size()) {
            log.warn("Position {} out of bounds for date group {} (size {})",
                    position, anchor.getExecutionDate(), sameDate.size());
            return null;
        }

        return sameDate.get(position).getAvgTime();
    }
}