package com.dbviewer.model;

import lombok.Data;

@Data
public class PlaceholderRequest {
    private String date1;   // executionDate (TEST_DATE) for 1st column
    private String date2;   // executionDate (TEST_DATE) for 2nd column
    private String date3;   // executionDate (TEST_DATE) for 3rd column
}