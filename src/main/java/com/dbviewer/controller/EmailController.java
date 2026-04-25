package com.dbviewer.controller;

import com.dbviewer.model.EmailRequest;
import com.dbviewer.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/email")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class EmailController {

    private final EmailService emailService;

    // POST /api/email/send
    // Body: { "toEmails": ["a@b.com","c@d.com"], "recordId1": 33, "recordId2": 17,
    //         "recordId3": 1, "label1": "12-Apr-26", "label2": "04-Apr-26", "label3": "02-Apr-26" }
    @PostMapping("/send")
    public ResponseEntity<Map<String, String>> sendReport(@RequestBody EmailRequest request) {
        if (request.getToEmails() == null || request.getToEmails().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("status", "error", "message", "At least one recipient email is required."));
        }
        if (request.getDate1() == null && request.getDate2() == null && request.getDate3() == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("status", "error", "message", "At least one column must be assigned before sending."));
        }
        try {
            String message = emailService.sendPerformanceReport(request);
            return ResponseEntity.ok(Map.of("status", "success", "message", message));
        } catch (Exception e) {
            log.error("Failed to send email report", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("status", "error", "message", "Failed to send email: " + e.getMessage()));
        }
    }
}