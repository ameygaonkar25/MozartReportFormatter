package com.dbviewer.controller;

import com.dbviewer.model.ExecutionRecord;
import com.dbviewer.model.PlaceholderRequest;
import com.dbviewer.model.PlaceholderRow;
import com.dbviewer.service.ExecutionRecordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")   // Allow requests from any origin during local dev
public class ExecutionRecordController {

    private final ExecutionRecordService service;

    // GET /api/records — all source records
    @GetMapping("/records")
    public ResponseEntity<List<ExecutionRecord>> getAllRecords() {
        log.info("Fetching all execution records");
        return ResponseEntity.ok(service.getAllRecords());
    }

    // GET /api/dates — distinct execution dates
    @GetMapping("/dates")
    public ResponseEntity<List<String>> getAvailableDates() {
        return ResponseEntity.ok(service.getAvailableDates());
    }

    // POST /api/placeholder — build placeholder table
    // Body: { "date1": "12-Apr-26", "date2": "04-Apr-26", "date3": "02-Apr-26" }
    @PostMapping("/placeholder")
    public ResponseEntity<List<PlaceholderRow>> buildPlaceholder(
            @RequestBody PlaceholderRequest request) {
        log.info("Building placeholder table: {}", request);
        List<PlaceholderRow> rows = service.buildPlaceholderTable(request);
        return ResponseEntity.ok(rows);
    }
}
