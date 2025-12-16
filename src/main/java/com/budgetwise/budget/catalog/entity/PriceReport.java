package com.budgetwise.budget.catalog.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.util.List;
import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "price_report")
public class PriceReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    private LocalDate dateReported;
    @Column
    private LocalDateTime dateProcessed;

    @Column(columnDefinition = "TEXT")
    private String url;

    public enum Status {COMPLETED, FAILED };
    @Enumerated(EnumType.STRING)

    @Column(length = 20)
    private Status status;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
    @UpdateTimestamp
    @Column
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "priceReport",fetch = FetchType.LAZY)
    private List<DailyPriceRecord> records;
}
