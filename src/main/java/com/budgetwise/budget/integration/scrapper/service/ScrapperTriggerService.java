package com.budgetwise.budget.integration.scrapper.service;

import com.budgetwise.budget.integration.scrapper.messaging.ScraperProducer;
import org.springframework.stereotype.Service;

@Service
public class ScrapperTriggerService {

    private final ScraperProducer scrapperProducer;

    public ScrapperTriggerService(ScraperProducer scrapperProducer) {
        this.scrapperProducer = scrapperProducer;
    }



    public void initiateTrigger() {

        String targetUrl = "https://www.da.gov.ph/price-monitoring/";

        System.out.println("Initiating scrapper trigger for URL: " + targetUrl);

        scrapperProducer.sendScrapeRequest(targetUrl);

    }


}
