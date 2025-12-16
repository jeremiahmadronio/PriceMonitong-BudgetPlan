package com.budgetwise.budget.market.repository;

import com.budgetwise.budget.market.dto.MarketTableResponse;
import com.budgetwise.budget.market.entity.MarketLocation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

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


}
