    package com.budgetwise.budget.catalog.service;

    import com.budgetwise.budget.catalog.entity.PriceReport;
    import com.budgetwise.budget.catalog.repository.PriceReportRepository;
    import com.budgetwise.budget.integration.scrapper.dto.ScrapeResultDto;
    import org.springframework.stereotype.Service;

    import java.time.LocalDate;
    import java.time.LocalDateTime;

    @Service
    public class PriceReportProcessingService {

        private final PriceReportRepository priceReportRepository;

        public PriceReportProcessingService(PriceReportRepository priceReportRepository){
            this.priceReportRepository = priceReportRepository;
        }



        /**
         * Checks if a report already exists for the given date string.
         * Used by ProductInfoService to decide whether to SKIP the process.
         */
        public boolean reportExists(String dateStr) {
            LocalDate reportDate = parseDate(dateStr);
            return priceReportRepository.existsByDateReported(reportDate);
        }

        /**
         * Creates and saves a new PriceReport based on the data received from the scraper.
         *
         * @param result The DTO containing raw data from the Python microservice.
         * @return The persisted PriceReport entity.
         */
        public PriceReport createFromScrapeResult(ScrapeResultDto result){
            PriceReport priceReport = new PriceReport();

            // Map DTO fields to Entity
            priceReport.setDateReported(parseDate(result.dateProcessed()));
            priceReport.setStatus(determineStatus(result.status()));
            priceReport.setDateProcessed(LocalDateTime.now());
            priceReport.setUrl(result.url());

            return priceReportRepository.save(priceReport);

        }


        /**
         * Safely parses the date string (YYYY-MM-DD).
         * Falls back to the current date if parsing fails or input is null.
         */
        private LocalDate parseDate(String dateStr){
            if(dateStr == null || dateStr.isEmpty()){
                return LocalDate.now();
            }
            try{
                return LocalDate.parse(dateStr);

            }catch(Exception e){
                return LocalDate.now();
            }
        }

        /**
         * Maps external status strings to the internal PriceReport.Status Enum.
         * Uses case-insensitive matching for robustness.
         */
        private PriceReport.Status determineStatus(String statusStr){
            if(statusStr == null){
                return PriceReport.Status.FAILED;
            }
            // Check for "success" substring to handle variations like "Success", "SUCCESS", "success"
            String statusLower = statusStr.toLowerCase();
            if (statusLower.contains("success")) {
                return PriceReport.Status.COMPLETED;
            }
            return PriceReport.Status.FAILED;
        }


    }

