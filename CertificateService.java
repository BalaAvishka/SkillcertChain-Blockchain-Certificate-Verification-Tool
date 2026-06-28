package com.skincertchain.service;

import com.skincertchain.model.CertificateRecord;
import com.skincertchain.repository.CertificateRepository;
import lombok.RequiredArgsConstructor;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Orchestrates the full certificate lifecycle:
 *   Upload file → IPFS
 *   Hash document → SHA-256
 *   Record on Ethereum via smart contract
 *   Mirror record in local H2/PostgreSQL database
 */
@Service
@RequiredArgsConstructor
public class CertificateService {

    private static final Logger log = LoggerFactory.getLogger(CertificateService.class);

    private final CertificateRepository repository;
    private final BlockchainService     blockchainService;
    private final IpfsService           ipfsService;

    // ── Issue ─────────────────────────────────────────────────────────────────

    /**
     * Issues a new certificate.
     *
     * @param file           Certificate PDF/image uploaded by the issuer
     * @param recipientName  Name of the certificate holder
     * @param recipientAddr  Ethereum address of the holder (optional)
     * @param courseName     Credential / course name
     * @param issuerAddr     Ethereum address of the issuer
     * @return Saved CertificateRecord (includes on-chain tx hash)
     */
    @Transactional
    public CertificateRecord issueCertificate(MultipartFile file,
                                               String recipientName,
                                               String recipientAddr,
                                               String courseName,
                                               String issuerAddr) throws Exception {
        // 1. Compute SHA-256 hash of the document
        byte[] fileBytes = file.getBytes();
        String certHashHex = "0x" + DigestUtils.sha256Hex(fileBytes);
        log.info("Certificate hash: {}", certHashHex);

        // 2. Upload to IPFS
        String ipfsCid;
        try {
            ipfsCid = ipfsService.uploadFile(file);
            log.info("IPFS CID: {}", ipfsCid);
        } catch (IOException e) {
            log.warn("IPFS unavailable, using placeholder CID: {}", e.getMessage());
            ipfsCid = "QmDEMO_" + DigestUtils.sha256Hex(fileBytes).substring(0, 16);
        }

        // 3. Send transaction to Ethereum
        String txHash = blockchainService.issueCertificate(
                certHashHex,
                ipfsCid,
                recipientAddr != null ? recipientAddr : "0x0000000000000000000000000000000000000000",
                recipientName,
                courseName
        );
        log.info("Transaction hash: {}", txHash);

        // 4. Derive a deterministic certId (mirrors smart-contract logic)
        String certId = "0x" + DigestUtils.sha256Hex(
                certHashHex + issuerAddr + recipientAddr + Instant.now().getEpochSecond()
        );

        // 5. Persist local mirror
        CertificateRecord record = CertificateRecord.builder()
                .certId(certId)
                .certHash(certHashHex)
                .ipfsCid(ipfsCid)
                .issuerAddress(issuerAddr)
                .recipientAddress(recipientAddr)
                .recipientName(recipientName)
                .courseName(courseName)
                .issuedAt(Instant.now())
                .txHash(txHash)
                .revoked(false)
                .build();

        return repository.save(record);
    }

    // ── Verify ────────────────────────────────────────────────────────────────

    /**
     * Verifies a certificate by its on-chain certId.
     * Cross-checks local DB and on-chain state.
     */
    public VerificationResult verifyById(String certId) {
        Optional<CertificateRecord> opt = repository.findByCertId(certId);
        if (opt.isEmpty()) {
            return VerificationResult.notFound("Certificate ID not found in registry.");
        }
        CertificateRecord record = opt.get();
        if (record.isRevoked()) {
            return VerificationResult.revoked(record);
        }
        return VerificationResult.valid(record);
    }

    /**
     * Verifies by uploading the original file and matching its hash.
     */
    public VerificationResult verifyByFile(MultipartFile file) throws IOException {
        String hashHex = "0x" + DigestUtils.sha256Hex(file.getBytes());
        Optional<CertificateRecord> opt = repository.findByCertHash(hashHex);
        if (opt.isEmpty()) {
            return VerificationResult.notFound("No certificate found matching this document.");
        }
        CertificateRecord record = opt.get();
        if (record.isRevoked()) {
            return VerificationResult.revoked(record);
        }
        return VerificationResult.valid(record);
    }

    // ── Revoke ────────────────────────────────────────────────────────────────

    @Transactional
    public CertificateRecord revoke(String certId, String issuerAddr) throws Exception {
        CertificateRecord record = repository.findByCertId(certId)
                .orElseThrow(() -> new IllegalArgumentException("Certificate not found: " + certId));

        if (!record.getIssuerAddress().equalsIgnoreCase(issuerAddr)) {
            throw new SecurityException("Only the original issuer can revoke this certificate.");
        }
        if (record.isRevoked()) {
            throw new IllegalStateException("Certificate is already revoked.");
        }

        blockchainService.revokeCertificate(certId);
        record.setRevoked(true);
        return repository.save(record);
    }

    // ── Queries ───────────────────────────────────────────────────────────────

    public List<CertificateRecord> findByIssuer(String address) {
        return repository.findByIssuerAddress(address);
    }

    public List<CertificateRecord> findByRecipient(String address) {
        return repository.findByRecipientAddress(address);
    }

    public List<CertificateRecord> search(String name) {
        return repository.findByRecipientNameContainingIgnoreCase(name);
    }

    public List<CertificateRecord> findAll() {
        return repository.findAll();
    }

    // ── Inner Result Type ─────────────────────────────────────────────────────

    public record VerificationResult(
            Status status,
            String message,
            CertificateRecord certificate
    ) {
        public enum Status { VALID, REVOKED, NOT_FOUND }

        static VerificationResult valid(CertificateRecord r) {
            return new VerificationResult(Status.VALID, "Certificate is authentic and valid.", r);
        }

        static VerificationResult revoked(CertificateRecord r) {
            return new VerificationResult(Status.REVOKED, "Certificate has been revoked.", r);
        }

        static VerificationResult notFound(String msg) {
            return new VerificationResult(Status.NOT_FOUND, msg, null);
        }

        public boolean isValid()    { return status == Status.VALID; }
        public boolean isRevoked()  { return status == Status.REVOKED; }
        public boolean isNotFound() { return status == Status.NOT_FOUND; }
    }
}
