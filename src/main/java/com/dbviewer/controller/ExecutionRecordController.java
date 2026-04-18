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
@CrossOrigin(origins = "*")
public class ExecutionRecordController {

    private final ExecutionRecordService service;

    // GET /api/records — all source records
    @GetMapping("/records")
    public ResponseEntity<List<ExecutionRecord>> getAllRecords() {
        return ResponseEntity.ok(service.getAllRecords());
    }

    // GET /api/records/filter?operator=>=&date1=02-Apr-26
    // GET /api/records/filter?operator=between&date1=02-Apr-26&date2=12-Apr-26
    @GetMapping("/records/filter")
    public ResponseEntity<List<ExecutionRecord>> getFilteredRecords(
            @RequestParam String operator,
            @RequestParam String date1,
            @RequestParam(required = false) String date2) {
        log.info("Filter request: operator={}, date1={}, date2={}", operator, date1, date2);
        return ResponseEntity.ok(service.getFilteredRecords(operator, date1, date2));
    }

    // GET /api/dates — distinct execution dates (for filter dropdowns)
    @GetMapping("/dates")
    public ResponseEntity<List<String>> getAvailableDates() {
        return ResponseEntity.ok(service.getAvailableDates());
    }

    // POST /api/placeholder — build placeholder table
    @PostMapping("/placeholder")
    public ResponseEntity<List<PlaceholderRow>> buildPlaceholder(
            @RequestBody PlaceholderRequest request) {
        log.info("Building placeholder table: {}", request);
        return ResponseEntity.ok(service.buildPlaceholderTable(request));
    }
}