package com.budgetwise.budget.catalog.dto;

import com.budgetwise.budget.catalog.entity.ProductInfo;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProductNewComersResponse {

    private long id;

    private String productName;
    private String category;
    private String origin;
    private String localName;
    private String unit;
    private Double price;
    private int totalMarkets;
    private LocalDate detectedDate;


}
