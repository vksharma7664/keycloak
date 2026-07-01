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

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Abstract base class for iVALT API clients
 * Provides common HTTP client functionality following SOLID principles
 * 
 * @author iVALT Integration Team
 */
public abstract class AbstractIvaltApiClient {

    protected static final Logger logger = Logger.getLogger(AbstractIvaltApiClient.class);
    protected static final ObjectMapper objectMapper = new ObjectMapper();

    protected final String baseUrl;
    protected final String apiKey;
    protected final int timeout;
    protected final HttpClient httpClient;

    /**
     * Constructor for abstract API client
     * 
     * @param config Configuration map containing API settings
     * @param baseUrlKey Configuration key for base URL
     * @param apiKeyKey Configuration key for API key
     * @param timeoutKey Configuration key for timeout
     * @param defaultBaseUrl Default base URL if not in config
     */
    protected AbstractIvaltApiClient(
            Map<String, String> config,
            String baseUrlKey,
            String apiKeyKey,
            String timeoutKey,
            String defaultBaseUrl) {
        this.baseUrl = config.getOrDefault(baseUrlKey, defaultBaseUrl);
        this.apiKey = config.get(apiKeyKey);
        this.timeout = Integer.parseInt(config.getOrDefault(timeoutKey, "300000"));

        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(timeout))
                .build();
    }

    /**
     * Generic POST request method
     * 
     * @param url API endpoint URL
     * @param payload JSON payload to send
     * @return JSON response from API
     * @throws IOException If API call fails
     */
    protected JsonNode postRequest(String url, String payload) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .header("x-api-key", apiKey)
                .timeout(Duration.ofMillis(timeout))
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build();

        logger.debugf("iVALT API POST request to %s", url);

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200 || response.statusCode() == 201) {
            logger.debugf("iVALT API POST request successful to %s", url);
            return objectMapper.readTree(response.body());
        } else {
            String errorDetail = extractErrorDetail(response);
            logger.errorf("iVALT API request failed. Status: %d, Response: %s",
                    response.statusCode(), response.body());
            throw new IvaltApiException(response.statusCode(), errorDetail, url);
        }
    }

    /**
     * Generic GET request method
     *
     * @param url API endpoint URL
     * @return JSON response from API
     * @throws IOException If API call fails
     */
    protected JsonNode getRequest(String url) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .header("x-api-key", apiKey)
                .timeout(Duration.ofMillis(timeout))
                .GET()
                .build();

        logger.debugf("iVALT API GET request to %s", url);

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            logger.debugf("iVALT API GET request successful to %s", url);
            return objectMapper.readTree(response.body());
        } else {
            String errorDetail = extractErrorDetail(response);
            logger.errorf("iVALT API request failed. Status: %d, Response: %s",
                    response.statusCode(), response.body());
            throw new IvaltApiException(response.statusCode(), errorDetail, url);
        }
    }

    /**
     * Generic PUT request method
     *
     * @param url API endpoint URL
     * @param payload JSON payload to send
     * @return JSON response from API
     * @throws IOException If API call fails
     */
    protected JsonNode putRequest(String url, String payload) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .header("x-api-key", apiKey)
                .timeout(Duration.ofMillis(timeout))
                .PUT(HttpRequest.BodyPublishers.ofString(payload))
                .build();

        logger.debugf("iVALT API PUT request to %s", url);

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200 || response.statusCode() == 201) {
            logger.debugf("iVALT API PUT request successful to %s", url);
            return objectMapper.readTree(response.body());
        } else {
            String errorDetail = extractErrorDetail(response);
            logger.errorf("iVALT API request failed. Status: %d, Response: %s",
                    response.statusCode(), response.body());
            throw new IvaltApiException(response.statusCode(), errorDetail, url);
        }
    }

    /**
     * Generic DELETE request method
     *
     * @param url API endpoint URL
     * @param payload JSON payload to send
     * @return JSON response from API
     * @throws IOException If API call fails
     */
    protected JsonNode deleteRequest(String url, String payload) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .header("x-api-key", apiKey)
                .timeout(Duration.ofMillis(timeout))
                .method("DELETE", HttpRequest.BodyPublishers.ofString(payload))
                .build();

        logger.debugf("iVALT API DELETE request to %s", url);

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200 || response.statusCode() == 204) {
            logger.debugf("iVALT API DELETE request successful to %s", url);
            return objectMapper.readTree(response.body());
        } else {
            String errorDetail = extractErrorDetail(response);
            logger.errorf("iVALT API request failed. Status: %d, Response: %s",
                    response.statusCode(), response.body());
            throw new IvaltApiException(response.statusCode(), errorDetail, url);
        }
    }

    /**
     * Extract a human-readable error detail from the iVALT API error response.
     * The iVALT API returns errors as: { data: null, error: { detail: "...", title: "..." } }
     *
     * @param response The HTTP response
     * @return Extracted error detail string, or a generic message if parsing fails
     */
    protected String extractErrorDetail(HttpResponse<String> response) {
        try {
            JsonNode root = objectMapper.readTree(response.body());
            JsonNode errorNode = root.path("error");
            if (errorNode.isObject()) {
                String detail = errorNode.path("detail").asText("");
                if (!detail.isEmpty()) {
                    return detail;
                }
                String title = errorNode.path("title").asText("");
                if (!title.isEmpty()) {
                    return title;
                }
            }
        } catch (Exception e) {
            logger.debugf("Failed to parse error response body: %s", e.getMessage());
        }
        return "iVALT API request failed: HTTP " + response.statusCode();
    }

    /**
     * Custom exception that carries the HTTP status code and parsed error detail
     * from the iVALT API, so callers can surface meaningful messages to users.
     */
    public static class IvaltApiException extends IOException {
        private final int statusCode;
        private final String url;

        public IvaltApiException(int statusCode, String message, String url) {
            super(message);
            this.statusCode = statusCode;
            this.url = url;
        }

        public int getStatusCode() {
            return statusCode;
        }

        public String getUrl() {
            return url;
        }
    }
}
