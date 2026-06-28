package com.skincertchain.controller;

import com.skincertchain.model.CertificateRecord;
import com.skincertchain.service.BlockchainService;
import com.skincertchain.service.CertificateService;
import com.skincertchain.service.CertificateService.VerificationResult;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * REST API for SkincertChain.
 *
 * Base path: /api/v1/certificates
 */
@RestController
@RequestMapping("/api/v1/certificates")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")   // tighten in production
public class CertificateController {

    private final CertificateService  certificateService;
    private final BlockchainService   blockchainService;

    // ── Health / Info ─────────────────────────────────────────────────────────

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
                "status",      "UP",
                "nodeVersion", blockchainService.getNodeVersion()
        ));
    }

    // ── Issue ─────────────────────────────────────────────────────────────────

    /**
     * POST /api/v1/certificates/issue
     *
     * Multipart form fields:
     *   file           – certificate document (PDF / PNG / JPEG)
     *   recipientName  – full name of the holder
     *   recipientAddr  – (optional) Ethereum address of holder
     *   courseName     – credential / course name
     *   issuerAddr     – Ethereum address of the issuing org
     */
    @PostMapping(value = "/issue", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> issue(
            @RequestPart("file")                        MultipartFile file,
            @RequestParam("recipientName")              String recipientName,
            @RequestParam(value = "recipientAddr",
                          defaultValue = "0x0000000000000000000000000000000000000000")
                                                        String recipientAddr,
            @RequestParam("courseName")                 String courseName,
            @RequestParam("issuerAddr")                 String issuerAddr
    ) {
        try {
            CertificateRecord record = certificateService.issueCertificate(
                    file, recipientName, recipientAddr, courseName, issuerAddr
            );
            return ResponseEntity.ok(toDto(record));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ── Verify ────────────────────────────────────────────────────────────────

    /**
     * GET /api/v1/certificates/verify/{certId}
     * Verify by on-chain certificate ID.
     */
    @GetMapping("/verify/{certId}")
    public ResponseEntity<?> verifyById(@PathVariable String certId) {
        VerificationResult result = certificateService.verifyById(certId);
        return ResponseEntity.ok(Map.of(
                "status",      result.status().name(),
                "message",     result.message(),
                "valid",       result.isValid(),
                "certificate", result.certificate() != null ? toDto(result.certificate()) : null
        ));
    }

    /**
     * POST /api/v1/certificates/verify/file
     * Verify by uploading the original document.
     */
    @PostMapping(value = "/verify/file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> verifyByFile(@RequestPart("file") MultipartFile file) {
        try {
            VerificationResult result = certificateService.verifyByFile(file);
            return ResponseEntity.ok(Map.of(
                    "status",      result.status().name(),
                    "message",     result.message(),
                    "valid",       result.isValid(),
                    "certificate", result.certificate() != null ? toDto(result.certificate()) : null
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ── Revoke ────────────────────────────────────────────────────────────────

    /**
     * DELETE /api/v1/certificates/{certId}?issuerAddr=0x...
     */
    @DeleteMapping("/{certId}")
    public ResponseEntity<?> revoke(
            @PathVariable String certId,
            @RequestParam String issuerAddr
    ) {
        try {
            CertificateRecord record = certificateService.revoke(certId, issuerAddr);
            return ResponseEntity.ok(Map.of(
                    "message", "Certificate revoked successfully.",
                    "certId",  certId,
                    "txHash",  record.getTxHash()
            ));
        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ── Queries ───────────────────────────────────────────────────────────────

    @GetMapping
    public List<Map<String, Object>> listAll() {
        return certificateService.findAll().stream().map(this::toDto).toList();
    }

    @GetMapping("/issuer/{address}")
    public List<Map<String, Object>> byIssuer(@PathVariable String address) {
        return certificateService.findByIssuer(address).stream().map(this::toDto).toList();
    }

    @GetMapping("/recipient/{address}")
    public List<Map<String, Object>> byRecipient(@PathVariable String address) {
        return certificateService.findByRecipient(address).stream().map(this::toDto).toList();
    }

    @GetMapping("/search")
    public List<Map<String, Object>> search(@RequestParam String name) {
        return certificateService.search(name).stream().map(this::toDto).toList();
    }

    // ── Mapper ────────────────────────────────────────────────────────────────

    private Map<String, Object> toDto(CertificateRecord r) {
        return Map.of(
                "certId",         r.getCertId(),
                "certHash",       r.getCertHash(),
                "ipfsCid",        r.getIpfsCid() != null ? r.getIpfsCid() : "",
                "ipfsUrl",        r.getIpfsCid() != null
                                  ? "https://ipfs.io/ipfs/" + r.getIpfsCid() : "",
                "issuerAddress",  r.getIssuerAddress(),
                "recipientName",  r.getRecipientName(),
                "courseName",     r.getCourseName(),
                "issuedAt",       r.getIssuedAt().toString(),
                "txHash",         r.getTxHash() != null ? r.getTxHash() : "",
                "revoked",        r.isRevoked()
        );
    }
}
