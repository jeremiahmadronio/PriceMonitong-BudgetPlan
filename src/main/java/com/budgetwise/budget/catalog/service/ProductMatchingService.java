package com.budgetwise.budget.catalog.service;

import com.budgetwise.budget.catalog.entity.ProductInfo;
import com.budgetwise.budget.catalog.repository.ProductInfoRepository;
import com.budgetwise.budget.integration.scrapper.dto.ScrapeResultDto;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

/**
 * Service responsible for resolving Product Identity.
 * Determines whether a scraped item is an existing ACTIVE product
 * or a new PENDING product based on historical data.
 */
@Service
public class ProductMatchingService {

    private final ProductInfoRepository productInfoRepository;

    public ProductMatchingService(ProductInfoRepository productInfoRepository) {
        this.productInfoRepository = productInfoRepository;


    }

    /**
     * Main decision logic for Product Matching.
     * LOGIC FLOW:
     * 1. Checks if the product (Category + Name) AND Origin (Brand) already exists in history.
     * 2. IF YES (Verified Source): Reactivate or return the Existing Product as ACTIVE.
     * 3. IF NO (New Source/New Product): Return or Create as PENDING (subject for review).
     */
    @Transactional
    public ProductInfo findOrCreateProduct(ScrapeResultDto.ScrapedProduct result){
        // Check if this specific product from this specific origin has appeared before
        // NOTE: Ensure Repository parameters match: (Category, ProductName, Origin)
        boolean exist = productInfoRepository.existsByCategoryAndProductNameAndOrigin(
                result.commodity(),
                result.category(),
                result.origin()
        );

        if(exist){
            return activateExistingProduct(result);
        }else {
            return createOrGetPendingProduct(result);
        }
    }

    /**
     * Handles products that are known and verified (have history).
     * Forces status to ACTIVE.
     */
    private ProductInfo activateExistingProduct(ScrapeResultDto.ScrapedProduct result){

        Optional<ProductInfo> existOPT = productInfoRepository.findByCategoryAndProductName(
                result.category(),
                result.commodity()
        );

        if(existOPT.isEmpty()){
            throw new IllegalStateException("Product should exist but not found in DB");
        }

        ProductInfo product = existOPT.get();
        if(product.getStatus() != ProductInfo.Status.ACTIVE){
            product.setStatus(ProductInfo.Status.ACTIVE);
            productInfoRepository.save(product);
            System.out.println("Re-activated product: " + product.getProductName() + " in category: " + product.getCategory());

        }else {
            System.out.println("Product already active: " + product.getProductName() + " in category: " + product.getCategory());
        }
        return product;

    }

    /**
     * Handles potential new products.
     * If product exists but no history with this origin -> Keep as is (don't force active).
     * If product is totally new -> Create as PENDING.
     */
    private ProductInfo createOrGetPendingProduct(ScrapeResultDto.ScrapedProduct result){

        Optional<ProductInfo> existOPT = productInfoRepository.findByCategoryAndProductName(
                result.category(),
                result.commodity()
        );

        if(existOPT.isPresent()){
           ProductInfo product = existOPT.get();
            System.out.println("Found existing pending product: " + product.getProductName() + " in category: " + product.getCategory());
            return product;
        }else{
            return createNewProduct(result);
        }
    }
    /**
     * Creates a brand new Product entity with PENDING status.
     */
    private ProductInfo createNewProduct(ScrapeResultDto.ScrapedProduct result){

       ProductInfo product = new ProductInfo();


            product.setCategory(result.category());
            product.setProductName(result.commodity());
            product.setStatus(ProductInfo.Status.PENDING);

            ProductInfo savedProduct = productInfoRepository.save(product);
            System.out.println("Created new pending product: " + savedProduct.getProductName() + " in category: " + savedProduct.getCategory());
            return savedProduct;
    }

}
