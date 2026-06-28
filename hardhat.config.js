require("@nomicfoundation/hardhat-toolbox");
require("dotenv").config();

/**
 * Hardhat configuration for CertificateRegistry deployment.
 *
 * Install dev deps once:
 *   cd contracts && npm install
 *
 * Deploy to local Ganache:
 *   npx hardhat run scripts/deploy.js --network localhost
 *
 * Deploy to Sepolia testnet (requires ETH_PRIVATE_KEY and INFURA_KEY env vars):
 *   npx hardhat run scripts/deploy.js --network sepolia
 */
module.exports = {
  solidity: {
    version: "0.8.19",
    settings: { optimizer: { enabled: true, runs: 200 } },
  },
  networks: {
    localhost: {
      url: "http://127.0.0.1:8545",
      chainId: 1337,
    },
    sepolia: {
      url: `https://sepolia.infura.io/v3/${process.env.INFURA_KEY || ""}`,
      accounts: process.env.ETH_PRIVATE_KEY
        ? [`0x${process.env.ETH_PRIVATE_KEY}`]
        : [],
      chainId: 11155111,
    },
  },
  etherscan: {
    apiKey: process.env.ETHERSCAN_API_KEY || "",
  },
};
