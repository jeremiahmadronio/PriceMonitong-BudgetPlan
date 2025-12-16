package com.budgetwise.budget.catalog.repository;

import com.budgetwise.budget.catalog.entity.ProductDietaryTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
@Repository
public interface ProductDietaryTagRepository extends JpaRepository <ProductDietaryTag, Long>{

    /**
     * Projection interface to map Product ID to Tag Name.
     */
    interface TagProjection{
        Long getProductId();
        String getDietaryTag();

    }

    /**
     * Batch retrieves dietary tags for a list of products.
     * Uses JOIN to fetch the readable tag name.
     *
     * @param ids List of Product IDs to fetch tags for.
     */
    @Query("""
           SELECT pdt.productInfo.id AS productId,t.tagName AS dietaryTag            
              FROM ProductDietaryTag pdt JOIN pdt.dietaryTag t WHERE pdt.productInfo.id IN :ids """)
    List<TagProjection> findByProductIdIn(@Param("ids") List<Long> ids);
}


