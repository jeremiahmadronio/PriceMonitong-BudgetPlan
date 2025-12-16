package com.budgetwise.budget.catalog.entity;

import com.budgetwise.budget.market.entity.MarketLocation;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "daily_price_record")
public class DailyPriceRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @Column
    private double price;
    @Column(length = 20)
    private String unit;
    @Column(length = 250)
    private String origin;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_info_id" , nullable = false)
    private ProductInfo productInfo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "price_report_id" , nullable = false)
    private PriceReport priceReport;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "market_location_id")
    private MarketLocation marketLocation;
}
