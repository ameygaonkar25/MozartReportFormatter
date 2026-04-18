package com.dbviewer.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlaceholderRow {

    private Integer noOfJobs;
    private Integer nodes;
    private Double dateOf3rdCol;   // Avg time for 3rd column date
    private Double dateOf2ndCol;   // Avg time for 2nd column date
    private Double dateOf1stCol;   // Avg time for 1st column date
    private Double improvement;    // 2nd col - 1st col (in seconds)
}
