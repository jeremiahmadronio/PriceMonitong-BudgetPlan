package com.budgetwise.budget.catalog.repository;

import com.budgetwise.budget.catalog.entity.DietaryTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DietaryTagRepository extends JpaRepository<DietaryTag, Long> {
}
