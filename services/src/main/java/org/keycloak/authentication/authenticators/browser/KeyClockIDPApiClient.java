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
 * HTTP client for KeyClockIDP API integration
 * Handles geofence and time window management operations
 * 
 * @author iVALT Integration Team
 */
public class KeyClockIDPApiClient {

    private static final Logger logger = Logger.getLogger(KeyClockIDPApiClient.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final String baseUrl;
    private final String apiKey;
    private final int timeout;
    private final HttpClient httpClient;

    public KeyClockIDPApiClient(Map<String, String> config) {
        this.baseUrl = config.getOrDefault(IvaltAuthenticatorFactory.IVALT_KEYCLOCKIDP_API_BASE_URL, 
                "https://dev.api.ivalt.com/admin/public/api/keyclockidp");
        this.apiKey = config.get(IvaltAuthenticatorFactory.IVALT_KEYCLOCKIDP_API_KEY);
        this.timeout = Integer.parseInt(config.getOrDefault(IvaltAuthenticatorFactory.IVALT_KEYCLOCKIDP_API_TIMEOUT, "300000"));

        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(timeout))
                .build();
    }

    /**
     * Get active geofences for a mobile number
     */
    public JsonNode getActiveGeofences(String mobile, int limit, int offset) throws IOException, InterruptedException {
        String url = baseUrl + "/geofence/active-list";
        String payload = String.format("{\"mobile\":\"%s\",\"limit\":%d,\"offset\":%d}", mobile, limit, offset);
        return postRequest(url, payload);
    }

    /**
     * Create a new geofence
     */
    public JsonNode createGeofence(String jsonPayload) throws IOException, InterruptedException {
        String url = baseUrl + "/geofence/create";
        return postRequest(url, jsonPayload);
    }

    /**
     * Update an existing geofence
     */
    public JsonNode updateGeofence(int geofenceId, String jsonPayload) throws IOException, InterruptedException {
        String url = baseUrl + "/geofence/update/" + geofenceId;
        return postRequest(url, jsonPayload);
    }

    /**
     * Delete a geofence
     */
    public JsonNode deleteGeofence(int geofenceId, String mobile) throws IOException, InterruptedException {
        String url = baseUrl + "/geofence/delete/" + geofenceId;
        String payload = String.format("{\"mobile\":\"%s\"}", mobile);
        return deleteRequest(url, payload);
    }

    /**
     * Get assigned geofences for a mobile number
     */
    public JsonNode getAssignedGeofences(String mobile) throws IOException, InterruptedException {
        String url = baseUrl + "/geofence/assigned-list";
        String payload = String.format("{\"mobile\":\"%s\"}", mobile);
        return postRequest(url, payload);
    }

    /**
     * Assign geofence to user
     */
    public JsonNode assignGeofence(String jsonPayload) throws IOException, InterruptedException {
        String url = baseUrl + "/geofence/assign";
        return postRequest(url, jsonPayload);
    }

    /**
     * Remove geofence assignment from user
     */
    public JsonNode removeGeofenceAssignment(String jsonPayload) throws IOException, InterruptedException {
        String url = baseUrl + "/geofence/assigned-delete";
        return deleteRequest(url, jsonPayload);
    }

    /**
     * Get active time windows for a mobile number
     */
    public JsonNode getActiveTimeWindows(String mobile, int limit, int offset) throws IOException, InterruptedException {
        String url = baseUrl + "/timewindow/active-list";
        String payload = String.format("{\"mobile\":\"%s\",\"limit\":%d,\"offset\":%d}", mobile, limit, offset);
        return postRequest(url, payload);
    }

    /**
     * Create a new time window
     */
    public JsonNode createTimeWindow(String jsonPayload) throws IOException, InterruptedException {
        String url = baseUrl + "/timewindow/create";
        return postRequest(url, jsonPayload);
    }

    /**
     * Update an existing time window
     */
    public JsonNode updateTimeWindow(int timewindowId, String jsonPayload) throws IOException, InterruptedException {
        String url = baseUrl + "/timewindow/update/" + timewindowId;
        return postRequest(url, jsonPayload);
    }

    /**
     * Delete a time window
     */
    public JsonNode deleteTimeWindow(int timewindowId, String mobile) throws IOException, InterruptedException {
        String url = baseUrl + "/timewindow/delete/" + timewindowId;
        String payload = String.format("{\"mobile\":\"%s\"}", mobile);
        return deleteRequest(url, payload);
    }

    /**
     * Get assigned time windows for a mobile number
     */
    public JsonNode getAssignedTimeWindows(String mobile) throws IOException, InterruptedException {
        String url = baseUrl + "/timewindow/assigned-list";
        String payload = String.format("{\"mobile\":\"%s\"}", mobile);
        return postRequest(url, payload);
    }

    /**
     * Assign time window to user
     */
    public JsonNode assignTimeWindow(String jsonPayload) throws IOException, InterruptedException {
        String url = baseUrl + "/timewindow/assign";
        return postRequest(url, jsonPayload);
    }

    /**
     * Remove time window assignment from user
     */
    public JsonNode removeTimeWindowAssignment(String jsonPayload) throws IOException, InterruptedException {
        String url = baseUrl + "/timewindow/assigned-delete";
        return deleteRequest(url, jsonPayload);
    }

    /**
     * Generic POST request method
     */
    private JsonNode postRequest(String url, String payload) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .header("x-api-key", apiKey)
                .timeout(Duration.ofMillis(timeout))
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build();

        logger.debugf("KeyClockIDP API POST request to %s", url);

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200 || response.statusCode() == 201) {
            return objectMapper.readTree(response.body());
        } else {
            logger.errorf("KeyClockIDP API request failed. Status: %d, Response: %s",
                    response.statusCode(), response.body());
            throw new IOException("KeyClockIDP API request failed: HTTP " + response.statusCode());
        }
    }

    /**
     * Generic DELETE request method
     */
    private JsonNode deleteRequest(String url, String payload) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .header("x-api-key", apiKey)
                .timeout(Duration.ofMillis(timeout))
                .DELETE(HttpRequest.BodyPublishers.ofString(payload))
                .build();

        logger.debugf("KeyClockIDP API DELETE request to %s", url);

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200 || response.statusCode() == 204) {
            return objectMapper.readTree(response.body());
        } else {
            logger.errorf("KeyClockIDP API request failed. Status: %d, Response: %s",
                    response.statusCode(), response.body());
            throw new IOException("KeyClockIDP API request failed: HTTP " + response.statusCode());
        }
    }
}
