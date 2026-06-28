package com.skincertchain.repository;

import com.skincertchain.model.CertificateRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CertificateRepository extends JpaRepository<CertificateRecord, Long> {

    Optional<CertificateRecord> findByCertId(String certId);

    Optional<CertificateRecord> findByCertHash(String certHash);

    List<CertificateRecord> findByIssuerAddress(String issuerAddress);

    List<CertificateRecord> findByRecipientAddress(String recipientAddress);

    List<CertificateRecord> findByRecipientNameContainingIgnoreCase(String name);

    boolean existsByCertId(String certId);
}
