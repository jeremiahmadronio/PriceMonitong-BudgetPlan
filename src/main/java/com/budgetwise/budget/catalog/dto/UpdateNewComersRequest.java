package com.budgetwise.budget.catalog.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdateNewComersRequest {

    private Long id;
    private String productName;
    private String category;
    private String localName;
    private String origin;
}
