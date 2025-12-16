package com.budgetwise.budget.catalog.service;

import com.budgetwise.budget.catalog.entity.DailyPriceRecord;
import com.budgetwise.budget.market.entity.MarketLocation;
import com.budgetwise.budget.catalog.entity.PriceReport;
import com.budgetwise.budget.catalog.entity.ProductInfo;
import com.budgetwise.budget.catalog.repository.DailyPriceRecordRepository;
import com.budgetwise.budget.integration.scrapper.dto.ScrapeResultDto;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class DailyPriceIngestionService {

    private final DailyPriceRecordRepository dailyPriceRecordRepository;

    public DailyPriceIngestionService(DailyPriceRecordRepository dailyPriceRecordRepository) {
        this.dailyPriceRecordRepository = dailyPriceRecordRepository;
    }


    /**
     * Broadcasts a single scraped price to ALL covered markets.

     * LOGIC:
     * Since the scraping source (DA PDF) provides a "Prevailing Price"
     * that applies to a list of markets in a region, we create one record per market
     * with the same price value.
     * * @param scrapedProduct The raw price and unit data.
     * @param productInfo    The resolved Product entity (Foreign Key).
     * @param priceReport    The parent Report entity (Foreign Key).
     * @param markets        List of markets where this price applies.
     */
    @Transactional
    public void createRecordForAllMarkets(
            ScrapeResultDto.ScrapedProduct scrapedProduct,
            ProductInfo productInfo,
            PriceReport priceReport,
            List<MarketLocation> markets) {

        if(markets == null || markets.isEmpty()){
            System.out.println("No markets found for product: " + scrapedProduct.commodity());
            return;
        }

        // Batch Collection (Optimization)
        // Instead of saving one by one, we collect them in a list first.
        List<DailyPriceRecord> batchRecords = new ArrayList<>();

        for(MarketLocation market : markets){

            DailyPriceRecord record = new DailyPriceRecord();

            record.setPrice(scrapedProduct.price());
            record.setUnit(scrapedProduct.unit());
            record.setOrigin(scrapedProduct.origin());

            //Connect all entities via foreign keys
            record.setProductInfo(productInfo);
            record.setPriceReport(priceReport);
            record.setMarketLocation(market);

            batchRecords.add(record);

        }
        dailyPriceRecordRepository.saveAll(batchRecords);

        System.out.println("Linked " + batchRecords.size() + " records for: "
                + scrapedProduct.commodity() + " across " + markets.size() + " markets.");
    }



}
