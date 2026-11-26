// ETHEREUM SMART CONTRACT CONFIGURATION

const CONTRACT_ADDRESS =
    "0xb8D9da6bb643DC05329908B46bB5caFd9363e3aC";

const CONTRACT_ABI = [
    "function owner() view returns (address)",

    "function registerProduct(string productId, string productName, string manufacturer, uint256 quantity, string origin, string destination)",

    "function updateStatus(string productId, string location, string status)",

    "function productExists(string productId) view returns (bool)",

    "function getTotalProducts() view returns (uint256)",

    "event ProductRegistered(string productId, string productName, uint256 timestamp)",

    "event StatusUpdated(string productId, string location, string status, uint256 timestamp)"
];

async function getSmartContract() {

    if (!window.ethereum) {
        throw new Error("MetaMask is not installed.");
    }

    await window.ethereum.request({
        method: "eth_requestAccounts"
    });

    await window.ethereum.request({
        method: "wallet_switchEthereumChain",
        params: [{ chainId: "0xaa36a7" }]
    });

    const provider = new ethers.BrowserProvider(
        window.ethereum
    );

    const signer = await provider.getSigner();

    const code = await provider.getCode(CONTRACT_ADDRESS);

    if (code === "0x") {
        throw new Error("Smart contract not found.");
    }

    const contract = new ethers.Contract(
        CONTRACT_ADDRESS,
        CONTRACT_ABI,
        signer
    );

    const owner = await contract.owner();

    const account = await signer.getAddress();

    if (owner.toLowerCase() !== account.toLowerCase()) {
        throw new Error(
            "Connect the smart contract owner's wallet."
        );
    }

    return contract;
}
function showPage(pageId) {
    document.querySelectorAll(".page").forEach(page => {
        page.classList.add("hidden");
    });

    document.getElementById(pageId).classList.remove("hidden");

    document.querySelectorAll(".sidebar button").forEach(button => {
        button.classList.remove("active");
    });

    const button = document.querySelector(
        `.sidebar button[onclick="showPage('${pageId}')"]`
    );

    if (button) {
        button.classList.add("active");
    }

    if (pageId === "dashboard") {
        loadDashboard();
    }

    if (pageId === "transactions") {
        loadTransactions();
    }

    if (pageId === "analytics") {
        loadAnalytics();
    }
}


// DASHBOARD

async function loadDashboard() {
    const table = document.getElementById("recentProducts");

    try {
        const [productResponse, transactionResponse] =
            await Promise.all([
                fetch("/api/products"),
                fetch("/api/transactions")
            ]);

        if (!productResponse.ok || !transactionResponse.ok) {
            throw new Error("Unable to load dashboard.");
        }

        const products = await productResponse.json();
        const transactions = await transactionResponse.json();

        document.getElementById("totalProducts").textContent =
            products.length;

        document.getElementById("inTransit").textContent =
            products.filter(p => p.status === "In Transit").length;

        document.getElementById("delivered").textContent =
            products.filter(p => p.status === "Delivered").length;

        document.getElementById("totalTransactions").textContent =
            transactions.length;

        table.innerHTML = "";

        if (products.length === 0) {
            table.innerHTML =
                '<tr><td colspan="4">No products registered yet.</td></tr>';
            return;
        }

        const recentProducts = [...products].sort(
            (a, b) => new Date(b.createdAt) - new Date(a.createdAt)
        ).slice(0, 10);

        recentProducts.forEach(product => {
            const row = table.insertRow();

            row.insertCell().textContent = product.productId;
            row.insertCell().textContent = product.productName;
            row.insertCell().textContent = product.manufacturer;
            row.insertCell().textContent = product.status;
        });

    } catch (error) {
        ["totalProducts", "inTransit", "delivered",
         "totalTransactions"].forEach(id => {
            document.getElementById(id).textContent = "—";
        });

        table.innerHTML =
            '<tr><td colspan="4">Unable to load products.</td></tr>';

        console.error(error);
    }
}
// PRODUCT REGISTRATION

document.getElementById("productForm")
    .addEventListener("submit", async function(event) {

        event.preventDefault();

        const message = document.getElementById("registerMessage");
        const button = this.querySelector('[type="submit"]');

        if (button) button.disabled = true;

        let transactionHash = null;
        let confirmed = false;

        const product = {
            productId: document.getElementById("productId").value.trim(),
            productName: document.getElementById("productName").value.trim(),
            manufacturer: document.getElementById("manufacturer").value.trim(),
            quantity: Number(document.getElementById("quantity").value),
            origin: document.getElementById("origin").value.trim(),
            destination: document.getElementById("destination").value.trim()
        };

        try {

            if (!product.productId ||
                !product.productName ||
                !product.manufacturer ||
                !product.origin ||
                !product.destination) {

                throw new Error("Please fill all product details.");
            }

            if (!Number.isSafeInteger(product.quantity) ||
                product.quantity <= 0) {

                throw new Error("Enter a valid positive whole quantity.");
            }

            message.style.color = "#475569";
            message.style.overflowWrap = "anywhere";
            message.textContent = "Checking product details...";

            const existingProduct = await fetch(
                "/api/products/" +
                encodeURIComponent(product.productId)
            );

            if (existingProduct.ok) {
                throw new Error("Product ID already exists in database.");
            }

            if (existingProduct.status !== 404) {
                throw new Error("Unable to check database.");
            }

            const contract = await getSmartContract();

            const exists = await contract.productExists(
                product.productId
            );

            if (exists) {
                throw new Error("Product already exists on blockchain.");
            }

            message.textContent =
                "Please confirm the transaction in MetaMask...";

            const transaction = await contract.registerProduct(
                product.productId,
                product.productName,
                product.manufacturer,
                BigInt(product.quantity),
                product.origin,
                product.destination
            );

            transactionHash = transaction.hash;

            message.textContent =
                "Waiting for blockchain confirmation...";

            const receipt = await transaction.wait();

            if (!receipt || receipt.status !== 1) {
                throw new Error("Blockchain transaction failed.");
            }

            confirmed = true;
            transactionHash = receipt.hash;

            message.textContent =
                "Blockchain confirmed. Saving to database...";

            const response = await fetch("/api/products", {
                method: "POST",

                headers: {
                    "Content-Type": "application/json",
                    "X-Transaction-Hash": transactionHash
                },

                body: JSON.stringify(product)
            });

            if (!response.ok) {
                throw new Error(
                    "Database registration failed: " + response.status
                );
            }

            message.style.color = "green";

            message.textContent =
                "Product successfully registered on Ethereum and MySQL! " +
                "Transaction Hash: " + transactionHash;

            this.reset();

            loadDashboard();

        } catch (error) {

            message.style.color = "red";

            if (confirmed) {

                message.textContent =
                    "Blockchain registration succeeded, but database " +
                    "synchronization failed. Do not register again. " +
                    "Save this transaction hash: " + transactionHash;

            } else if (transactionHash) {

                message.textContent =
                    "Transaction submitted but confirmation is uncertain. " +
                    "Check Etherscan before retrying. Hash: " +
                    transactionHash;

            } else {

                message.textContent =
                    error.shortMessage || error.message;
            }

            console.error(error);

        } finally {

            if (button) button.disabled = false;
        }
    });
// PRODUCT TRACKING

async function trackProduct() {
    const result = document.getElementById("trackingResult");

    const input = document.getElementById("trackProductId")
        || document.getElementById("searchId")
        || document.getElementById("trackId");

    if (!input) {
        result.textContent = "Tracking input not found.";
        return;
    }

    const productId = input.value.trim();

    if (!productId) {
        result.textContent = "Please enter a Product ID.";
        return;
    }

    try {
        result.textContent = "Tracking product...";

        const response = await fetch(
            "/api/products/" + encodeURIComponent(productId)
        );

        if (!response.ok) {
            result.textContent =
                "Product not found or server error.";
            return;
        }

        const product = await response.json();

        result.style.color = "#1e293b";
        result.style.whiteSpace = "pre-line";
        result.style.lineHeight = "1.8";

        result.textContent =
            "Product ID: " + product.productId + "\n" +
            "Product Name: " + product.productName + "\n" +
            "Manufacturer: " + product.manufacturer + "\n" +
            "Origin: " + product.origin + "\n" +
            "Destination: " + product.destination + "\n" +
            "Current Location: " + product.currentLocation + "\n" +
            "Status: " + product.status;

    } catch (error) {
        result.textContent = "Unable to connect to backend.";
    }
}

// UPDATE PRODUCT STATUS

document.getElementById("updateForm")
    .addEventListener("submit", async function(event) {

        event.preventDefault();

        const productId =
            document.getElementById("updateId").value.trim();

        const location =
            document.getElementById("location").value.trim();

        const status =
            document.getElementById("status").value;

        const message =
            document.getElementById("updateMessage");

        const button = this.querySelector('[type="submit"]');

        if (button) button.disabled = true;

        let transactionHash = null;
        let confirmed = false;

        try {

            if (!productId || !location) {
                throw new Error("Product ID and location are required.");
            }

            if (!["Registered", "In Transit",
                  "At Warehouse", "Delivered"].includes(status)) {

                throw new Error("Invalid product status.");
            }

            message.style.color = "#475569";
            message.style.overflowWrap = "anywhere";
            message.textContent = "Checking product...";

            const productResponse = await fetch(
                "/api/products/" + encodeURIComponent(productId)
            );

            if (!productResponse.ok) {
                throw new Error("Product not found in database or server unavailable.");
            }

            const contract = await getSmartContract();

            const exists = await contract.productExists(productId);

            if (!exists) {
                throw new Error("Product is not registered on blockchain.");
            }

            message.textContent =
                "Please confirm the status update in MetaMask...";

            const transaction = await contract.updateStatus(
                productId,
                location,
                status
            );

            transactionHash = transaction.hash;

            message.textContent =
                "Waiting for blockchain confirmation...";

            const receipt = await transaction.wait();

            if (!receipt || receipt.status !== 1) {
                throw new Error("Blockchain transaction failed.");
            }

            confirmed = true;
            transactionHash = receipt.hash;

            message.textContent =
                "Blockchain confirmed. Updating database...";

            const response = await fetch(
                "/api/products/" +
                encodeURIComponent(productId) +
                "/status",
                {
                    method: "PUT",

                    headers: {
                        "Content-Type": "application/json",
                        "X-Transaction-Hash": transactionHash
                    },

                    body: JSON.stringify({
                        status: status,
                        location: location
                    })
                }
            );

            if (!response.ok) {
                throw new Error(
                    "Database update failed: " + response.status
                );
            }

            message.style.color = "green";

            message.textContent =
                "Product status successfully updated on Ethereum and MySQL! " +
                "Transaction Hash: " + transactionHash;

            this.reset();

            loadDashboard();

        } catch (error) {

            message.style.color = "red";

            if (confirmed) {

                message.textContent =
                    "Blockchain update succeeded, but database " +
                    "synchronization failed. Do not submit again. " +
                    "Save this transaction hash: " + transactionHash;

            } else if (transactionHash) {

                message.textContent =
                    "Transaction submitted but confirmation is uncertain. " +
                    "Check Etherscan before retrying. Hash: " +
                    transactionHash;

            } else {

                message.textContent =
                    error.shortMessage || error.message;
            }

            console.error(error);

        } finally {

            if (button) button.disabled = false;
        }
    });

// TRANSACTION HISTORY

async function loadTransactions() {
    const table = document.getElementById("transactionTable");

    try {
        table.innerHTML =
            '<tr><td colspan="4">Loading transactions...</td></tr>';

        const response = await fetch("/api/transactions");

        if (!response.ok) {
            throw new Error("Failed to load transactions.");
        }

        const transactions = await response.json();

        table.innerHTML = "";

        if (transactions.length === 0) {
            table.innerHTML =
                '<tr><td colspan="4">No transactions yet.</td></tr>';
            return;
        }

        transactions.forEach(transaction => {
            const row = table.insertRow();

            row.insertCell().textContent =
                transaction.productId;

            row.insertCell().textContent =
                transaction.action;

            row.insertCell().textContent =
                new Date(transaction.timestamp).toLocaleString();

            row.insertCell().textContent =
                transaction.transactionHash || "Not recorded";
        });

    } catch (error) {
        table.innerHTML =
            '<tr><td colspan="4">Unable to load transactions.</td></tr>';

        console.error(error);
    }
}


// SUPPLY CHAIN ANALYTICS

async function loadAnalytics() {
    const result = document.getElementById("analyticsResult");

    try {
        result.textContent = "Loading analytics...";

        const response = await fetch("/api/products");

        if (!response.ok) {
            throw new Error("Unable to load analytics.");
        }

        const products = await response.json();

        const total = products.length;

        const registered = products.filter(
            p => p.status === "Registered"
        ).length;

        const inTransit = products.filter(
            p => p.status === "In Transit"
        ).length;

        const warehouse = products.filter(
            p => p.status === "At Warehouse"
        ).length;

        const delivered = products.filter(
            p => p.status === "Delivered"
        ).length;

        const deliveryRate = total > 0
            ? ((delivered / total) * 100).toFixed(1)
            : "0.0";

        const other = total -
            registered - inTransit - warehouse - delivered;

        result.innerHTML = `
            <div style="display:grid;
                        grid-template-columns:repeat(auto-fit,minmax(150px,1fr));
                        gap:15px;
                        margin:20px 0;">

                <div style="background:#eff6ff;
                            padding:20px;
                            border-radius:10px;">
                    <h4>Total Products</h4>
                    <h2>${total}</h2>
                </div>

                <div style="background:#ecfdf5;
                            padding:20px;
                            border-radius:10px;">
                    <h4>Delivered</h4>
                    <h2>${delivered}</h2>
                </div>

                <div style="background:#f5f3ff;
                            padding:20px;
                            border-radius:10px;">
                    <h4>Delivery Rate</h4>
                    <h2>${deliveryRate}%</h2>
                </div>

            </div>

            <h3 style="margin-top:30px;">
                Product Status Distribution
            </h3>

            ${createAnalyticsBar(
                "Registered", registered, total, "#2563eb"
            )}

            ${createAnalyticsBar(
                "In Transit", inTransit, total, "#f59e0b"
            )}

            ${createAnalyticsBar(
                "At Warehouse", warehouse, total, "#8b5cf6"
            )}

            ${createAnalyticsBar(
                "Delivered", delivered, total, "#10b981"
            )}

            ${other > 0 ? createAnalyticsBar(
                "Other", other, total, "#64748b"
            ) : ""}

            <div style="background:#f1f5f9;
                        padding:20px;
                        margin-top:30px;
                        border-radius:10px;">

                <h3>Supply Chain Summary</h3>

                <p>Total registered products: ${total}</p>
                <p>Products awaiting movement: ${registered}</p>
                <p>Products currently in transit: ${inTransit}</p>
                <p>Products at warehouse: ${warehouse}</p>
                <p>Successfully delivered: ${delivered}</p>
                <p>Overall delivery rate: ${deliveryRate}%</p>

            </div>
        `;

    } catch (error) {
        result.textContent = "Unable to load analytics.";
        console.error(error);
    }
}


// ANALYTICS BAR CHART

function createAnalyticsBar(label, count, total, color) {

    const percentage = total > 0
        ? ((count / total) * 100).toFixed(1)
        : "0.0";

    return `
        <div style="margin:20px 0;">

            <div style="display:flex;
                        justify-content:space-between;
                        margin-bottom:8px;">

                <strong>${label}</strong>

                <span>${count} (${percentage}%)</span>

            </div>

            <div style="height:22px;
                        background:#e2e8f0;
                        border-radius:8px;
                        overflow:hidden;">

                <div style="width:${percentage}%;
                            height:100%;
                            background:${color};
                            border-radius:8px;">
                </div>

            </div>

        </div>
    `;
}



// BLOCKCHAIN CONNECTION STATUS

async function loadBlockchainStatus() {

    const badge = document.getElementById("blockchainStatus")
        || [...document.querySelectorAll("span, div")]
            .find(el => el.children.length === 0 &&
                el.textContent.trim() === "Ethereum | Not Connected");

    try {

        const response = await fetch("/api/blockchain/status");

        if (!response.ok) {
            throw new Error("Blockchain connection failed");
        }

        const data = await response.json();

        if (data.connected && String(data.chainId) === "11155111") {

            if (badge) {
                badge.id = "blockchainStatus";
                badge.textContent = "Ethereum | Connected";
                badge.style.backgroundColor = "#dcfce7";
                badge.style.color = "#166534";
            }

        } else {
            throw new Error("Sepolia connection unavailable");
        }

    } catch (error) {

        if (badge) {
            badge.textContent = "Ethereum | Not Connected";
            badge.style.backgroundColor = "#fef3c7";
            badge.style.color = "#92400e";
        }

        console.error(error);
    }
}

// BLOCKCHAIN VERIFICATION

async function verifyTransaction() {

    const result =
        document.getElementById("verificationResult");

    const transactionHash =
        document.getElementById("transactionHash").value.trim();

    result.style.whiteSpace = "pre-line";
    result.style.lineHeight = "1.8";
    result.style.overflowWrap = "anywhere";

    if (!/^0x[0-9a-fA-F]{64}$/.test(transactionHash)) {

        result.style.color = "red";

        result.textContent =
            "Please enter a valid Ethereum transaction hash.";

        return;
    }

    try {

        result.style.color = "#475569";

        result.textContent =
            "Verifying transaction on Ethereum Sepolia...";

        const response = await fetch(
            "/api/blockchain/verify/" +
            encodeURIComponent(transactionHash)
        );

        const data = await response.json();

        if (!response.ok || !data.verified) {

            throw new Error(
                data.message || "Transaction verification failed."
            );
        }

        result.style.color = "#166534";

        result.textContent =
            "TRANSACTION SUCCESSFULLY VERIFIED\n\n" +

            "Network: " + data.network + "\n\n" +

            "Transaction Hash: " +
            data.transactionHash + "\n\n" +

            "Contract Address: " +
            data.contractAddress + "\n\n" +

            "Block Number: " +
            data.blockNumber + "\n\n" +

            "Sender Address: " +
            data.from + "\n\n" +

            "Transaction Status: " +
            data.status + "\n\n";

        const link = document.createElement("a");

        link.href =
            "https://sepolia.etherscan.io/tx/" +
            transactionHash;

        link.target = "_blank";
        link.rel = "noopener noreferrer";

        link.textContent =
            "View Transaction on Etherscan";

        link.style.color = "#2563eb";

        result.appendChild(link);

    } catch (error) {

        result.style.color = "red";

        result.textContent =
            error.message || "Unable to verify transaction.";

        console.error(error);
    }
}


// INITIAL PAGE

showPage("dashboard");

loadBlockchainStatus();

setInterval(loadBlockchainStatus, 30000);
// METAMASK WALLET CONNECTION

async function connectWallet() {

    if (!window.ethereum) {
        alert("Please install MetaMask.");
        return;
    }

    try {

        const accounts = await window.ethereum.request({
            method: "eth_requestAccounts"
        });

        await window.ethereum.request({
            method: "wallet_switchEthereumChain",
            params: [{ chainId: "0xaa36a7" }]
        });

        const account = accounts[0];

        document.getElementById("connectWalletBtn")
            .textContent =
                account.slice(0, 6) + "..." +
                account.slice(-4);

        alert("MetaMask connected to Ethereum Sepolia!");

    } catch (error) {

        alert("Wallet connection failed: " + error.message);

        console.error(error);
    }
}