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
import org.jboss.logging.Logger;

import java.io.IOException;
import java.util.Map;

/**
 * HTTP client for iVALT API integration
 * Handles sending notifications and checking status
 * 
 * @author iVALT Integration Team
 */
public class IvaltApiClient extends AbstractIvaltApiClient {

    private static final Logger logger = Logger.getLogger(IvaltApiClient.class);

    public enum NotificationStatus {
        APPROVED,
        REJECTED,
        PENDING,
        INVALID_TIMEZONE,
        INVALID_GEOFENCE,
        ERROR
    }

    public IvaltApiClient(Map<String, String> config) {
        super(
            config,
            IvaltAuthenticatorFactory.IVALT_API_BASE_URL,
            IvaltAuthenticatorFactory.IVALT_API_KEY,
            IvaltAuthenticatorFactory.IVALT_API_TIMEOUT,
            "https://api.ivalt.com"
        );
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
        String url = baseUrl + "/send/global/notification";

        // Build request payload - API expects only mobile number
        String payload = String.format("{\"mobile\":\"%s\"}", mobileNumber);

        logger.infof("Sending iVALT biometric auth request to %s for user %s", mobileNumber, username);

        JsonNode response = postRequest(url, payload);

        logger.infof("iVALT notification sent successfully to %s", mobileNumber);
        // API doesn't return transaction ID, use mobile number as identifier
        return mobileNumber;
    }

    /**
     * Check the biometric authentication status
     * 
     * @param mobileNumber Mobile number used as transaction identifier
     * @return Current status of the authentication
     * @throws IOException If API call fails
     */
    public NotificationStatus getStatus(String mobileNumber) throws IOException, InterruptedException {
        String url = baseUrl + "/validate-geo-fence-auth";

        // Build request payload - API expects mobile number
        String payload = String.format("{\"mobile\":\"%s\"}", mobileNumber);

        logger.debugf("Checking iVALT auth status for mobile %s", mobileNumber);

        try {
            JsonNode response = postRequest(url, payload);

            // Success - authentication approved
            logger.infof("iVALT authentication approved for mobile %s", mobileNumber);
            return NotificationStatus.APPROVED;

        } catch (IOException e) {
            // Parse error response to determine status
            // Since postRequest throws on non-200/201 status, we need to parse the error message
            String errorMessage = e.getMessage();
            
            if (errorMessage != null) {
                String lowerMessage = errorMessage.toLowerCase();

                if (lowerMessage.contains("timezone")) {
                    logger.warnf("iVALT authentication failed: Invalid timezone for mobile %s", mobileNumber);
                    return NotificationStatus.INVALID_TIMEZONE;
                }

                if (lowerMessage.contains("geofencing") || lowerMessage.contains("geofence")) {
                    logger.warnf("iVALT authentication failed: Invalid geofence for mobile %s", mobileNumber);
                    return NotificationStatus.INVALID_GEOFENCE;
                }
            }

            // Check if it's a pending/waiting state or rejection
            // If no specific error, assume it's still pending
            logger.debugf("iVALT authentication pending for mobile %s", mobileNumber);
            return NotificationStatus.PENDING;
        }
    }
}
