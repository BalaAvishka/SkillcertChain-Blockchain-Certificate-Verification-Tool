package com.skincertchain.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * Uploads certificate files to IPFS via the local Kubo daemon or a
 * remote pinning service (e.g. Infura IPFS, web3.storage).
 *
 * Default: local daemon at http://127.0.0.1:5001
 * Override via application.properties:
 *   ipfs.api.url=https://ipfs.infura.io:5001
 *   ipfs.project.id=<ID>     (Infura only)
 *   ipfs.project.secret=<S>  (Infura only)
 */
@Service
public class IpfsService {

    private static final Logger log = LoggerFactory.getLogger(IpfsService.class);

    @Value("${ipfs.api.url:http://127.0.0.1:5001}")
    private String ipfsApiUrl;

    @Value("${ipfs.project.id:}")
    private String projectId;

    @Value("${ipfs.project.secret:}")
    private String projectSecret;

    private final OkHttpClient http = new OkHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * Uploads a file to IPFS and returns its CID.
     */
    public String uploadFile(MultipartFile file) throws IOException {
        String uploadUrl = ipfsApiUrl + "/api/v0/add?pin=true";

        RequestBody fileBody = RequestBody.create(
                file.getBytes(),
                MediaType.parse(file.getContentType() != null
                        ? file.getContentType() : "application/octet-stream")
        );

        MultipartBody body = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", file.getOriginalFilename(), fileBody)
                .build();

        Request.Builder reqBuilder = new Request.Builder()
                .url(uploadUrl)
                .post(body);

        // Basic auth for Infura IPFS
        if (!projectId.isBlank() && !projectSecret.isBlank()) {
            reqBuilder.header("Authorization",
                    Credentials.basic(projectId, projectSecret));
        }

        try (Response response = http.newCall(reqBuilder.build()).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("IPFS upload failed: " + response.code());
            }
            String responseBody = response.body().string();
            JsonNode json = mapper.readTree(responseBody);
            String cid = json.get("Hash").asText();
            log.info("Uploaded to IPFS: {}", cid);
            return cid;
        }
    }

    /**
     * Uploads raw JSON metadata and returns its CID.
     */
    public String uploadJson(String json) throws IOException {
        String uploadUrl = ipfsApiUrl + "/api/v0/add?pin=true";

        RequestBody fileBody = RequestBody.create(
                json.getBytes(),
                MediaType.parse("application/json")
        );

        MultipartBody body = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", "metadata.json", fileBody)
                .build();

        Request request = new Request.Builder()
                .url(uploadUrl)
                .post(body)
                .build();

        try (Response response = http.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("IPFS JSON upload failed: " + response.code());
            }
            JsonNode node = mapper.readTree(response.body().string());
            return node.get("Hash").asText();
        }
    }

    /** Returns the public gateway URL for a CID. */
    public String gatewayUrl(String cid) {
        return "https://ipfs.io/ipfs/" + cid;
    }
}
