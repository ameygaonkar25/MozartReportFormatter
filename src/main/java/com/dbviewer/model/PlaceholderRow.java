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

    // These are now Strings so they can hold "Not Run" when data is missing
    private String dateOf3rdCol;
    private String dateOf2ndCol;
    private String dateOf1stCol;
    private String improvement;   // "Not Run", "—", or "+Xs" / "-Xs"
}