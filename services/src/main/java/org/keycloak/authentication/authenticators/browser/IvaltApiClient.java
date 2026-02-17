/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.authentication.authenticators.browser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

/**
 * HTTP client for iVALT API integration
 * Handles sending notifications and checking status
 * 
 * @author iVALT Integration Team
 */
public class IvaltApiClient {

    private static final Logger logger = Logger.getLogger(IvaltApiClient.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final String baseUrl;
    private final String apiKey;
    private final int timeout;
    private final HttpClient httpClient;

    public enum NotificationStatus {
        APPROVED,
        REJECTED,
        PENDING,
        INVALID_TIMEZONE,
        INVALID_GEOFENCE,
        ERROR
    }

    public IvaltApiClient(Map<String, String> config) {
        this.baseUrl = config.getOrDefault(IvaltAuthenticatorFactory.IVALT_API_BASE_URL, "https://api.ivalt.com");
        this.apiKey = config.get(IvaltAuthenticatorFactory.IVALT_API_KEY);
        this.timeout = Integer.parseInt(config.getOrDefault(IvaltAuthenticatorFactory.IVALT_API_TIMEOUT, "300000"));

        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(timeout))
                .build();
    }

    /**
     * Send biometric authentication request to user's mobile device
     * 
     * @param mobileNumber Full mobile number with country code (e.g.,
     *                     +919876543210)
     * @param username     Username for logging/tracking (not sent to API)
     * @param realm        Realm name (not sent to API)
     * @return Mobile number as transaction identifier (API doesn't return
     *         transaction ID)
     * @throws IOException If API call fails
     */
    public String sendNotification(String mobileNumber, String username, String realm)
            throws IOException, InterruptedException {
        String url = baseUrl + "/biometric-auth-request";

        // Build request payload - API expects only mobile number
        String payload = String.format("{\"mobile\":\"%s\"}", mobileNumber);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .header("x-api-key", apiKey)
                .timeout(Duration.ofMillis(timeout))
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build();

        logger.infof("Sending iVALT biometric auth request to %s for user %s", mobileNumber, username);

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            logger.infof("iVALT notification sent successfully to %s", mobileNumber);
            // API doesn't return transaction ID, use mobile number as identifier
            return mobileNumber;
        } else {
            logger.errorf("Failed to send iVALT notification. Status: %d, Response: %s",
                    response.statusCode(), response.body());
            throw new IOException("Failed to send notification: HTTP " + response.statusCode());
        }
    }

    /**
     * Check the biometric authentication status
     * 
     * @param mobileNumber Mobile number used as transaction identifier
     * @return Current status of the authentication
     * @throws IOException If API call fails
     */
    public NotificationStatus getStatus(String mobileNumber) throws IOException, InterruptedException {
        String url = baseUrl + "/biometric-geo-fence-auth-results";

        // Build request payload - API expects mobile number
        String payload = String.format("{\"mobile\":\"%s\"}", mobileNumber);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .header("x-api-key", apiKey)
                .timeout(Duration.ofMillis(timeout))
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build();

        logger.debugf("Checking iVALT auth status for mobile %s", mobileNumber);

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            // Success - authentication approved
            logger.infof("iVALT authentication approved for mobile %s", mobileNumber);
            return NotificationStatus.APPROVED;
        } else {
            // Parse error response to determine status
            try {
                JsonNode jsonResponse = objectMapper.readTree(response.body());
                JsonNode errorNode = jsonResponse.get("error");

                if (errorNode != null) {
                    JsonNode detailNode = errorNode.get("detail");
                    if (detailNode != null) {
                        String detail = detailNode.asText().toLowerCase();

                        if (detail.contains("timezone")) {
                            logger.warnf("iVALT authentication failed: Invalid timezone for mobile %s", mobileNumber);
                            return NotificationStatus.INVALID_TIMEZONE;
                        }

                        if (detail.contains("geofencing") || detail.contains("geofence")) {
                            logger.warnf("iVALT authentication failed: Invalid geofence for mobile %s", mobileNumber);
                            return NotificationStatus.INVALID_GEOFENCE;
                        }
                    }
                }

                // Check if it's a pending/waiting state or rejection
                // If no specific error, assume it's still pending
                logger.debugf("iVALT authentication pending for mobile %s. Status: %d", mobileNumber,
                        response.statusCode());
                return NotificationStatus.PENDING;

            } catch (Exception e) {
                logger.errorf(e, "Failed to parse iVALT error response for mobile %s", mobileNumber);
                return NotificationStatus.ERROR;
            }
        }
    }
}
