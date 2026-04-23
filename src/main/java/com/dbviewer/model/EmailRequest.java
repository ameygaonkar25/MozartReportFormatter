package com.dbviewer.model;

import lombok.Data;
import java.util.List;

@Data
public class EmailRequest {
    private List<String> toEmails;
    private String subject;
    private Long recordId1;
    private Long recordId2;
    private Long recordId3;
    private String label1;
    private String label2;
    private String label3;
    private String threadPoolSize;
    private String jobTime;
    private String sortable;
}