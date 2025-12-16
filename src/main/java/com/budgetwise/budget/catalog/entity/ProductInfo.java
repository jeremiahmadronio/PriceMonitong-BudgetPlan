package com.budgetwise.budget.catalog.entity;
import java.util.List;
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
@Table(name = "product_info",
                            uniqueConstraints = {
                                    @UniqueConstraint(columnNames = {"product_name", "category"}),

                            })
public class ProductInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;



    @Column(unique = true , nullable = false ,length = 250)
    private String productName;

    @Column(length = 250)
    private String category;

    @Column(length = 250)
    private String localName;

    public enum Status { ACTIVE, INACTIVE,PENDING }
    @Enumerated(EnumType.STRING)
    @Column(length = 20,nullable = false)
    private Status status;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "productInfo",cascade = CascadeType.ALL,fetch = FetchType.LAZY)
    private List<DailyPriceRecord> priceRecords;

    @OneToMany(mappedBy = "productInfo",fetch = FetchType.LAZY)
   private List<ProductDietaryTag> productDietaryTags;


}
