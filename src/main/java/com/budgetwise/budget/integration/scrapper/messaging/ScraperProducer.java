package com.budgetwise.budget.integration.scrapper.messaging;

import com.budgetwise.budget.integration.scrapper.dto.ScrapeRequestDto;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
public class ScraperProducer {

    private final RabbitTemplate rabbitTemplate;

    public ScraperProducer(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void sendScrapeRequest(String url) {

        ScrapeRequestDto requestDTO = new ScrapeRequestDto(url);



        System.out.println("Sending JSON request for URL: " + url);

        rabbitTemplate.convertAndSend("scrape_request_queue", requestDTO);
    }



}
