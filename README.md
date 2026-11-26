# ChainTrack – Supply Chain Transparency Analytics Using Distributed Ledger

## Project Overview

ChainTrack is a blockchain-based supply chain management and analytics application designed to improve transparency, traceability and accountability across supply chain operations.

The application integrates Ethereum blockchain technology with a Java Spring Boot backend, MySQL database and an interactive web interface.

Product registration and status updates are recorded through Solidity smart contracts on the Ethereum Sepolia test network. Blockchain transaction hashes are stored in MySQL for tracking and verification.

## Key Features

- Supply chain dashboard
- Blockchain-based product registration
- Product tracking using unique product IDs
- Product location and status updates
- Transaction history management
- Supply chain analytics
- Delivery rate calculation
- Ethereum transaction verification
- MetaMask wallet integration
- Etherscan transaction exploration
- Smart contract owner-based write authorization

## Technologies Used

| Component | Technology |
|---|---|
| Frontend | HTML, CSS, JavaScript |
| Backend | Java, Spring Boot |
| Database | MySQL |
| Blockchain | Ethereum Sepolia |
| Smart Contracts | Solidity |
| Blockchain Integration | Web3j, Ethers.js |
| Wallet | MetaMask |
| Smart Contract Development | Remix IDE |
| Development Environment | Visual Studio Code |
| Build Tool | Maven |
| Version Control | Git and GitHub |

## System Architecture

The system consists of four major layers:

1. Presentation Layer – HTML, CSS and JavaScript.
2. Application Layer – Java Spring Boot REST APIs.
3. Database Layer – MySQL for product records and transaction history.
4. Blockchain Layer – Ethereum and Solidity smart contracts.

MetaMask is used to authorize blockchain transactions. The backend verifies successful transactions before saving the associated product records in MySQL.

## Application Modules

### Dashboard
Displays product counts and supply chain activity.

### Register Product
Registers product information on Ethereum and stores verified records in MySQL.

### Track Product
Retrieves product information, current location and delivery status.

### Update Status
Updates product movement and status through blockchain transactions.

### Transaction History
Displays registration and status update records with associated transaction hashes where available.

### Analytics
Provides product status distribution and delivery performance statistics.

### Blockchain Verification
Verifies transaction success, contract address, sender address and block number on Ethereum Sepolia.

## Project Structure

```text
supplychain/
├── blockchain/
│   ├── SupplyChain.sol
│   └── BlockchainDetails.txt
├── src/
│   └── main/
│       ├── java/com/supplychain/
│       │   ├── controller/
│       │   ├── model/
│       │   ├── repository/
│       │   ├── service/
│       │   └── SupplychainApplication.java
│       └── resources/
│           ├── static/
│           │   ├── index.html
│           │   ├── style.css
│           │   └── script.js
│           └── application.properties
├── pom.xml
├── mvnw
├── mvnw.cmd
├── .gitignore
└── README.md
```

## How to Run the Application

### Prerequisites

- Java JDK 21
- MySQL Server
- Visual Studio Code
- MetaMask browser extension
- Ethereum Sepolia test ETH
- Internet connection

### Database Setup

Create a MySQL database named:

```sql
CREATE DATABASE supplychain_db;
```

Create a local MySQL user named supplychain_user and grant the required permissions on supplychain_db.

Configure the database password using an environment variable.

### Start the Application

Open PowerShell in the project directory and execute:

```powershell
$env:DB_PASSWORD="YOUR_DATABASE_PASSWORD"
$env:ETHEREUM_RPC_URL="https://ethereum-sepolia-rpc.publicnode.com"
.\mvnw.cmd spring-boot:run
```

Open the application in a browser:

http://localhost:8081

Connect MetaMask to the Ethereum Sepolia test network.

Product registration and status updates require the deployed smart contract owner's wallet.

## Blockchain Details

- Network: Ethereum Sepolia Testnet
- Chain ID: 11155111
- Smart Contract Language: Solidity
- Smart Contract Address: 0xb8D9da6bb643DC05329908B46bB5caFd9363e3aC

Blockchain Explorer:

https://sepolia.etherscan.io/

## Security Features

- Environment variable-based credential configuration
- MetaMask transaction authorization
- Smart contract owner-based access control
- Blockchain transaction receipt verification
- Transaction hash validation
- Duplicate transaction hash prevention

## Current Limitations

- The application operates on an Ethereum test network.
- Blockchain write operations are restricted to the contract owner.
- Earlier database-only records may not have blockchain transaction hashes.
- Blockchain and database operations are not atomic.

## Future Enhancements

- QR code-based product tracking
- Multi-role stakeholder access
- IoT-based supply chain monitoring
- Advanced predictive analytics
- Automated blockchain and database reconciliation
- Mainnet deployment

## Project Team

**Batch Number:** 213

**Institution:** KL University

| Team Member | Student ID |
|---|---|
| Sai Ram (Team Leader) | 2300033398 |
| P. Harshini | 2300032210 |
| V. Manaswini | 2300032756 |
| T. Nihashika | 2300033710 |

**Project Guide:** Venkateshwar Rao

## Conclusion

ChainTrack demonstrates the integration of distributed ledger technology with conventional enterprise applications to improve supply chain transparency, product traceability and transaction accountability.

Developed as an academic capstone project at KL University.
