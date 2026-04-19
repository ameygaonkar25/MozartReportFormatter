package com.dbviewer.controller;

import com.dbviewer.model.ExecutionRecord;
import com.dbviewer.repository.ExecutionRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * DEV-ONLY controller for testing "Not Run" / missing row scenarios.
 * Remove this class (or keep @Profile("dev")) before going to production.
 */
@Slf4j
@RestController
@RequestMapping("/api/test")
@RequiredArgsConstructor
public class TestDataController {

    private final ExecutionRecordRepository repository;

    // GET /api/test/rows — see all records (id, nodes, totalTxn, date)
    @GetMapping("/rows")
    public ResponseEntity<List<ExecutionRecord>> listAll() {
        return ResponseEntity.ok(repository.findAllByOrderByIdAsc());
    }

    // DELETE /api/test/delete?nodes=5&totalTxn=10000&date=12-Apr-26
    @DeleteMapping("/delete")
    public ResponseEntity<Map<String, Object>> deleteRow(
            @RequestParam Integer nodes,
            @RequestParam Integer totalTxn,
            @RequestParam String date) {

        List<ExecutionRecord> matches = repository
                .findByExecutionDateOrderByIdAsc(date)
                .stream()
                .filter(r -> r.getNodes().equals(nodes) && r.getTotalTxn().equals(totalTxn))
                .toList();

        if (matches.isEmpty()) {
            return ResponseEntity.ok(Map.of(
                "deleted", 0,
                "message", "No matching rows found for nodes=" + nodes +
                           ", totalTxn=" + totalTxn + ", date=" + date
            ));
        }

        repository.deleteAll(matches);
        log.info("TEST: Deleted {} row(s) — nodes={}, totalTxn={}, date={}", matches.size(), nodes, totalTxn, date);

        return ResponseEntity.ok(Map.of(
            "deleted", matches.size(),
            "message", "Deleted " + matches.size() + " row(s) for nodes=" + nodes +
                       ", totalTxn=" + totalTxn + ", date=" + date
        ));
    }

    // POST /api/test/reset — re-seeds all data back (calls DataSeeder logic)
    @PostMapping("/reset")
    public ResponseEntity<Map<String, String>> reset() {
        repository.deleteAll();
        log.info("TEST: All records deleted. Restart the app to re-seed, or call /api/test/seed");
        return ResponseEntity.ok(Map.of("message",
            "All records deleted. Restart the app to re-seed data automatically."));
    }
}
