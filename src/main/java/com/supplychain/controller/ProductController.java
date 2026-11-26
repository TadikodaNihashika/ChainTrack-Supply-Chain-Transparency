package com.supplychain.controller;

import com.supplychain.model.Product;
import com.supplychain.model.Transaction;
import com.supplychain.service.ProductService;

import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @PostMapping("/products")
    public Product registerProduct(
            @RequestBody Product product,
            @RequestHeader("X-Transaction-Hash")
            String transactionHash) {

        return productService.registerProduct(
                product,
                transactionHash
        );
    }

    @GetMapping("/products")
    public List<Product> getAllProducts() {

        return productService.getAllProducts();
    }

    @GetMapping("/products/{productId}")
    public Product getProduct(
            @PathVariable String productId) {

        return productService.getProduct(productId);
    }

    @PutMapping("/products/{productId}/status")
    public Product updateStatus(
            @PathVariable String productId,
            @RequestBody Map<String, String> request,
            @RequestHeader("X-Transaction-Hash")
            String transactionHash) {

        return productService.updateStatus(
                productId,
                request.get("status"),
                request.get("location"),
                transactionHash
        );
    }

    @GetMapping("/transactions")
    public List<Transaction> getAllTransactions() {

        return productService.getAllTransactions();
    }

    @GetMapping("/transactions/{productId}")
    public List<Transaction> getProductTransactions(
            @PathVariable String productId) {

        return productService
                .getProductTransactions(productId);
    }

    @GetMapping("/analytics")
    public Map<String, Long> getAnalytics() {

        return productService.getAnalytics();
    }
}