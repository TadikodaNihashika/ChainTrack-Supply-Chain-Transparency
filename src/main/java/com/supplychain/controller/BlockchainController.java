package com.supplychain.controller;

import com.supplychain.service.BlockchainService;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.LinkedHashMap;

@RestController
@RequestMapping("/api/blockchain")
public class BlockchainController {

    private final BlockchainService blockchainService;

    public BlockchainController(
            BlockchainService blockchainService) {

        this.blockchainService = blockchainService;
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {

        Map<String, Object> response = new LinkedHashMap<>();

        try {

            String chainId = blockchainService.getChainId();

            String latestBlock =
                    blockchainService.getLatestBlock();

            response.put("connected", true);

            response.put(
                    "network",
                    "11155111".equals(chainId)
                    ? "Ethereum Sepolia"
                    : "Unexpected network"
            );

            response.put("chainId", chainId);

            response.put(
                    "contractAddress",
                    blockchainService.getContractAddress()
            );

            response.put("latestBlock", latestBlock);

            return ResponseEntity.ok(response);

        } catch (Exception e) {

            response.put("connected", false);

            response.put(
                    "message",
                    "Blockchain connection failed"
            );

            return ResponseEntity.status(503).body(response);
        }
    }

    @GetMapping("/verify/{transactionHash}")
    public ResponseEntity<Map<String, Object>> verifyTransaction(
            @PathVariable String transactionHash) {

        try {

            Map<String, Object> response =
                    blockchainService.verifyTransaction(
                            transactionHash
                    );

            return ResponseEntity.ok(response);

        } catch (ResponseStatusException e) {

            Map<String, Object> response =
                    new LinkedHashMap<>();

            response.put("verified", false);
            response.put("transactionHash", transactionHash);
            response.put("message", e.getReason());

            return ResponseEntity
                    .status(e.getStatusCode())
                    .body(response);

        } catch (Exception e) {

            Map<String, Object> response =
                    new LinkedHashMap<>();

            response.put("verified", false);

            response.put(
                    "message",
                    "Unable to verify blockchain transaction"
            );

            return ResponseEntity.status(503).body(response);
        }
    }
}