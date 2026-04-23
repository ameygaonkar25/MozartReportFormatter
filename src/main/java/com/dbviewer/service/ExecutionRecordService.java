package com.dbviewer.service;

import com.dbviewer.model.ExecutionRecord;
import com.dbviewer.model.PlaceholderRequest;
import com.dbviewer.model.PlaceholderRow;
import com.dbviewer.repository.ExecutionRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExecutionRecordService {

    private final ExecutionRecordRepository repository;

    private static final String NOT_RUN = "Not Run";

    /**
     * Static 16-row placeholder structure.
     * Each entry: { nodes, totalTxn, occurrenceIndex }
     * occurrenceIndex = 0 for all since Oracle table has unique (date+nodes+totalTxn)
     * Grouped by noOfJobs for the rowspan UI.
     */
    private static final List<int[]> STATIC_ROWS = Arrays.asList(
            // noOfJobs = 10,000
            new int[]{1,  10000,   0},
            new int[]{2,  10000,   0},
            new int[]{5,  10000,   0},
            new int[]{10, 10000,   0},
            // noOfJobs = 100,000
            new int[]{1,  100000,  0},
            new int[]{2,  100000,  0},
            new int[]{5,  100000,  0},
            new int[]{10, 100000,  0},
            // noOfJobs = 500,000
            new int[]{1,  500000,  0},
            new int[]{2,  500000,  0},
            new int[]{5,  500000,  0},
            new int[]{10, 500000,  0},
            // noOfJobs = 1,000,000
            new int[]{1,  1000000, 0},
            new int[]{2,  1000000, 0},
            new int[]{5,  1000000, 0},
            new int[]{10, 1000000, 0}
    );

    public List<ExecutionRecord> getAllRecords() {
        return repository.findAllByOrderByIdAsc();
    }

    public List<ExecutionRecord> getFilteredRecords(String operator, String date1, String date2) {
        log.debug("Filtering: operator={}, date1={}, date2={}", operator, date1, date2);
        return switch (operator) {
            case ">=" -> repository.findByDateGreaterThanEqual(date1);
            case "<=" -> repository.findByDateLessThanEqual(date1);
            case "between" -> repository.findByDateBetween(date1, date2);
            default -> repository.findAllByOrderByIdAsc();
        };
    }

    public List<String> getAvailableDates() {
        return repository.findAllDistinctDates();
    }

    public List<PlaceholderRow> buildPlaceholderTable(PlaceholderRequest request) {
        log.debug("Building placeholder for recordIds: col1={}, col2={}, col3={}",
                request.getRecordId1(), request.getRecordId2(), request.getRecordId3());

        ExecutionRecord anchor1 = resolveAnchor(request.getRecordId1());
        ExecutionRecord anchor2 = resolveAnchor(request.getRecordId2());
        ExecutionRecord anchor3 = resolveAnchor(request.getRecordId3());

        // Build lookup maps per date: key="nodes_totalTxn_occIdx" → avgTime
        Map<String, Double> map1 = buildLookupMap(anchor1 != null ? anchor1.getExecutionDate() : null);
        Map<String, Double> map2 = buildLookupMap(anchor2 != null ? anchor2.getExecutionDate() : null);
        Map<String, Double> map3 = buildLookupMap(anchor3 != null ? anchor3.getExecutionDate() : null);

        List<PlaceholderRow> rows = new ArrayList<>();

        for (int[] combo : STATIC_ROWS) {
            int nodes    = combo[0];
            int totalTxn = combo[1];
            int occIdx   = combo[2];
            String key   = nodes + "_" + totalTxn + "_" + occIdx;

            String col1Val = map1.isEmpty() ? "—" : formatAvg(map1.get(key));
            String col2Val = map2.isEmpty() ? "—" : formatAvg(map2.get(key));
            String col3Val = map3.isEmpty() ? "—" : formatAvg(map3.get(key));

            Double raw1 = map1.isEmpty() ? null : map1.get(key);
            Double raw2 = map2.isEmpty() ? null : map2.get(key);
            String improvement = calcImprovement(raw2, raw1, col2Val, col1Val);

            rows.add(PlaceholderRow.builder()
                    .noOfJobs(totalTxn)
                    .nodes(nodes)
                    .dateOf3rdCol(col3Val)
                    .dateOf2ndCol(col2Val)
                    .dateOf1stCol(col1Val)
                    .improvement(improvement)
                    .build());
        }

        return rows;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private ExecutionRecord resolveAnchor(Long recordId) {
        if (recordId == null) return null;
        return repository.findById(recordId).orElse(null);
    }

    /**
     * Builds lookup map for a given date.
     * Key = "nodes_totalTxn_occurrenceIndex"
     * Since Oracle table has unique (date+nodes+totalTxn), occIdx is always 0.
     */
    private Map<String, Double> buildLookupMap(String date) {
        if (date == null || date.isBlank()) return Collections.emptyMap();

        List<ExecutionRecord> dateGroup = repository.findByExecutionDateOrderByIdAsc(date);
        Map<String, Double> result = new LinkedHashMap<>();
        Map<String, Integer> occCounter = new HashMap<>();

        for (ExecutionRecord rec : dateGroup) {
            String baseKey = rec.getNodes() + "_" + rec.getTotalTxn();
            int occ = occCounter.getOrDefault(baseKey, 0);
            result.put(baseKey + "_" + occ, rec.getAvgTime());
            occCounter.put(baseKey, occ + 1);
        }
        return result;
    }

    private String formatAvg(Double avg) {
        if (avg == null) return NOT_RUN;
        return avg % 1 == 0 ? avg.intValue() + "s" : avg + "s";
    }

    private String calcImprovement(Double raw2, Double raw1, String col2Val, String col1Val) {
        if ("—".equals(col1Val) || "—".equals(col2Val)) return "—";
        if (NOT_RUN.equals(col1Val) || NOT_RUN.equals(col2Val)) return NOT_RUN;
        if (raw1 == null || raw2 == null) return NOT_RUN;
        double diff = raw2 - raw1;
        int diffInt = (int) diff;
        if (diff > 0) return "+" + diffInt + "s";
        if (diff < 0) return diffInt + "s";
        return "0s";
    }
}