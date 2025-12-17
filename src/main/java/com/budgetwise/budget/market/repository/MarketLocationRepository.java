package com.budgetwise.budget.market.repository;

import com.budgetwise.budget.market.dto.MarketProductsResponse;
import com.budgetwise.budget.market.dto.MarketTableResponse;
import com.budgetwise.budget.market.entity.MarketLocation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MarketLocationRepository extends JpaRepository<MarketLocation, Long> {


    List<MarketLocation> findByMarketLocationIn(List<String> marketLocations);

    @Query("""
            SELECT COUNT(m) FROM MarketLocation m
            WHERE m.type = :marketType
            """)
   long countByMarketType(@Param("marketType")  MarketLocation.Type marketType);

    @Query("""
            SELECT COUNT(m) FROM MarketLocation m
            WHERE m.status = :status
            """)
    long countByStatus(MarketLocation.Status status);



    /**
     * Fetches market details along with a count of distinct products available in each market.
     * Utilizes a constructor expression (JPQL SELECT new) to return a DTO directly,
     * avoiding the overhead of fetching full Entity objects and N+1 query issues.
     *
     * @param pageable Pagination info.
     * @return Page of MarketTableResponse.
     */
    @Query("""
        SELECT new com.budgetwise.budget.market.dto.MarketTableResponse(
            m.id,
            m.marketLocation,
            m.type,
            m.status,
            COUNT(DISTINCT dpr.productInfo.id)
        )
        FROM MarketLocation m
        LEFT JOIN m.dailyPriceRecords dpr
        GROUP BY m.id, m.marketLocation, m.type, m.status
        ORDER BY m.marketLocation ASC
        """)
    Page<MarketTableResponse> displayMarketInformation(Pageable pageable);



    /**
     * Projects market and product data into a DTO using a JPQL constructor expression.
     * Performance Note:This query uses a correlated subquery:
     * @code (SELECT COUNT(dprSub) ...) to calculate the total products for the market.
     * While this avoids a separate N+1 query for the count, it runs per row.
     * Optimized for single-market retrieval via ID.
     *
     * @param marketId The ID of the market to filter by.
     * @return A list of flattened DTOs containing market info, product details, and price.
     */
    @Query("""
    SELECT new com.budgetwise.budget.market.dto.MarketProductsResponse(
        m.id,
        m.marketLocation,
        m.type,
        (SELECT COUNT(dprSub) FROM DailyPriceRecord dprSub WHERE dprSub.marketLocation.id = m.id),
        p.productName,
        p.category,
        dpr.price,
        dpr.priceReport.dateReported
    )
    FROM MarketLocation m
    JOIN m.dailyPriceRecords dpr
    JOIN dpr.productInfo p
    WHERE m.id = :marketId       
    ORDER BY m.marketLocation ASC, p.productName ASC
    """)
    List<MarketProductsResponse> displayProductByMarketId(@Param("marketId") Long marketId);


    Optional<MarketLocation> findById(Long id);

    boolean existsByMarketLocation(String marketLocation);
    boolean existsByMarketLocationAndIdNot(String marketLocation, Long id);
}
