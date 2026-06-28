#  SkillcertChain

> **Blockchain Certificate Verification System**  
> Tamper-proof, decentralized credential issuance and verification using Ethereum smart contracts + IPFS.

[![Java](https://img.shields.io/badge/Java-17-orange?logo=openjdk)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2-green?logo=spring)](https://spring.io/projects/spring-boot)
[![Solidity](https://img.shields.io/badge/Solidity-0.8.19-blue?logo=solidity)](https://soliditylang.org/)
[![Web3j](https://img.shields.io/badge/Web3j-4.10-purple)](https://web3j.io/)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

---

## 🏗️ Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                     Browser / MetaMask                       │
│            HTML + Vanilla JS (Single Page UI)                │
└───────────────────────┬─────────────────────────────────────┘
                        │ REST (HTTP/JSON)
┌───────────────────────▼─────────────────────────────────────┐
│            Spring Boot 3 Application (Java 17)               │
│  ┌─────────────────┐  ┌────────────────┐  ┌──────────────┐  │
│  │CertificateCtrl  │  │CertificateServ │  │BlockchainSvc │  │
│  │  REST /api/v1   │─▶│ Business Logic │─▶│  Web3j calls │  │
│  └─────────────────┘  └────────────────┘  └──────┬───────┘  │
│                              │                    │           │
│                    ┌─────────▼────────┐           │           │
│                    │  H2 / PostgreSQL │  ┌────────▼────────┐ │
│                    │  (local mirror)  │  │   IpfsService   │ │
│                    └──────────────────┘  └────────┬────────┘ │
└─────────────────────────────────────────────────┼─┘──────────┘
                                                   │
                  ┌────────────────────────────────┼──────────┐
                  │          Ethereum (Sepolia)     │          │
                  │  ┌──────────────────────────┐  │          │
                  │  │  CertificateRegistry.sol │  │          │
                  │  │  - issueCertificate()    │  │          │
                  │  │  - revokeCertificate()   │  │          │
                  │  │  - verifyCertificate()   │  │          │
                  │  └──────────────────────────┘  │          │
                  │                                 ▼          │
                  │                    ┌──────────────────┐   │
                  │                    │   IPFS Network   │   │
                  │                    │  (Kubo / Infura) │   │
                  │                    └──────────────────┘   │
                  └────────────────────────────────────────────┘
```

## 🛠️ Tech Stack

| Layer | Technology |
|-------|-----------|
| Smart Contract | Solidity 0.8.19, Hardhat |
| Backend | **Java 17**, Spring Boot 3.2, Web3j 4.10 |
| Database | H2 (dev) / PostgreSQL (prod) |
| Storage | IPFS (Kubo daemon or Infura IPFS) |
| Frontend | HTML5, Vanilla JS, CSS3 |
| Wallet | MetaMask (browser extension) |
| CI/CD | GitHub Actions |
| Container | Docker, Docker Compose |

---

## 🚀 Quick Start (Docker – recommended)

```bash
git clone https://github.com/yourusername/skincertchain.git
cd skincertchain

# Start Ganache + IPFS + Spring Boot in one command
docker compose up -d

# Open the app
open http://localhost:8080
```

The first time, Ganache starts a local blockchain with 10 pre-funded accounts.
You need to deploy the smart contract (see step 3 below) and set `CONTRACT_ADDRESS`.

---

## 📦 Manual Setup

### Prerequisites

| Tool | Version |
|------|---------|
| Java | 17+ |
| Maven | 3.9+ |
| Node.js | 18+ (for contract deployment) |
| IPFS Kubo | latest (optional for local IPFS) |
| MetaMask | browser extension |

---

### Step 1 – Clone

```bash
git clone https://github.com/yourusername/skincertchain.git
cd skincertchain
```

---

### Step 2 – Configure environment

Create a `.env` file in the project root (never commit this):

```dotenv
# Ethereum node (Ganache local or Infura/Alchemy for testnet)
ETH_NODE_URL=http://localhost:8545
ETH_PRIVATE_KEY=your_64_char_hex_private_key_without_0x

# Filled in after Step 3
CONTRACT_ADDRESS=0x0000000000000000000000000000000000000000

# IPFS (leave empty to use local Kubo at localhost:5001)
IPFS_API_URL=http://127.0.0.1:5001
# For Infura IPFS:
# IPFS_API_URL=https://ipfs.infura.io:5001
# IPFS_PROJECT_ID=your_project_id
# IPFS_PROJECT_SECRET=your_secret
```

Export them (or let Docker Compose pick them up automatically):

```bash
export $(cat .env | xargs)
```

---

### Step 3 – Deploy Smart Contract

```bash
cd contracts
npm install
npx hardhat compile

# Local Ganache:
npx hardhat run scripts/deploy.js --network localhost

# Sepolia testnet:
npx hardhat run scripts/deploy.js --network sepolia
```

Copy the printed contract address into your `.env`:

```dotenv
CONTRACT_ADDRESS=0xYourContractAddressHere
```

---

### Step 4 – Generate Web3j Java Wrappers

```bash
cd ..   # back to project root
mvn web3j:generate-sources
```

This reads `contracts/CertificateRegistry.sol` and generates
`src/main/java/com/skincertchain/contracts/CertificateRegistry.java`.

After generation, uncomment the relevant lines in `BlockchainService.java`
(marked with `// TODO: replace with generated wrapper`).

---

### Step 5 – Run the Spring Boot App

```bash
mvn spring-boot:run
```

Open **http://localhost:8080** in your browser.

---

## 🔌 REST API Reference

Base URL: `http://localhost:8080/api/v1/certificates`

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/health` | Server + node health check |
| `POST` | `/issue` | Issue a new certificate (multipart) |
| `GET` | `/verify/{certId}` | Verify by on-chain certificate ID |
| `POST` | `/verify/file` | Verify by uploading original document |
| `DELETE` | `/{certId}?issuerAddr=0x…` | Revoke a certificate |
| `GET` | `/` | List all certificates |
| `GET` | `/issuer/{address}` | List certs issued by address |
| `GET` | `/recipient/{address}` | List certs held by address |
| `GET` | `/search?name=Alice` | Search by recipient name |

### Example – Issue a Certificate

```bash
curl -X POST http://localhost:8080/api/v1/certificates/issue \
  -F "file=@certificate.pdf" \
  -F "recipientName=Alice Johnson" \
  -F "recipientAddr=0xRecipientAddress" \
  -F "courseName=Certified Ethereum Developer" \
  -F "issuerAddr=0xYourIssuerAddress"
```

### Example – Verify by ID

```bash
curl http://localhost:8080/api/v1/certificates/verify/0xCertId
```

---

## 🧪 Run Tests

```bash
mvn test
```

Test reports: `target/surefire-reports/`

---

## 🔑 GitHub Secrets (for CI/CD)

Add these in **Settings → Secrets and variables → Actions**:

| Secret | Description |
|--------|-------------|
| `ETH_NODE_URL` | Infura/Alchemy endpoint |
| `ETH_PRIVATE_KEY` | Deployer private key |
| `CONTRACT_ADDRESS` | Deployed contract address |
| `DOCKERHUB_USERNAME` | Docker Hub login (optional) |
| `DOCKERHUB_TOKEN` | Docker Hub PAT (optional) |

---

## 📁 Project Structure

```
skincertchain/
├── contracts/
│   ├── CertificateRegistry.sol     ← Solidity smart contract
│   ├── scripts/deploy.js           ← Hardhat deployment script
│   ├── hardhat.config.js
│   └── package.json
├── src/
│   ├── main/java/com/skincertchain/
│   │   ├── SkincertChainApplication.java
│   │   ├── config/Web3jConfig.java
│   │   ├── controller/
│   │   │   ├── CertificateController.java  ← REST API
│   │   │   └── WebController.java
│   │   ├── model/CertificateRecord.java    ← JPA entity
│   │   ├── repository/CertificateRepository.java
│   │   └── service/
│   │       ├── BlockchainService.java      ← Web3j / Ethereum
│   │       ├── CertificateService.java     ← Business logic
│   │       └── IpfsService.java            ← IPFS uploads
│   ├── main/resources/
│   │   ├── application.properties
│   │   └── static/index.html              ← Web UI
│   └── test/java/com/skincertchain/
│       └── CertificateServiceTest.java    ← JUnit 5 tests
├── .github/workflows/ci.yml               ← GitHub Actions
├── Dockerfile
├── docker-compose.yml
├── pom.xml
└── README.md
```

---

## 🔐 Security Notes

- Never commit `.env` or private keys — `.gitignore` excludes them.
- In production, replace H2 with PostgreSQL and add Spring Security.
- The `@CrossOrigin("*")` in the controller should be restricted in production.
- Use environment variables or a secrets manager (AWS Secrets Manager, Vault) for keys.

---

## 📄 License

MIT © 2024 SkincertChain
