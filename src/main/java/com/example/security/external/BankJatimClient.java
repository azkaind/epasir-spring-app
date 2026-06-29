package com.example.security.external;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Client HTTP untuk API Bank Jatim (CircleAPI).
 * Sama persis dengan external/bankjatim/service.go di sistem lama.
 *
 * Endpoint: POST /CircleAPI/rest/circleAPI/cekSaldoVA
 * Auth: Basic Auth (username:password)
 */
@Component
@Slf4j
public class BankJatimClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${bankjatim.url:https://majapahit.bankjatim.co.id/CircleAPI/rest/circleAPI/cekSaldoVA}")
    private String bankJatimUrl;

    @Value("${bankjatim.username:}")
    private String username;

    @Value("${bankjatim.password:}")
    private String password;

    public BankJatimClient(ObjectMapper objectMapper) {
        this.restTemplate = new RestTemplate();
        this.objectMapper = objectMapper;
    }

    /**
     * Cek saldo Virtual Account Bank Jatim.
     *
     * @param virtualAccount nomor VA yang akan dicek
     * @return raw JSON response dari Bank Jatim sebagai String,
     *         atau null jika terjadi error koneksi
     */
    public String cekSaldoVA(String virtualAccount) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // Basic auth — sama seperti yang dipakai di Go (username:password di-encode base64)
            if (!username.isBlank() && !password.isBlank()) {
                String cred = username + ":" + password;
                String encoded = Base64.getEncoder()
                        .encodeToString(cred.getBytes(StandardCharsets.UTF_8));
                headers.set("Authorization", "Basic " + encoded);
            }

            String payload = "{\"VirtualAccount\": \"" + virtualAccount + "\"}";
            HttpEntity<String> entity = new HttpEntity<>(payload, headers);

            ResponseEntity<String> response =
                    restTemplate.exchange(bankJatimUrl, HttpMethod.POST, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                return response.getBody();
            }

            log.warn("Bank Jatim API returned status: {}", response.getStatusCode());
            return null;

        } catch (Exception e) {
            log.error("Error calling Bank Jatim API: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Parse nominal dari response Bank Jatim.
     * Field "Nominal" bisa berupa "-" jika kosong, atau angka sebagai String.
     *
     * @return nominal sebagai double, atau 0 jika "-" atau parse gagal
     */
    public double parseNominal(String rawJson) {
        try {
            var node = objectMapper.readTree(rawJson);
            String nominal = node.path("Nominal").asText("-");
            if ("-".equals(nominal) || nominal.isBlank()) return 0.0;
            return Double.parseDouble(nominal);
        } catch (Exception e) {
            log.error("Error parsing nominal from Bank Jatim response: {}", e.getMessage());
            return 0.0;
        }
    }
}
