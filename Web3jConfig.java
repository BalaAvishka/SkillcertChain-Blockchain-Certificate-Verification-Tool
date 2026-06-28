package com.skincertchain.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.gas.ContractGasProvider;
import org.web3j.tx.gas.DefaultGasProvider;

/**
 * Configures the Web3j client and Ethereum credentials.
 *
 * Environment variables (set in .env or GitHub Secrets):
 *   ETH_NODE_URL      – e.g. https://sepolia.infura.io/v3/<PROJECT_ID>
 *   ETH_PRIVATE_KEY   – 64-char hex private key (no 0x prefix)
 *   CONTRACT_ADDRESS  – deployed CertificateRegistry address
 */
@Configuration
public class Web3jConfig {

    @Value("${eth.node.url:http://localhost:8545}")
    private String ethNodeUrl;

    @Value("${eth.private.key:}")
    private String privateKey;

    @Bean
    public Web3j web3j() {
        return Web3j.build(new HttpService(ethNodeUrl));
    }

    @Bean
    public Credentials credentials() {
        if (privateKey == null || privateKey.isBlank()) {
            // Demo mode – generate a random throwaway wallet
            try {
                return Credentials.create(
                    org.web3j.crypto.Keys.createEcKeyPair()
                );
            } catch (Exception e) {
                throw new IllegalStateException("Cannot create demo credentials", e);
            }
        }
        return Credentials.create(privateKey);
    }

    @Bean
    public ContractGasProvider gasProvider() {
        return new DefaultGasProvider();
    }
}
