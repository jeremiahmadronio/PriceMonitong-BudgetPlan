package com.budgetwise.budget.integration.scrapper.config;

import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

   @Bean
    public Queue requestQueue() {
        return new Queue("scraped_data_queue", true);
    }

    @Bean
   public Queue resultQueue() {
        return new Queue("scrape_request_queue", true);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }
}
