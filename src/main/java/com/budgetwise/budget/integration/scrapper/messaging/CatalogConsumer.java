package com.budgetwise.budget.integration.scrapper.messaging;

import com.budgetwise.budget.catalog.service.CatalogIngestionService;
import com.budgetwise.budget.integration.scrapper.dto.ScrapeResultDto;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class CatalogConsumer {

    private final CatalogIngestionService productInfoService;

    public CatalogConsumer(CatalogIngestionService productInfoService) {
        this.productInfoService = productInfoService;
    }


    @RabbitListener(queues = "scraped_data_queue")
    public void receivePythonResult(ScrapeResultDto resultDTO) {

        System.out.println("Received scrape result for URL: " + resultDTO.status());
        productInfoService.processAndSaveScrapeResult(resultDTO);

        System.out.println("Processed scrape result complete");

    }
}
