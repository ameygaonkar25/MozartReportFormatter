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

    @GetMapping("/records")
    public ResponseEntity<List<ExecutionRecord>> getAllRecords() {
        return ResponseEntity.ok(service.getAllRecords());
    }

    @GetMapping("/records/filter")
    public ResponseEntity<List<ExecutionRecord>> getFilteredRecords(
            @RequestParam String operator,
            @RequestParam String date1,
            @RequestParam(required = false) String date2) {
        return ResponseEntity.ok(service.getFilteredRecords(operator, date1, date2));
    }

    @GetMapping("/dates")
    public ResponseEntity<List<String>> getAvailableDates() {
        return ResponseEntity.ok(service.getAvailableDates());
    }

    // POST /api/placeholder
    // Body: { "date1": "12-Apr-26", "date2": "04-Apr-26", "date3": "02-Apr-26" }
    @PostMapping("/placeholder")
    public ResponseEntity<List<PlaceholderRow>> buildPlaceholder(
            @RequestBody PlaceholderRequest request) {
        log.info("Placeholder request: {}", request);
        return ResponseEntity.ok(service.buildPlaceholderTable(request));
    }
}