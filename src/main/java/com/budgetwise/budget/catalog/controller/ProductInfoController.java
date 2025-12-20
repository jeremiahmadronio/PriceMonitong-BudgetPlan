package com.budgetwise.budget.catalog.controller;

import com.budgetwise.budget.catalog.dto.*;
import com.budgetwise.budget.catalog.service.ProductInfoService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/products")
public class ProductInfoController {

    private final ProductInfoService productInfoService;

    public ProductInfoController(ProductInfoService productInfoService) {
        this.productInfoService = productInfoService;
    }


    @GetMapping("/display")
    public ResponseEntity<Page<ProductTableResponse>> displayProducts(
            @PageableDefault(size = 10, sort = "productName", direction = Sort.Direction.ASC) Pageable pageable){

        Page<ProductTableResponse> response = productInfoService.displayProducts(pageable);

        return ResponseEntity.ok(response);
    }


    @GetMapping("/stats")
    public ResponseEntity<ProductStatsResponse> displayProductStats() {

        ProductStatsResponse response = productInfoService.getProductStats();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/newcomers")
    public ResponseEntity<List<ProductNewComersResponse>> displayNewComersProduct() {
       List<ProductNewComersResponse> newcomers =  productInfoService.findNewComersProducts();

       return ResponseEntity.ok(newcomers);
    }

    @PutMapping("/updatenewcomers/{id}")
    public ResponseEntity<UpdateNewComersRequest> updateNewComersProducts(
            @PathVariable("id") Long id,
            @RequestBody UpdateNewComersRequest request) {

        UpdateNewComersRequest response = productInfoService.ManageNewComersProduct(id, request);

        return ResponseEntity.ok(response);
    }

    @PutMapping("/updateStatus")
    public ResponseEntity<UpdateProductStatus> updateProductStatus(@RequestBody UpdateProductStatus request) {

        UpdateProductStatus response = productInfoService.updateProductStatus(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/marketDetails/{id}")
    public ResponseEntity<ProductMarketDetailResponse> getProductMarketDetails(@PathVariable("id") Long id) {
        ProductMarketDetailResponse response = productInfoService.getProductMarketDetails(id);
        return ResponseEntity.ok(response);
    }


    @GetMapping("archive/stats")
    public ResponseEntity<ArchiveStatsResponse> getStats() {
        return ResponseEntity.ok(productInfoService.getArchiveStats());
    }


    @GetMapping("archive/table")
    public ResponseEntity<Page<ArchiveTableResponse>> getArchivedProducts(
            @RequestParam(required = false) String search,

            @PageableDefault(size = 7, sort = "updatedAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(productInfoService.getArchivedProducts(search, pageable));
    }
}
