package com.budgetwise.budget.catalog.service;

import com.budgetwise.budget.catalog.entity.PriceReport;
import com.budgetwise.budget.catalog.repository.PriceReportRepository;
import com.budgetwise.budget.integration.scrapper.dto.ScrapeResultDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests for PriceReportService
 * Validates report creation logic, date parsing, status mapping, and repository interactions
 * Uses realistic DA (Department of Agriculture) market data scenarios
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PriceReportService Tests")
class PriceReportProcessingServiceTest {

    @Mock
    private PriceReportRepository priceReportRepository;

    @InjectMocks
    private PriceReportProcessingService priceReportService;

    private ScrapeResultDto validSuccessResult;
    private ScrapeResultDto validFailedResult;
    private ScrapeResultDto invalidDateResult;
    private ScrapeResultDto nullStatusResult;

    @BeforeEach
    void setUp() {
        validSuccessResult = new ScrapeResultDto(
                "SUCCESS",
                "2025-11-10",
                "http://da.gov.ph/price-report-2025-11-10",
                null,
                null
        );

        validFailedResult = new ScrapeResultDto(
                "FAILED",
                "2025-11-11",
                "http://da.gov.ph/price-report-2025-11-11",
                null,
                null
        );

        invalidDateResult = new ScrapeResultDto(
                "successfully done",
                "invalid-date-format",
                "http://da.gov.ph/price-report-invalid",
                null,
                null
        );

        nullStatusResult = new ScrapeResultDto(
                null,
                "2025-11-12",
                "http://da.gov.ph/price-report-2025-11-12",
                null,
                null
        );
    }

    // ==================== HAPPY PATH ====================

    @Test
    @DisplayName("Happy Path: reportExists with valid date - should return true when report exists")
    void reportExists_ValidDateReportExists_ShouldReturnTrue() {
        when(priceReportRepository.existsByDateReported(any(LocalDate.class))).thenReturn(true);

        boolean exists = priceReportService.reportExists("2025-11-10");

        assertTrue(exists);
        ArgumentCaptor<LocalDate> captor = ArgumentCaptor.forClass(LocalDate.class);
        verify(priceReportRepository).existsByDateReported(captor.capture());
        assertEquals(LocalDate.of(2025, 11, 10), captor.getValue());
    }

    @Test
    @DisplayName("Happy Path: reportExists with valid date - should return false when report does not exist")
    void reportExists_ValidDateReportNotExists_ShouldReturnFalse() {
        when(priceReportRepository.existsByDateReported(any(LocalDate.class))).thenReturn(false);

        boolean exists = priceReportService.reportExists("2025-11-09");

        assertFalse(exists);
        ArgumentCaptor<LocalDate> captor = ArgumentCaptor.forClass(LocalDate.class);
        verify(priceReportRepository).existsByDateReported(captor.capture());
        assertEquals(LocalDate.of(2025, 11, 9), captor.getValue());
    }

    @Test
    @DisplayName("Happy Path: createFromScrapeResult with SUCCESS status - should map to COMPLETED and save")
    void createFromScrapeResult_StatusSuccess_ShouldMapToCompletedAndSave() {
        when(priceReportRepository.save(any(PriceReport.class)))
                .thenAnswer(invocation -> {
                    PriceReport p = invocation.getArgument(0);
                    p.setId(123L);
                    return p;
                });

        PriceReport saved = priceReportService.createFromScrapeResult(validSuccessResult);

        assertNotNull(saved);
        assertEquals(123L, saved.getId());
        assertEquals(LocalDate.of(2025, 11, 10), saved.getDateReported());
        assertEquals(PriceReport.Status.COMPLETED, saved.getStatus());
        assertEquals("http://da.gov.ph/price-report-2025-11-10", saved.getUrl());
        assertNotNull(saved.getDateProcessed());

        ArgumentCaptor<PriceReport> captor = ArgumentCaptor.forClass(PriceReport.class);
        verify(priceReportRepository).save(captor.capture());
        PriceReport captured = captor.getValue();
        assertEquals(PriceReport.Status.COMPLETED, captured.getStatus());
    }

    @Test
    @DisplayName("Happy Path: createFromScrapeResult with FAILED status - should map to FAILED and save")
    void createFromScrapeResult_StatusFailed_ShouldMapToFailedAndSave() {
        when(priceReportRepository.save(any(PriceReport.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PriceReport result = priceReportService.createFromScrapeResult(validFailedResult);

        assertNotNull(result);
        assertEquals(PriceReport.Status.FAILED, result.getStatus());
        assertEquals(LocalDate.of(2025, 11, 11), result.getDateReported());
        verify(priceReportRepository).save(any(PriceReport.class));
    }

    // ==================== STATUS MAPPING ====================

    @Test
    @DisplayName("Status Mapping: Null status - should default to FAILED")
    void createFromScrapeResult_NullStatus_ShouldMapToFailed() {
        when(priceReportRepository.save(any(PriceReport.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PriceReport result = priceReportService.createFromScrapeResult(nullStatusResult);

        assertNotNull(result);
        assertEquals(PriceReport.Status.FAILED, result.getStatus());
        verify(priceReportRepository).save(any(PriceReport.class));
    }

    @Test
    @DisplayName("Status Mapping: Case-insensitive SUCCESS - should map to COMPLETED")
    void createFromScrapeResult_CaseInsensitiveSuccess_ShouldMapToCompleted() {
        ScrapeResultDto[] testCases = {
                new ScrapeResultDto("success", "2025-11-15", "http://test1", null, null),
                new ScrapeResultDto("SUCCESS", "2025-11-16", "http://test2", null, null),
                new ScrapeResultDto("Success", "2025-11-17", "http://test3", null, null),
                new ScrapeResultDto("successfully done", "2025-11-18", "http://test4", null, null)
        };

        when(priceReportRepository.save(any(PriceReport.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        for (ScrapeResultDto testCase : testCases) {
            PriceReport result = priceReportService.createFromScrapeResult(testCase);
            assertEquals(PriceReport.Status.COMPLETED, result.getStatus(),
                    "Status '" + testCase.status() + "' should map to COMPLETED");
        }
    }

    @Test
    @DisplayName("Status Mapping: Non-success strings - should map to FAILED")
    void createFromScrapeResult_NonSuccessStatus_ShouldMapToFailed() {
        ScrapeResultDto[] testCases = {
                new ScrapeResultDto("error", "2025-11-19", "http://test5", null, null),
                new ScrapeResultDto("failed", "2025-11-20", "http://test6", null, null),
                new ScrapeResultDto("timeout", "2025-11-21", "http://test7", null, null),
                new ScrapeResultDto("unknown", "2025-11-22", "http://test8", null, null)
        };

        when(priceReportRepository.save(any(PriceReport.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        for (ScrapeResultDto testCase : testCases) {
            PriceReport result = priceReportService.createFromScrapeResult(testCase);
            assertEquals(PriceReport.Status.FAILED, result.getStatus(),
                    "Status '" + testCase.status() + "' should map to FAILED");
        }
    }

    // ==================== DATE PARSING ====================

    @Test
    @DisplayName("Date Parsing: Valid YYYY-MM-DD format - should parse correctly")
    void reportExists_ValidDateFormat_ShouldParseCorrectly() {
        when(priceReportRepository.existsByDateReported(any(LocalDate.class))).thenReturn(true);

        priceReportService.reportExists("2025-11-10");

        ArgumentCaptor<LocalDate> captor = ArgumentCaptor.forClass(LocalDate.class);
        verify(priceReportRepository).existsByDateReported(captor.capture());
        assertEquals(LocalDate.of(2025, 11, 10), captor.getValue());
    }

    @Test
    @DisplayName("Date Parsing: Invalid date format - should fall back to today")
    void reportExists_InvalidDateFormat_ShouldUseTodayAsDefault() {
        when(priceReportRepository.existsByDateReported(any(LocalDate.class))).thenReturn(false);

        priceReportService.reportExists("not-a-valid-date");

        ArgumentCaptor<LocalDate> captor = ArgumentCaptor.forClass(LocalDate.class);
        verify(priceReportRepository).existsByDateReported(captor.capture());
        assertEquals(LocalDate.now(), captor.getValue());
    }

    @Test
    @DisplayName("Date Parsing: Null date string - should fall back to today")
    void reportExists_NullDateString_ShouldUseTodayAsDefault() {
        when(priceReportRepository.existsByDateReported(any(LocalDate.class))).thenReturn(false);

        priceReportService.reportExists(null);

        ArgumentCaptor<LocalDate> captor = ArgumentCaptor.forClass(LocalDate.class);
        verify(priceReportRepository).existsByDateReported(captor.capture());
        assertEquals(LocalDate.now(), captor.getValue());
    }

    @Test
    @DisplayName("Date Parsing: Empty date string - should fall back to today")
    void reportExists_EmptyDateString_ShouldUseTodayAsDefault() {
        when(priceReportRepository.existsByDateReported(any(LocalDate.class))).thenReturn(false);

        priceReportService.reportExists("");

        ArgumentCaptor<LocalDate> captor = ArgumentCaptor.forClass(LocalDate.class);
        verify(priceReportRepository).existsByDateReported(captor.capture());
        assertEquals(LocalDate.now(), captor.getValue());
    }

    @Test
    @DisplayName("Date Parsing: Invalid format in createFromScrapeResult - should use today for dateReported")
    void createFromScrapeResult_InvalidDateInDto_ShouldUseTodayForDateReported() {
        when(priceReportRepository.save(any(PriceReport.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PriceReport result = priceReportService.createFromScrapeResult(invalidDateResult);

        assertNotNull(result);
        assertEquals(LocalDate.now(), result.getDateReported());
        assertEquals(PriceReport.Status.COMPLETED, result.getStatus());
    }

    // ==================== DATE PROCESSED ====================

    @Test
    @DisplayName("Date Processed: Should be set to current time when report is created")
    void createFromScrapeResult_DateProcessedShouldBeCurrentTime() {
        LocalDateTime beforeCreation = LocalDateTime.now();
        when(priceReportRepository.save(any(PriceReport.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PriceReport result = priceReportService.createFromScrapeResult(validSuccessResult);
        LocalDateTime afterCreation = LocalDateTime.now();

        assertNotNull(result.getDateProcessed());
        assertTrue(!result.getDateProcessed().isBefore(beforeCreation.minusSeconds(1)));
        assertTrue(!result.getDateProcessed().isAfter(afterCreation.plusSeconds(1)));
    }

    @Test
    @DisplayName("Date Processed: Should not be null in saved entity")
    void createFromScrapeResult_DateProcessedNotNull() {
        when(priceReportRepository.save(any(PriceReport.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PriceReport result = priceReportService.createFromScrapeResult(validSuccessResult);

        assertNotNull(result.getDateProcessed());
    }

    // ==================== REPOSITORY INTERACTION ====================

    @Test
    @DisplayName("Repository: save() should be called exactly once per createFromScrapeResult")
    void createFromScrapeResult_VerifyRepositorySaveCalledOnce() {
        when(priceReportRepository.save(any(PriceReport.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        priceReportService.createFromScrapeResult(validSuccessResult);

        verify(priceReportRepository, times(1)).save(any(PriceReport.class));
    }

    @Test
    @DisplayName("Repository: existsByDateReported() should be called with parsed LocalDate")
    void reportExists_VerifyRepositoryCallWithParsedDate() {
        when(priceReportRepository.existsByDateReported(any(LocalDate.class))).thenReturn(true);

        priceReportService.reportExists("2025-11-10");

        ArgumentCaptor<LocalDate> captor = ArgumentCaptor.forClass(LocalDate.class);
        verify(priceReportRepository, times(1)).existsByDateReported(captor.capture());
        assertEquals(LocalDate.of(2025, 11, 10), captor.getValue());
    }

    @Test
    @DisplayName("Repository: Verify correct entity fields are saved")
    void createFromScrapeResult_VerifyEntityFieldsInSave() {
        when(priceReportRepository.save(any(PriceReport.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        priceReportService.createFromScrapeResult(validSuccessResult);

        ArgumentCaptor<PriceReport> captor = ArgumentCaptor.forClass(PriceReport.class);
        verify(priceReportRepository).save(captor.capture());
        PriceReport savedReport = captor.getValue();

        assertEquals(LocalDate.of(2025, 11, 10), savedReport.getDateReported());
        assertEquals(PriceReport.Status.COMPLETED, savedReport.getStatus());
        assertEquals("http://da.gov.ph/price-report-2025-11-10", savedReport.getUrl());
        assertNotNull(savedReport.getDateProcessed());
    }

    // ==================== EDGE CASES ====================

    @Test
    @DisplayName("Edge Case: Leap year date - should parse correctly")
    void reportExists_LeapYearDate_ShouldParseCorrectly() {
        when(priceReportRepository.existsByDateReported(any(LocalDate.class))).thenReturn(true);

        priceReportService.reportExists("2024-02-29");

        ArgumentCaptor<LocalDate> captor = ArgumentCaptor.forClass(LocalDate.class);
        verify(priceReportRepository).existsByDateReported(captor.capture());
        assertEquals(LocalDate.of(2024, 2, 29), captor.getValue());
    }

    @Test
    @DisplayName("Edge Case: Year boundary - should handle December 31 to January 1 transition")
    void reportExists_YearBoundary_ShouldParseCorrectly() {
        when(priceReportRepository.existsByDateReported(any(LocalDate.class))).thenReturn(true);

        priceReportService.reportExists("2025-12-31");

        ArgumentCaptor<LocalDate> captor = ArgumentCaptor.forClass(LocalDate.class);
        verify(priceReportRepository).existsByDateReported(captor.capture());
        assertEquals(LocalDate.of(2025, 12, 31), captor.getValue());
    }

    @Test
    @DisplayName("Edge Case: Special characters in URL - should preserve URL as-is")
    void createFromScrapeResult_SpecialCharactersInUrl_ShouldPreserveUrl() {
        ScrapeResultDto dtoWithSpecialUrl = new ScrapeResultDto(
                "SUCCESS",
                "2025-11-10",
                "http://da.gov.ph/report?id=123&date=2025-11-10#section",
                null,
                null
        );
        when(priceReportRepository.save(any(PriceReport.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PriceReport result = priceReportService.createFromScrapeResult(dtoWithSpecialUrl);

        assertEquals("http://da.gov.ph/report?id=123&date=2025-11-10#section", result.getUrl());
    }

    // ==================== EXCEPTION CASES ====================

    @Test
    @DisplayName("Exception: Repository throws exception during exists check")
    void reportExists_RepositoryThrowsException_ShouldPropagateException() {
        when(priceReportRepository.existsByDateReported(any(LocalDate.class)))
                .thenThrow(new RuntimeException("Database connection error"));

        assertThrows(RuntimeException.class, () -> priceReportService.reportExists("2025-11-10"));
    }

    @Test
    @DisplayName("Exception: Repository throws exception during save")
    void createFromScrapeResult_RepositoryThrowsException_ShouldPropagateException() {
        when(priceReportRepository.save(any(PriceReport.class)))
                .thenThrow(new RuntimeException("Database save error"));

        assertThrows(RuntimeException.class,
                () -> priceReportService.createFromScrapeResult(validSuccessResult));
    }

    // ==================== REAL WORLD SCENARIOS ====================

    @Test
    @DisplayName("Real World: Multiple reports in succession - should handle date transitions")
    void createFromScrapeResult_MultipleReportsDifferentDates_ShouldHandleCorrectly() {
        when(priceReportRepository.save(any(PriceReport.class)))
                .thenAnswer(invocation -> {
                    PriceReport report = invocation.getArgument(0);
                    report.setId(System.nanoTime());
                    return report;
                });

        ScrapeResultDto report1 = new ScrapeResultDto("SUCCESS", "2025-11-08", "http://url1", null, null);
        ScrapeResultDto report2 = new ScrapeResultDto("SUCCESS", "2025-11-09", "http://url2", null, null);
        ScrapeResultDto report3 = new ScrapeResultDto("SUCCESS", "2025-11-10", "http://url3", null, null);

        PriceReport result1 = priceReportService.createFromScrapeResult(report1);
        PriceReport result2 = priceReportService.createFromScrapeResult(report2);
        PriceReport result3 = priceReportService.createFromScrapeResult(report3);

        assertEquals(LocalDate.of(2025, 11, 8), result1.getDateReported());
        assertEquals(LocalDate.of(2025, 11, 9), result2.getDateReported());
        assertEquals(LocalDate.of(2025, 11, 10), result3.getDateReported());
        verify(priceReportRepository, times(3)).save(any(PriceReport.class));
    }

    @Test
    @DisplayName("Real World: Duplicate report check for same date - should prevent duplicates")
    void reportExists_DuplicateReportSameDateCheck_ShouldReturnTrue() {
        when(priceReportRepository.existsByDateReported(any(LocalDate.class))).thenReturn(true);

        boolean firstCheck = priceReportService.reportExists("2025-11-10");
        boolean secondCheck = priceReportService.reportExists("2025-11-10");

        assertTrue(firstCheck);
        assertTrue(secondCheck);
        verify(priceReportRepository, times(2)).existsByDateReported(any(LocalDate.class));
    }
}
