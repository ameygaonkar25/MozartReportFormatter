package com.dbviewer.model;

import lombok.Data;
import java.util.List;

@Data
public class EmailRequest {

    private List<String> toEmails;       // recipient email addresses
    private String subject;              // optional override, defaults to "Mozart Performance Result"
    private Long recordId1;              // same as PlaceholderRequest
    private Long recordId2;
    private Long recordId3;
    private String label1;               // friendly label e.g. "12-Apr-26 (Baseline)"
    private String label2;
    private String label3;

    // Configuration table values
    private String threadPoolSize;
    private String jobTime;
    private String sortable;
}