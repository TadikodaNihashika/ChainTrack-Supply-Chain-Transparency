package com.supplychain.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.core.methods.response.EthCall;
import org.web3j.protocol.core.methods.response.Log;

import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.EventEncoder;
import org.web3j.abi.TypeReference;

import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Event;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.Utf8String;
import org.web3j.abi.datatypes.Bool;
import org.web3j.abi.datatypes.generated.Uint256;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.math.BigInteger;

@Service
@SuppressWarnings("rawtypes")
public class BlockchainService {

    private final Web3j web3j;
    private final String contractAddress;

    private static final Event PRODUCT_REGISTERED =
            new Event(
                    "ProductRegistered",
                    List.of(
                            new TypeReference<Utf8String>() {},
                            new TypeReference<Utf8String>() {},
                            new TypeReference<Uint256>() {}
                    )
            );

    private static final Event STATUS_UPDATED =
            new Event(
                    "StatusUpdated",
                    List.of(
                            new TypeReference<Utf8String>() {},
                            new TypeReference<Utf8String>() {},
                            new TypeReference<Utf8String>() {},
                            new TypeReference<Uint256>() {}
                    )
            );

    public BlockchainService(
            @Value("${ethereum.rpc-url}") String rpcUrl,
            @Value("${ethereum.contract-address}") String contractAddress) {

        this.web3j = Web3j.build(new HttpService(rpcUrl));
        this.contractAddress = contractAddress;
    }

    public String getChainId() throws IOException {

        return web3j.ethChainId()
                .send()
                .getChainId()
                .toString();
    }

    public String getContractAddress() {
        return contractAddress;
    }

    public String getLatestBlock() throws IOException {

        return web3j.ethBlockNumber()
                .send()
                .getBlockNumber()
                .toString();
    }

    private ResponseStatusException badRequest(String message) {

        return new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                message
        );
    }

    private boolean matchesEvent(Log log, Event event) {

        return contractAddress.equalsIgnoreCase(log.getAddress())
                && log.getTopics() != null
                && !log.getTopics().isEmpty()
                && EventEncoder.encode(event)
                .equalsIgnoreCase(log.getTopics().get(0));
    }

    private TransactionReceipt getVerifiedReceipt(
            String transactionHash) throws IOException {

        if (transactionHash == null ||
                !transactionHash.matches("0x[0-9a-fA-F]{64}")) {

            throw badRequest("Invalid transaction hash");
        }

        if (!"11155111".equals(getChainId())) {

            throw badRequest("Incorrect Ethereum network");
        }

        TransactionReceipt receipt = web3j
                .ethGetTransactionReceipt(transactionHash)
                .send()
                .getTransactionReceipt()
                .orElseThrow(() ->
                        badRequest(
                                "Transaction not found or still pending"
                        )
                );

        if (!receipt.isStatusOK()) {

            throw badRequest("Blockchain transaction failed");
        }

        boolean validEvent = receipt.getLogs()
                .stream()
                .anyMatch(log ->
                        matchesEvent(log, PRODUCT_REGISTERED)
                        || matchesEvent(log, STATUS_UPDATED)
                );

        if (!validEvent) {

            throw badRequest(
                    "No valid SupplyChain event found"
            );
        }

        return receipt;
    }

    public Map<String, Object> verifyTransaction(
            String transactionHash) {

        try {

            TransactionReceipt receipt =
                    getVerifiedReceipt(transactionHash);

            return Map.of(
                    "verified", true,
                    "transactionHash", receipt.getTransactionHash(),
                    "network", "Ethereum Sepolia",
                    "contractAddress", contractAddress,
                    "blockNumber", receipt.getBlockNumber().toString(),
                    "from", receipt.getFrom(),
                    "status", "Success",
                    "explorerUrl",
                    "https://sepolia.etherscan.io/tx/"
                            + receipt.getTransactionHash()
            );

        } catch (IOException e) {

            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Unable to connect to Ethereum blockchain"
            );
        }
    }

    private boolean containsMatchingEvent(
            TransactionReceipt receipt,
            Event event,
            String... expectedValues) {

        for (Log log : receipt.getLogs()) {

            if (!matchesEvent(log, event)) {
                continue;
            }

            List<Type> values = FunctionReturnDecoder.decode(
                    log.getData(),
                    event.getNonIndexedParameters()
            );

            if (values.size() != expectedValues.length + 1) {
                continue;
            }

            boolean matched = true;

            for (int i = 0; i < expectedValues.length; i++) {

                if (!expectedValues[i].equals(
                        values.get(i).getValue().toString())) {

                    matched = false;
                    break;
                }
            }

            if (matched) {
                return true;
            }
        }

        return false;
    }

    private List<Type> readProduct(
            String productId) throws IOException {

        Function function = new Function(
                "getProduct",
                List.of(new Utf8String(productId)),
                List.of(
                        new TypeReference<Utf8String>() {},
                        new TypeReference<Utf8String>() {},
                        new TypeReference<Utf8String>() {},
                        new TypeReference<Uint256>() {},
                        new TypeReference<Utf8String>() {},
                        new TypeReference<Utf8String>() {},
                        new TypeReference<Utf8String>() {},
                        new TypeReference<Utf8String>() {},
                        new TypeReference<Uint256>() {},
                        new TypeReference<Bool>() {}
                )
        );

        String encodedFunction =
                FunctionEncoder.encode(function);

        EthCall response = web3j.ethCall(
                org.web3j.protocol.core.methods.request.Transaction
                        .createEthCallTransaction(
                                null,
                                contractAddress,
                                encodedFunction
                        ),
                DefaultBlockParameterName.LATEST
        ).send();

        if (response.hasError() || response.isReverted()) {

            throw badRequest(
                    "Unable to read product from blockchain"
            );
        }

        String rawData = response.getValue();

        if (rawData == null ||
                !rawData.startsWith("0x") ||
                rawData.length() < 66) {

            throw badRequest(
                    "Invalid blockchain product response"
            );
        }

        try {

            String hexData = rawData.substring(2);

            int tupleOffset = new BigInteger(
                    hexData.substring(0, 64),
                    16
            ).intValueExact();

            int start = Math.multiplyExact(tupleOffset, 2);

            if (tupleOffset < 32 ||
                    tupleOffset % 32 != 0 ||
                    start > hexData.length() - 640) {

                throw new IllegalArgumentException(
                        "Invalid tuple offset"
                );
            }

            String tupleData =
                    "0x" + hexData.substring(start);

            List<Type> decoded =
                    FunctionReturnDecoder.decode(
                            tupleData,
                            function.getOutputParameters()
                    );

            if (decoded.size() != 10) {

                throw new IllegalArgumentException(
                        "Unexpected product field count"
                );
            }

            return decoded;

        } catch (RuntimeException e) {

            throw badRequest(
                    "Unable to decode blockchain product data"
            );
        }
    }

    public void verifyProductRegistration(
            String transactionHash,
            String productId,
            String productName,
            String manufacturer,
            Long quantity,
            String origin,
            String destination) {

        if (quantity == null || quantity <= 0) {

            throw badRequest(
                    "Quantity must be greater than zero"
            );
        }

        try {

            TransactionReceipt receipt =
                    getVerifiedReceipt(transactionHash);

            boolean validRegistration =
                    containsMatchingEvent(
                            receipt,
                            PRODUCT_REGISTERED,
                            productId,
                            productName
                    );

            if (!validRegistration) {

                throw badRequest(
                        "Matching product registration event not found"
                );
            }

            List<Type> product = readProduct(productId);

            boolean matchingProduct =
                    product.size() == 10
                    && productId.equals(product.get(0).getValue())
                    && productName.equals(product.get(1).getValue())
                    && manufacturer.equals(product.get(2).getValue())
                    && BigInteger.valueOf(quantity)
                            .equals(product.get(3).getValue())
                    && origin.equals(product.get(4).getValue())
                    && destination.equals(product.get(5).getValue())
                    && origin.equals(product.get(6).getValue())
                    && "Registered".equals(product.get(7).getValue())
                    && Boolean.TRUE.equals(product.get(9).getValue());

            if (!matchingProduct) {

                throw badRequest(
                        "Product details do not match blockchain storage"
                );
            }

        } catch (IOException e) {

            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Unable to verify product registration"
            );
        }
    }

    public void verifyStatusUpdate(
            String transactionHash,
            String productId,
            String location,
            String status) {

        try {

            TransactionReceipt receipt =
                    getVerifiedReceipt(transactionHash);

            boolean validUpdate = containsMatchingEvent(
                    receipt,
                    STATUS_UPDATED,
                    productId,
                    location,
                    status
            );

            if (!validUpdate) {

                throw badRequest(
                        "Matching status update event not found"
                );
            }

            List<Type> product = readProduct(productId);

            if (product.size() != 10
                    || !productId.equals(product.get(0).getValue())
                    || !location.equals(product.get(6).getValue())
                    || !status.equals(product.get(7).getValue())
                    || !Boolean.TRUE.equals(product.get(9).getValue())) {

                throw badRequest(
                        "Status does not match current blockchain storage"
                );
            }

        } catch (IOException e) {

            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Unable to verify status update"
            );
        }
    }
}