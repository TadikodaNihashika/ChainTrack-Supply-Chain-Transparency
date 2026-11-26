package com.supplychain.service;

import com.supplychain.model.Product;
import com.supplychain.model.Transaction;
import com.supplychain.repository.ProductRepository;
import com.supplychain.repository.TransactionRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Map;
import java.util.Locale;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final TransactionRepository transactionRepository;
    private final BlockchainService blockchainService;

    public ProductService(
            ProductRepository productRepository,
            TransactionRepository transactionRepository,
            BlockchainService blockchainService) {

        this.productRepository = productRepository;
        this.transactionRepository = transactionRepository;
        this.blockchainService = blockchainService;
    }

    @Transactional
    public Product registerProduct(
            Product product,
            String transactionHash) {

        if (product.getProductId() == null ||
                product.getProductId().isBlank() ||
                product.getProductName() == null ||
                product.getProductName().isBlank() ||
                product.getManufacturer() == null ||
                product.getManufacturer().isBlank() ||
                product.getOrigin() == null ||
                product.getOrigin().isBlank() ||
                product.getDestination() == null ||
                product.getDestination().isBlank()) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "All product details are required"
            );
        }

        if (product.getQuantity() == null ||
                product.getQuantity() <= 0) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Quantity must be greater than zero"
            );
        }

        if (productRepository.existsByProductId(
                product.getProductId())) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Product ID already exists"
            );
        }

        transactionHash = validateTransactionHash(
                transactionHash
        );

        blockchainService.verifyProductRegistration(
                transactionHash,
                product.getProductId(),
                product.getProductName(),
                product.getManufacturer(),
                product.getQuantity(),
                product.getOrigin(),
                product.getDestination()
        );

        product.setStatus("Registered");
        product.setCurrentLocation(product.getOrigin());

        Product savedProduct =
                productRepository.save(product);

        saveTransaction(
                savedProduct,
                "Product Registered",
                transactionHash
        );

        return savedProduct;
    }

    public List<Product> getAllProducts() {

        return productRepository.findAll();
    }

    public Product getProduct(String productId) {

        return productRepository.findByProductId(productId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Product not found"
                        )
                );
    }

    @Transactional
    public Product updateStatus(
            String productId,
            String status,
            String location,
            String transactionHash) {

        if (status == null ||
                !List.of(
                        "Registered",
                        "In Transit",
                        "At Warehouse",
                        "Delivered"
                ).contains(status) ||
                location == null ||
                location.isBlank()) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Valid status and location are required"
            );
        }

        Product product = getProduct(productId);

        transactionHash = validateTransactionHash(
                transactionHash
        );

        blockchainService.verifyStatusUpdate(
                transactionHash,
                productId,
                location,
                status
        );

        product.setStatus(status);
        product.setCurrentLocation(location);

        Product updatedProduct =
                productRepository.save(product);

        saveTransaction(
                updatedProduct,
                "Status Updated",
                transactionHash
        );

        return updatedProduct;
    }

    private String validateTransactionHash(
            String transactionHash) {

        if (transactionHash == null ||
                !transactionHash.matches(
                        "0x[0-9a-fA-F]{64}")) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Valid blockchain transaction hash is required"
            );
        }

        String normalizedHash =
                transactionHash.toLowerCase(Locale.ROOT);

        if (transactionRepository.existsByTransactionHash(
                normalizedHash)) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Blockchain transaction already recorded"
            );
        }

        return normalizedHash;
    }

    private void saveTransaction(
            Product product,
            String action,
            String transactionHash) {

        Transaction transaction = new Transaction();

        transaction.setProductId(product.getProductId());
        transaction.setAction(action);
        transaction.setStatus(product.getStatus());
        transaction.setLocation(product.getCurrentLocation());
        transaction.setTransactionHash(transactionHash);

        transactionRepository.save(transaction);
    }

    public List<Transaction> getAllTransactions() {

        return transactionRepository
                .findAllByOrderByTimestampDesc();
    }

    public List<Transaction> getProductTransactions(
            String productId) {

        getProduct(productId);

        return transactionRepository
                .findByProductIdOrderByTimestampDesc(productId);
    }

    public Map<String, Long> getAnalytics() {

        return Map.of(
                "totalProducts",
                productRepository.count(),

                "registered",
                productRepository.countByStatus("Registered"),

                "inTransit",
                productRepository.countByStatus("In Transit"),

                "atWarehouse",
                productRepository.countByStatus("At Warehouse"),

                "delivered",
                productRepository.countByStatus("Delivered"),

                "transactions",
                transactionRepository.count()
        );
    }
}