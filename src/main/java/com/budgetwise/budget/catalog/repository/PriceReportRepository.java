package com.budgetwise.budget.catalog.repository;

import com.budgetwise.budget.catalog.entity.PriceReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface PriceReportRepository extends JpaRepository<PriceReport, Long> {

    boolean existsByDateReported(LocalDate dateReported);

}
