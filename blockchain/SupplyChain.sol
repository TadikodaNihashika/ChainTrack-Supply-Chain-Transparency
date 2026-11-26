
 // SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

contract SupplyChain {

    address public owner;

    constructor() {
        owner = msg.sender;
    }

    modifier onlyOwner() {
        require(msg.sender == owner, "Unauthorized access");
        _;
    }

    struct Product {
        string productId;
        string productName;
        string manufacturer;
        uint256 quantity;
        string origin;
        string destination;
        string currentLocation;
        string status;
        uint256 timestamp;
        bool exists;
    }

    struct History {
        string action;
        string location;
        string status;
        uint256 timestamp;
    }

    mapping(string => Product) private products;

    mapping(string => History[]) private productHistory;

    string[] private productIds;

    event ProductRegistered(
        string productId,
        string productName,
        uint256 timestamp
    );

    event StatusUpdated(
        string productId,
        string location,
        string status,
        uint256 timestamp
    );

    function registerProduct(
        string memory productId,
        string memory productName,
        string memory manufacturer,
        uint256 quantity,
        string memory origin,
        string memory destination
    ) public onlyOwner {

        require(bytes(productId).length > 0, "Invalid product ID");
        require(!products[productId].exists, "Product already exists");
        require(quantity > 0, "Invalid quantity");

        products[productId] = Product(
            productId,
            productName,
            manufacturer,
            quantity,
            origin,
            destination,
            origin,
            "Registered",
            block.timestamp,
            true
        );

        productIds.push(productId);

        productHistory[productId].push(
            History(
                "Product Registered",
                origin,
                "Registered",
                block.timestamp
            )
        );

        emit ProductRegistered(
            productId,
            productName,
            block.timestamp
        );
    }

    function updateStatus(
        string memory productId,
        string memory location,
        string memory status
    ) public onlyOwner {

        require(products[productId].exists, "Product not found");

        bytes32 statusHash = keccak256(bytes(status));

        require(
            statusHash == keccak256(bytes("Registered")) ||
            statusHash == keccak256(bytes("In Transit")) ||
            statusHash == keccak256(bytes("At Warehouse")) ||
            statusHash == keccak256(bytes("Delivered")),
            "Invalid status"
        );

        products[productId].currentLocation = location;
        products[productId].status = status;
        products[productId].timestamp = block.timestamp;

        productHistory[productId].push(
            History(
                "Status Updated",
                location,
                status,
                block.timestamp
            )
        );

        emit StatusUpdated(
            productId,
            location,
            status,
            block.timestamp
        );
    }

    function getProduct(
        string memory productId
    ) public view returns (Product memory) {

        require(products[productId].exists, "Product not found");

        return products[productId];
    }

    function getProductHistory(
        string memory productId
    ) public view returns (History[] memory) {

        require(products[productId].exists, "Product not found");

        return productHistory[productId];
    }

    function getTotalProducts()
        public view returns (uint256) {

        return productIds.length;
    }

    function getAllProductIds()
        public view returns (string[] memory) {

        return productIds;
    }

    function productExists(
        string memory productId
    ) public view returns (bool) {

        return products[productId].exists;
    }
}
