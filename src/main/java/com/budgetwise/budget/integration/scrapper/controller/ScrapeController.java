package com.budgetwise.budget.integration.scrapper.controller;


import com.budgetwise.budget.integration.scrapper.service.ScrapperTriggerService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/scrape")
public class ScrapeController {

    private final ScrapperTriggerService scrapperTriggerService;

    public ScrapeController(ScrapperTriggerService scrapperTriggerService) {
        this.scrapperTriggerService = scrapperTriggerService;
    }

    @PostMapping("/trigger")
    public ResponseEntity<String> TriggerScrape() {
        scrapperTriggerService.initiateTrigger();
        return ResponseEntity.accepted().body("Scraping request has been dispatched to Python worker.");
    }

}
