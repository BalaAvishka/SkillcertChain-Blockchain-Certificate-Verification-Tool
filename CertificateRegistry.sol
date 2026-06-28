// SPDX-License-Identifier: MIT
pragma solidity ^0.8.19;

/**
 * @title CertificateRegistry
 * @dev Decentralized certificate issuance and verification on Ethereum
 */
contract CertificateRegistry {

    struct Certificate {
        bytes32 certHash;       // SHA-256 hash of the certificate
        string ipfsCid;         // IPFS Content Identifier for the file
        address issuer;         // Ethereum address of the issuer
        address recipient;      // Ethereum address of the recipient
        string recipientName;   // Human-readable name
        string courseName;      // Course/credential name
        uint256 issuedAt;       // Unix timestamp
        bool isRevoked;         // Revocation flag
    }

    // certId => Certificate
    mapping(bytes32 => Certificate) private certificates;

    // Authorized issuers
    mapping(address => bool) public authorizedIssuers;

    // Owner of the registry
    address public owner;

    // ── Events ────────────────────────────────────────────────────────────────
    event CertificateIssued(
        bytes32 indexed certId,
        bytes32 certHash,
        string  ipfsCid,
        address indexed issuer,
        address indexed recipient,
        string  recipientName,
        string  courseName,
        uint256 issuedAt
    );

    event CertificateRevoked(bytes32 indexed certId, address indexed revokedBy);
    event IssuerAuthorized(address indexed issuer);
    event IssuerRevoked(address indexed issuer);

    // ── Modifiers ─────────────────────────────────────────────────────────────
    modifier onlyOwner() {
        require(msg.sender == owner, "Not owner");
        _;
    }

    modifier onlyIssuer() {
        require(authorizedIssuers[msg.sender], "Not an authorized issuer");
        _;
    }

    // ── Constructor ───────────────────────────────────────────────────────────
    constructor() {
        owner = msg.sender;
        authorizedIssuers[msg.sender] = true;
    }

    // ── Issuer Management ─────────────────────────────────────────────────────
    function authorizeIssuer(address issuer) external onlyOwner {
        authorizedIssuers[issuer] = true;
        emit IssuerAuthorized(issuer);
    }

    function revokeIssuer(address issuer) external onlyOwner {
        authorizedIssuers[issuer] = false;
        emit IssuerRevoked(issuer);
    }

    // ── Certificate Operations ────────────────────────────────────────────────

    /**
     * @dev Issue a new certificate.
     * @param certHash   SHA-256 hash of the certificate document
     * @param ipfsCid    IPFS CID where the certificate PDF/JSON is stored
     * @param recipient  Ethereum address of the certificate holder
     * @param recipientName Human-readable name of the recipient
     * @param courseName  Name of the course / credential
     * @return certId Unique on-chain identifier for this certificate
     */
    function issueCertificate(
        bytes32 certHash,
        string  calldata ipfsCid,
        address recipient,
        string  calldata recipientName,
        string  calldata courseName
    ) external onlyIssuer returns (bytes32 certId) {
        certId = keccak256(abi.encodePacked(certHash, msg.sender, recipient, block.timestamp));

        require(certificates[certId].issuedAt == 0, "Certificate already exists");

        certificates[certId] = Certificate({
            certHash:      certHash,
            ipfsCid:       ipfsCid,
            issuer:        msg.sender,
            recipient:     recipient,
            recipientName: recipientName,
            courseName:    courseName,
            issuedAt:      block.timestamp,
            isRevoked:     false
        });

        emit CertificateIssued(
            certId, certHash, ipfsCid,
            msg.sender, recipient,
            recipientName, courseName,
            block.timestamp
        );
    }

    /**
     * @dev Revoke an existing certificate (issuer only).
     */
    function revokeCertificate(bytes32 certId) external onlyIssuer {
        Certificate storage cert = certificates[certId];
        require(cert.issuedAt != 0,          "Certificate does not exist");
        require(cert.issuer == msg.sender,    "Not the original issuer");
        require(!cert.isRevoked,             "Already revoked");

        cert.isRevoked = true;
        emit CertificateRevoked(certId, msg.sender);
    }

    /**
     * @dev Verify a certificate by its on-chain ID.
     * @return All certificate fields plus validity status.
     */
    function verifyCertificate(bytes32 certId)
        external
        view
        returns (
            bytes32 certHash,
            string  memory ipfsCid,
            address issuer,
            address recipient,
            string  memory recipientName,
            string  memory courseName,
            uint256 issuedAt,
            bool    isRevoked,
            bool    isValid
        )
    {
        Certificate storage cert = certificates[certId];
        require(cert.issuedAt != 0, "Certificate not found");

        return (
            cert.certHash,
            cert.ipfsCid,
            cert.issuer,
            cert.recipient,
            cert.recipientName,
            cert.courseName,
            cert.issuedAt,
            cert.isRevoked,
            !cert.isRevoked
        );
    }

    /**
     * @dev Quick existence + validity check by hash (no certId needed).
     */
    function verifyByHash(bytes32 certHash) external view returns (bool exists, bool valid) {
        // Note: in production, maintain a hash→certId mapping for O(1) lookup
        // This stub lets the Java layer do the lookup via events
        exists = false;
        valid  = false;
    }
}
