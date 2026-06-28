package com.skincertchain.service;

import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tuples.generated.Tuple9;
import org.web3j.tx.gas.ContractGasProvider;
import org.web3j.abi.datatypes.generated.Bytes32;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * Thin Java wrapper around the CertificateRegistry smart contract.
 *
 * The generated Web3j wrapper class (CertificateRegistry) is created by
 * running: mvn web3j:generate-sources
 *
 * Until you run that command the import below will show red in the IDE;
 * this is expected – the generated source does not yet exist.
 */
@Service
public class BlockchainService {

    private static final Logger log = LoggerFactory.getLogger(BlockchainService.class);

    private final Web3j web3j;
    private final Credentials credentials;
    private final ContractGasProvider gasProvider;

    @Value("${contract.address:0x0000000000000000000000000000000000000000}")
    private String contractAddress;

    public BlockchainService(Web3j web3j,
                             Credentials credentials,
                             ContractGasProvider gasProvider) {
        this.web3j        = web3j;
        this.credentials  = credentials;
        this.gasProvider  = gasProvider;
    }

    // ── Contract Loader ──────────────────────────────────────────────────────

    /**
     * Loads the deployed CertificateRegistry contract wrapper.
     * Replace the stub below with the generated class once you run
     * `mvn web3j:generate-sources`.
     */
    private Object loadContract() {
        // TODO: replace with generated wrapper after `mvn web3j:generate-sources`
        // return CertificateRegistry.load(contractAddress, web3j, credentials, gasProvider);
        log.warn("Contract wrapper not yet generated. Run: mvn web3j:generate-sources");
        return null;
    }

    // ── Hashing ──────────────────────────────────────────────────────────────

    /**
     * Computes SHA-256 of the given bytes and returns it as a 32-byte array
     * suitable for passing as bytes32 to the contract.
     */
    public byte[] sha256AsBytes32(byte[] data) {
        byte[] hash = DigestUtils.sha256(data);
        // DigestUtils already returns 32 bytes for SHA-256
        return hash;
    }

    /** Hex string representation (0x-prefixed) of a SHA-256 hash. */
    public String sha256Hex(byte[] data) {
        return "0x" + DigestUtils.sha256Hex(data);
    }

    // ── Transaction Helpers ──────────────────────────────────────────────────

    /**
     * Issue a certificate on-chain.
     *
     * @param certHashHex  0x-prefixed 64-char hex SHA-256 hash of the document
     * @param ipfsCid      IPFS CID
     * @param recipient    Ethereum address (0x...)
     * @param recipientName Human-readable name
     * @param courseName   Name of credential
     * @return Transaction hash (0x...)
     */
    public String issueCertificate(String certHashHex,
                                   String ipfsCid,
                                   String recipient,
                                   String recipientName,
                                   String courseName) throws Exception {

        // Convert hex hash → bytes32
        byte[] hashBytes = hexToBytes32(certHashHex);

        /*
         * Uncomment after running `mvn web3j:generate-sources`:
         *
         * CertificateRegistry contract = CertificateRegistry.load(
         *     contractAddress, web3j, credentials, gasProvider);
         *
         * TransactionReceipt receipt = contract.issueCertificate(
         *     hashBytes, ipfsCid, recipient, recipientName, courseName).send();
         *
         * return receipt.getTransactionHash();
         */

        // Demo stub – returns a fake tx hash so the UI works without a live node
        log.info("DEMO: issueCertificate called for recipient={} course={}", recipientName, courseName);
        return "0x" + DigestUtils.sha256Hex(
                (certHashHex + recipient + System.currentTimeMillis())
                        .getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Revoke a certificate on-chain.
     *
     * @param certId On-chain certId (0x-prefixed bytes32 hex)
     * @return Transaction hash
     */
    public String revokeCertificate(String certId) throws Exception {
        byte[] certIdBytes = hexToBytes32(certId);

        /*
         * Uncomment after generating wrappers:
         *
         * CertificateRegistry contract = CertificateRegistry.load(
         *     contractAddress, web3j, credentials, gasProvider);
         * TransactionReceipt receipt = contract.revokeCertificate(certIdBytes).send();
         * return receipt.getTransactionHash();
         */

        log.info("DEMO: revokeCertificate called for certId={}", certId);
        return "0x" + DigestUtils.sha256Hex(certId.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Verify a certificate on-chain by its certId.
     * Returns a simple boolean valid status; extend to return the full tuple.
     */
    public boolean verifyCertificate(String certId) throws Exception {
        /*
         * Uncomment after generating wrappers:
         *
         * CertificateRegistry contract = CertificateRegistry.load(
         *     contractAddress, web3j, credentials, gasProvider);
         * var result = contract.verifyCertificate(hexToBytes32(certId)).send();
         * return result.component9(); // isValid
         */

        log.info("DEMO: verifyCertificate called for certId={}", certId);
        return true; // demo always returns valid
    }

    /** Returns the connected Ethereum node's client version string. */
    public String getNodeVersion() {
        try {
            return web3j.web3ClientVersion().send().getWeb3ClientVersion();
        } catch (Exception e) {
            return "unavailable (" + e.getMessage() + ")";
        }
    }

    // ── Utilities ────────────────────────────────────────────────────────────

    private byte[] hexToBytes32(String hex) {
        String clean = hex.startsWith("0x") ? hex.substring(2) : hex;
        // Pad to 64 chars (32 bytes)
        while (clean.length() < 64) clean = "0" + clean;
        byte[] out = new byte[32];
        for (int i = 0; i < 32; i++) {
            out[i] = (byte) Integer.parseInt(clean.substring(i * 2, i * 2 + 2), 16);
        }
        return out;
    }
}
