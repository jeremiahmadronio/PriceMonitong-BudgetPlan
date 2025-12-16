package com.budgetwise.budget.catalog.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "product_dietary_tag")
public class ProductDietaryTag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_info_id",nullable = false)
    private ProductInfo productInfo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dietary_tag_id",nullable = false)
    private DietaryTag dietaryTag;


}
