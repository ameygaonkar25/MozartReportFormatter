package com.dbviewer.service;

import com.dbviewer.model.ExecutionRecord;
import com.dbviewer.model.PlaceholderRequest;
import com.dbviewer.model.PlaceholderRow;
import com.dbviewer.repository.ExecutionRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExecutionRecordService {

    private final ExecutionRecordRepository repository;

    private static final String NOT_RUN = "Not Run";

    /**
     * Static 16-row definition for the placeholder table.
     *
     * Each entry: { nodes, totalTxn, occurrenceIndex }
     *
     * occurrenceIndex handles the duplicate totalTxn=100000 case:
     *   - 0 = first time this (nodes, totalTxn) pair appears in the date group
     *   - 1 = second time (the duplicate row with different avgTime)
     *
     * Ordered by noOfJobs group for the rowspan UI:
     *   10000 x4 → 100000(occ=0) x4 → 500000 x4 → 100000(occ=1) x4
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

    // ── Public methods ────────────────────────────────────────────────────────

    public List<ExecutionRecord> getAllRecords() {
        return repository.findAllByOrderByIdAsc();
    }

    public List<ExecutionRecord> getFilteredRecords(String operator, String date1, String date2) {
        log.debug("Filtering records: operator={}, date1={}, date2={}", operator, date1, date2);
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

    // ── Placeholder table builder ─────────────────────────────────────────────

    public List<PlaceholderRow> buildPlaceholderTable(PlaceholderRequest request) {
        log.debug("Building placeholder for recordIds: col1={}, col2={}, col3={}",
                request.getRecordId1(), request.getRecordId2(), request.getRecordId3());

        ExecutionRecord anchor1 = resolveAnchor(request.getRecordId1());
        ExecutionRecord anchor2 = resolveAnchor(request.getRecordId2());
        ExecutionRecord anchor3 = resolveAnchor(request.getRecordId3());

        // Build lookup maps per date: key = "nodes_totalTxn_occurrenceIndex" → avgTime
        Map<String, Double> map1 = buildLookupMap(anchor1);
        Map<String, Double> map2 = buildLookupMap(anchor2);
        Map<String, Double> map3 = buildLookupMap(anchor3);

        List<PlaceholderRow> rows = new ArrayList<>();

        for (int[] combo : STATIC_ROWS) {
            int nodes       = combo[0];
            int totalTxn    = combo[1];
            int occIdx      = combo[2];
            String key      = nodes + "_" + totalTxn + "_" + occIdx;

            String col1Val = formatAvg(map1.isEmpty() ? null : map1.get(key));
            String col2Val = formatAvg(map2.isEmpty() ? null : map2.get(key));
            String col3Val = formatAvg(map3.isEmpty() ? null : map3.get(key));
            String improvement = calcImprovement(map2.get(key), map1.get(key), col2Val, col1Val);

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

    /**
     * Builds a lookup map for the date group of the anchor record.
     * Key = "nodes_totalTxn_occurrenceIndex"
     * Value = avgTime
     *
     * occurrenceIndex tracks how many times a (nodes, totalTxn) pair has been
     * seen within the date group (in ID order), so duplicate rows are uniquely
     * addressable even without a position index.
     */
    private Map<String, Double> buildLookupMap(ExecutionRecord anchor) {
        if (anchor == null) return Collections.emptyMap();

        List<ExecutionRecord> dateGroup =
                repository.findByExecutionDateOrderByIdAsc(anchor.getExecutionDate());

        Map<String, Double> result = new LinkedHashMap<>();
        // occurrence counter: how many times have we seen each (nodes, totalTxn) pair
        Map<String, Integer> occurrenceCounter = new HashMap<>();

        for (ExecutionRecord rec : dateGroup) {
            String baseKey = rec.getNodes() + "_" + rec.getTotalTxn();
            int occ = occurrenceCounter.getOrDefault(baseKey, 0);
            String fullKey = baseKey + "_" + occ;
            result.put(fullKey, rec.getAvgTime());
            occurrenceCounter.put(baseKey, occ + 1);
        }

        return result;
    }

    private ExecutionRecord resolveAnchor(Long recordId) {
        if (recordId == null) return null;
        return repository.findById(recordId).orElse(null);
    }

    /**
     * Formats avgTime as a string.
     * null means the row was missing from DB → "Not Run"
     * A real value → "200.0s"
     * Empty map (no column assigned) → "—"
     */
    private String formatAvg(Double avg) {
        if (avg == null) return NOT_RUN;
        return avg % 1 == 0
                ? String.valueOf(avg.intValue()) + "s"
                : avg + "s";
    }

    /**
     * Calculates improvement (col2 - col1).
     * Returns "Not Run" if either column is missing.
     * Returns "—" if either column is unassigned.
     */
    private String calcImprovement(Double raw2, Double raw1, String col2Val, String col1Val) {
        // If either column is not assigned at all
        if (col1Val == null || col2Val == null) return "—";
        // If either is Not Run
        if (NOT_RUN.equals(col1Val) || NOT_RUN.equals(col2Val)) return NOT_RUN;
        if (raw1 == null || raw2 == null) return NOT_RUN;
        double diff = raw2 - raw1;
        String sign = diff > 0 ? "+" : "";
        return sign + (diff % 1 == 0 ? String.valueOf((int) diff) : String.valueOf(diff)) + "s";
    }
}