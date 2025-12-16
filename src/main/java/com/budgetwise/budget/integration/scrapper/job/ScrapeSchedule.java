package com.budgetwise.budget.integration.scrapper.job;

import com.budgetwise.budget.integration.scrapper.service.ScrapperTriggerService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ScrapeSchedule {

    private final ScrapperTriggerService scrapeTriggerService;

    public ScrapeSchedule(ScrapperTriggerService scrapeTriggerService) {
        this.scrapeTriggerService = scrapeTriggerService;
    }

    @Scheduled(cron = "0 0 1 * * ?") // Runs daily at 1 AM
    public void runDailyScrape() {
        scrapeTriggerService.initiateTrigger();
    }

}
