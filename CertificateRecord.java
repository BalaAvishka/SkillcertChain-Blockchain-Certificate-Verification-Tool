package com.skincertchain.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Local database mirror of on-chain certificate data.
 * Acts as a cache / search index; truth always lives on-chain.
 */
@Entity
@Table(name = "certificates")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CertificateRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Keccak-256 certId returned by the smart contract */
    @Column(name = "cert_id", unique = true, nullable = false, length = 66)
    private String certId;

    /** SHA-256 hash of the original certificate document */
    @Column(name = "cert_hash", nullable = false, length = 66)
    private String certHash;

    /** IPFS Content Identifier */
    @Column(name = "ipfs_cid", length = 128)
    private String ipfsCid;

    /** Ethereum address of the issuing organisation */
    @Column(name = "issuer_address", nullable = false, length = 42)
    private String issuerAddress;

    /** Ethereum address of the certificate holder */
    @Column(name = "recipient_address", length = 42)
    private String recipientAddress;

    @Column(name = "recipient_name", nullable = false)
    private String recipientName;

    @Column(name = "course_name", nullable = false)
    private String courseName;

    @Column(name = "issued_at", nullable = false)
    private Instant issuedAt;

    @Column(name = "tx_hash", length = 66)
    private String txHash;

    @Column(name = "is_revoked", nullable = false)
    private boolean revoked;
}
