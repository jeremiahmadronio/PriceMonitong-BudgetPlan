package com.budgetwise.budget.catalog.repository;

import com.budgetwise.budget.catalog.entity.DailyPriceRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
@Repository
public interface DailyPriceRecordRepository extends JpaRepository<DailyPriceRecord, Long> {

    /**
     * Projection interface to map the result of the count query.
     */
    public interface MarketCountProjection{
        Long getProductId();
        Long getTotalMarkets();
    }

    /**
     * Batch counts the number of markets for a list of products.
     * Uses GROUP BY to return counts for multiple products in a single query.
     *
     * @param ids List of Product IDs to count markets for.
     */
    @Query("""
        SELECT 
            r.productInfo.id AS productId,     
            COUNT(r.marketLocation) AS totalMarkets
        FROM DailyPriceRecord r 
        WHERE r.productInfo.id IN :ids 
        GROUP BY r.productInfo.id               
    """)
    List<MarketCountProjection> countMarketsByProductIds(@Param("ids") List<Long> ids);
}
