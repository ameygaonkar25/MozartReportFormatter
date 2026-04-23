package com.dbviewer.service;

import com.dbviewer.model.ExecutionRecord;
import com.dbviewer.model.PlaceholderRequest;
import com.dbviewer.model.PlaceholderRow;
import com.dbviewer.repository.ExecutionRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExecutionRecordService {

    private final ExecutionRecordRepository repository;

    private static final String NOT_RUN = "Not Run";

    // Must match getExecutionDate() format in ExecutionRecord — "dd-MMM-yy"
    private static final DateTimeFormatter DISPLAY_FMT = DateTimeFormatter.ofPattern("dd-MMM-yy");

    private static final List<int[]> STATIC_ROWS = Arrays.asList(
            new int[]{1,  10000,   0},
            new int[]{2,  10000,   0},
            new int[]{5,  10000,   0},
            new int[]{10, 10000,   0},
            new int[]{1,  100000,  0},
            new int[]{2,  100000,  0},
            new int[]{5,  100000,  0},
            new int[]{10, 100000,  0},
            new int[]{1,  500000,  0},
            new int[]{2,  500000,  0},
            new int[]{5,  500000,  0},
            new int[]{10, 500000,  0},
            new int[]{1,  1000000, 0},
            new int[]{2,  1000000, 0},
            new int[]{5,  1000000, 0},
            new int[]{10, 1000000, 0}
    );

    // ── Public methods ────────────────────────────────────────────────────────

    public List<ExecutionRecord> getAllRecords() {
        return repository.findAllByOrderByIdAsc();
    }

    public List<String> getAvailableDates() {
        // Convert LocalDate list → formatted strings for the frontend
        return repository.findAllDistinctDates().stream()
                .map(d -> d.format(DISPLAY_FMT))
                .toList();
    }

    public List<ExecutionRecord> getFilteredRecords(String operator, String date1Str, String date2Str) {
        log.debug("Filtering: operator={}, date1={}, date2={}", operator, date1Str, date2Str);
        LocalDate date1 = parseDate(date1Str);
        LocalDate date2 = parseDate(date2Str);
        if (date1 == null) return repository.findAllByOrderByIdAsc();
        return switch (operator) {
            case ">=" -> repository.findByDateGreaterThanEqual(date1);
            case "<=" -> repository.findByDateLessThanEqual(date1);
            case "between" -> date2 != null
                    ? repository.findByDateBetween(date1, date2)
                    : repository.findByDateGreaterThanEqual(date1);
            default   -> repository.findAllByOrderByIdAsc();
        };
    }

    public List<PlaceholderRow> buildPlaceholderTable(PlaceholderRequest request) {
        log.debug("Building placeholder for dates: col1={}, col2={}, col3={}",
                request.getDate1(), request.getDate2(), request.getDate3());

        Map<String, Double> map1 = buildLookupMap(request.getDate1());
        Map<String, Double> map2 = buildLookupMap(request.getDate2());
        Map<String, Double> map3 = buildLookupMap(request.getDate3());

        List<PlaceholderRow> rows = new ArrayList<>();
        for (int[] combo : STATIC_ROWS) {
            int nodes    = combo[0];
            int totalTxn = combo[1];
            int occIdx   = combo[2];
            String key   = nodes + "_" + totalTxn + "_" + occIdx;

            String col1Val = map1.isEmpty() ? "—" : formatAvg(map1.get(key));
            String col2Val = map2.isEmpty() ? "—" : formatAvg(map2.get(key));
            String col3Val = map3.isEmpty() ? "—" : formatAvg(map3.get(key));
            Double raw1    = map1.isEmpty() ? null : map1.get(key);
            Double raw2    = map2.isEmpty() ? null : map2.get(key);

            rows.add(PlaceholderRow.builder()
                    .noOfJobs(totalTxn)
                    .nodes(nodes)
                    .dateOf3rdCol(col3Val)
                    .dateOf2ndCol(col2Val)
                    .dateOf1stCol(col1Val)
                    .improvement(calcImprovement(raw2, raw1, col2Val, col1Val))
                    .build());
        }
        return rows;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Builds lookup map keyed by "nodes_totalTxn_occIdx" for a given date string.
     * Parses the display date string back to LocalDate for the DB query.
     */
    private Map<String, Double> buildLookupMap(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) return Collections.emptyMap();

        LocalDate date = parseDate(dateStr);
        if (date == null) {
            log.warn("Could not parse date string: '{}'", dateStr);
            return Collections.emptyMap();
        }

        List<ExecutionRecord> dateGroup = repository.findByTestDateOrderByIdAsc(date);
        log.debug("Found {} records for date {}", dateGroup.size(), dateStr);

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

    /**
     * Parses a display-format date string "dd-MMM-yy" back to LocalDate.
     * Handles both "04-Apr-26" and ISO format "2026-04-04" as fallback.
     */
    private LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) return null;
        try {
            return LocalDate.parse(dateStr, DISPLAY_FMT);
        } catch (DateTimeParseException e) {
            try {
                return LocalDate.parse(dateStr); // try ISO format as fallback
            } catch (DateTimeParseException e2) {
                log.warn("Cannot parse date: '{}'", dateStr);
                return null;
            }
        }
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