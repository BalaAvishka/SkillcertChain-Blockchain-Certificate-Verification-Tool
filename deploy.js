const { ethers } = require("hardhat");
const fs = require("fs");
const path = require("path");

/**
 * Deploy CertificateRegistry and write the contract address to
 * ../src/main/resources/contract-address.txt so the Java app can read it.
 */
async function main() {
  console.log("🚀 Deploying CertificateRegistry…");

  const [deployer] = await ethers.getSigners();
  console.log(`📋 Deployer address : ${deployer.address}`);
  console.log(`💰 Balance          : ${ethers.formatEther(
    await ethers.provider.getBalance(deployer.address)
  )} ETH`);

  const Factory  = await ethers.getContractFactory("CertificateRegistry");
  const contract = await Factory.deploy();
  await contract.waitForDeployment();

  const address = await contract.getAddress();
  console.log(`✅ CertificateRegistry deployed to: ${address}`);

  // Write address to a file the Java layer can read
  const outDir = path.join(__dirname, "../../src/main/resources");
  fs.mkdirSync(outDir, { recursive: true });
  fs.writeFileSync(path.join(outDir, "contract-address.txt"), address);
  console.log(`📝 Contract address written to src/main/resources/contract-address.txt`);
  console.log(`\n💡 Set the environment variable:\n   CONTRACT_ADDRESS=${address}`);
}

main().catch(err => {
  console.error(err);
  process.exitCode = 1;
});
