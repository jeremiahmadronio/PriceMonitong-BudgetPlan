package com.budgetwise.budget.catalog.service;


import com.budgetwise.budget.market.entity.MarketLocation;
import com.budgetwise.budget.catalog.entity.PriceReport;
import com.budgetwise.budget.catalog.entity.ProductInfo;
import com.budgetwise.budget.integration.scrapper.dto.ScrapeResultDto;
import com.budgetwise.budget.market.service.MarketLocationResolver;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class CatalogIngestionService {

   private final PriceReportProcessingService priceReportService;
   private final DailyPriceIngestionService dailyPriceRecordService;
   private final MarketLocationResolver marketLocationService;
   private final ProductMatchingService productMatchingService;

    public CatalogIngestionService(PriceReportProcessingService priceReportService, DailyPriceIngestionService dailyPriceRecordService, MarketLocationResolver marketLocationService, ProductMatchingService productMatchingService) {
        this.priceReportService = priceReportService;
        this.dailyPriceRecordService = dailyPriceRecordService;
        this.marketLocationService = marketLocationService;
        this.productMatchingService = productMatchingService;
    }

    /**
     * Service Orchestrator for Scrape Data Ingestion.
     * Coordinates the flow between Reports, Markets, Products, and Price Records.
     * This acts as the "Manager" ensuring all sub-services work together in one transaction.
     */
    @Transactional
    public void processAndSaveScrapeResult(ScrapeResultDto result){

        //  Check for Existing Report to Prevent Duplicates
        if (priceReportService.reportExists(result.dateProcessed())) {
            System.out.println(" [SKIPPED] Report already exists for date: " + result.dateProcessed());
            System.out.println(" [LOGIC] Aborting process to prevent duplication.");
            return; // <--- EXIT POINT
        }

           //  Create the Report Header
            PriceReport priceReport = priceReportService.createFromScrapeResult(result);
           //  Resolve Markets (Bulk Operation)
            List<MarketLocation> markets =  marketLocationService.findOrCreateMarket(result.coveredMarkets());



            if(result.products() == null || result.products().isEmpty() ){

                System.out.println("No products found");
                return;
            }

        int productCount = 0;
        int totalRecords = 0;

        //  Process Each Product
        for(ScrapeResultDto.ScrapedProduct scrapedProduct : result.products()){
            productCount++;
            System.out.println("Processing product " + productCount + ": " + scrapedProduct.commodity());

            ProductInfo productInfo = productMatchingService.findOrCreateProduct(scrapedProduct);

            dailyPriceRecordService.createRecordForAllMarkets(
                    scrapedProduct,
                    productInfo,
                    priceReport,
                    markets
            );

            totalRecords += markets.size();
        }
        // Final Summary
        System.out.println("Batch Processing Complete!");
        System.out.println("   Report ID: " + priceReport.getId());
        System.out.println("   Products Processed: " + productCount);
        System.out.println("   Total Price Rows Saved: " + totalRecords);
    }

    }
