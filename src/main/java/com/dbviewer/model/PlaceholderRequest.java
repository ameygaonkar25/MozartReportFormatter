package com.dbviewer.model;

import lombok.Data;

@Data
public class PlaceholderRequest {
    private Long recordId1;   // ID of the record assigned to 1st column
    private Long recordId2;   // ID of the record assigned to 2nd column
    private Long recordId3;   // ID of the record assigned to 3rd column
}