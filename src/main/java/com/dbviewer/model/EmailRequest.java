package com.dbviewer.model;

import lombok.Data;
import java.util.List;

@Data
public class EmailRequest {
    private List<String> toEmails;
    private String subject;
    private String date1;
    private String date2;
    private String date3;
    private String label1;
    private String label2;
    private String label3;
    private String threadPoolSize;
    private String jobTime;
    private String sortable;
}