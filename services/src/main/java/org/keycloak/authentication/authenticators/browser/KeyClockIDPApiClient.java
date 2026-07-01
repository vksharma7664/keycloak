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
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * HTTP client for iVALT API integration
 * Handles geofence and time window management operations
 *
 * @author iVALT Integration Team
 */
public class KeyClockIDPApiClient extends AbstractIvaltApiClient {

    private static final Logger logger = Logger.getLogger(KeyClockIDPApiClient.class);

    public KeyClockIDPApiClient(Map<String, String> config) {
        super(
            config,
            IvaltAuthenticatorFactory.IVALT_API_BASE_URL,
            IvaltAuthenticatorFactory.IVALT_API_KEY,
            IvaltAuthenticatorFactory.IVALT_API_TIMEOUT,
            "https://api.ivalt.com"
        );
    }

    // -----------------------------------------------------------------
    // Geofence endpoints (org-level)
    // -----------------------------------------------------------------

    /**
     * Get all geofences for an organization
     *
     * @param orgCode Organization code
     * @param page    Page number (1-based)
     * @param perPage Items per page
     * @param sortBy  Sort column name
     * @param sortOrder Sort order (asc or desc)
     * @return JSON response containing paginated list of geofences
     * @throws IOException If API call fails
     */
    public JsonNode getGeofences(String orgCode, int page, int perPage, String sortBy, String sortOrder) throws IOException, InterruptedException {
        String encodedOrgCode = URLEncoder.encode(orgCode, StandardCharsets.UTF_8);
        String url = baseUrl + "/keyclock/organization/" + encodedOrgCode + "/geo-fences?page=" + page + "&per_page=" + perPage + "&sort_by=" + sortBy + "&sort_order=" + sortOrder;
        logger.debugf("Getting geofences for org %s", orgCode);
        return getRequest(url);
    }

    /**
     * Create a new geofence for an organization
     *
     * @param orgCode Organization code
     * @param jsonPayload JSON payload containing geofence details
     * @return JSON response with created geofence details
     * @throws IOException If API call fails
     */
    public JsonNode createGeofence(String orgCode, String jsonPayload) throws IOException, InterruptedException {
        String encodedOrgCode = URLEncoder.encode(orgCode, StandardCharsets.UTF_8);
        String url = baseUrl + "/keyclock/organization/" + encodedOrgCode + "/create/geo-fence";
        logger.infof("Creating new geofence for org %s", orgCode);
        return postRequest(url, jsonPayload);
    }

    /**
     * Update an existing geofence
     *
     * @param orgCode Organization code
     * @param geofenceId ID of the geofence to update
     * @param jsonPayload JSON payload containing updated geofence details
     * @return JSON response with updated geofence details
     * @throws IOException If API call fails
     */
    public JsonNode updateGeofence(String orgCode, int geofenceId, String jsonPayload) throws IOException, InterruptedException {
        String encodedOrgCode = URLEncoder.encode(orgCode, StandardCharsets.UTF_8);
        String url = baseUrl + "/keyclock/organization/" + encodedOrgCode + "/update/geo-fence/" + geofenceId;
        logger.infof("Updating geofence %d for org %s", geofenceId, orgCode);
        return putRequest(url, jsonPayload);
    }

    /**
     * Delete a geofence
     *
     * @param orgCode Organization code
     * @param geofenceId ID of the geofence to delete
     * @return JSON response confirming deletion
     * @throws IOException If API call fails
     */
    public JsonNode deleteGeofence(String orgCode, int geofenceId) throws IOException, InterruptedException {
        String encodedOrgCode = URLEncoder.encode(orgCode, StandardCharsets.UTF_8);
        String url = baseUrl + "/keyclock/organization/" + encodedOrgCode + "/delete/geo-fence/" + geofenceId;
        logger.infof("Deleting geofence %d for org %s", geofenceId, orgCode);
        return deleteRequest(url, "{}");
    }

    // -----------------------------------------------------------------
    // User geofence assignment endpoints
    // -----------------------------------------------------------------

    /**
     * Get geofences assigned to a user
     *
     * @param userMobile User mobile number
     * @param page       Page number (1-based)
     * @param perPage    Items per page
     * @param sortBy     Sort column name
     * @param sortOrder  Sort order (asc or desc)
     * @return JSON response containing paginated list of assigned geofences
     * @throws IOException If API call fails
     */
    public JsonNode getUserGeofences(String userMobile, int page, int perPage, String sortBy, String sortOrder) throws IOException, InterruptedException {
        String encodedUserMobile = URLEncoder.encode(userMobile, StandardCharsets.UTF_8);
        String url = baseUrl + "/keyclock/user/" + encodedUserMobile + "/geofences?page=" + page + "&per_page=" + perPage + "&sort_by=" + sortBy + "&sort_order=" + sortOrder;
        logger.debugf("Getting geofences for user %s", userMobile);
        return getRequest(url);
    }

    /**
     * Assign geofences to a user
     *
     * @param userMobile User mobile number
     * @param jsonPayload JSON payload containing orgGeoFence_ids array
     * @return JSON response with updated assignment list
     * @throws IOException If API call fails
     */
    public JsonNode updateUserGeofences(String userMobile, String jsonPayload) throws IOException, InterruptedException {
        String encodedUserMobile = URLEncoder.encode(userMobile, StandardCharsets.UTF_8);
        String url = baseUrl + "/keyclock/user/" + encodedUserMobile + "/geofences/update";
        logger.infof("Updating geofence assignments for user %s", userMobile);
        return putRequest(url, jsonPayload);
    }

    // -----------------------------------------------------------------
    // Timeslot endpoints (org-level)
    // -----------------------------------------------------------------

    /**
     * Get all timeslots for an organization
     *
     * @param orgCode Organization code
     * @param page    Page number (1-based)
     * @param perPage Items per page
     * @param sortBy  Sort column name
     * @param sortOrder Sort order (asc or desc)
     * @return JSON response containing paginated list of timeslots
     * @throws IOException If API call fails
     */
    public JsonNode getTimeslots(String orgCode, int page, int perPage, String sortBy, String sortOrder) throws IOException, InterruptedException {
        String encodedOrgCode = URLEncoder.encode(orgCode, StandardCharsets.UTF_8);
        String url = baseUrl + "/keyclock/organization/" + encodedOrgCode + "/timeslots?page=" + page + "&per_page=" + perPage + "&sort_by=" + sortBy + "&sort_order=" + sortOrder;
        logger.debugf("Getting timeslots for org %s", orgCode);
        return getRequest(url);
    }

    /**
     * Create a new timeslot for an organization
     *
     * @param orgCode Organization code
     * @param jsonPayload JSON payload containing timeslot details
     * @return JSON response with created timeslot details
     * @throws IOException If API call fails
     */
    public JsonNode createTimeslot(String orgCode, String jsonPayload) throws IOException, InterruptedException {
        String encodedOrgCode = URLEncoder.encode(orgCode, StandardCharsets.UTF_8);
        String url = baseUrl + "/keyclock/organization/" + encodedOrgCode + "/timeslots/add";
        logger.infof("Creating new timeslot for org %s", orgCode);
        return postRequest(url, jsonPayload);
    }

    /**
     * Update an existing timeslot
     *
     * @param timeslotId ID of the timeslot to update
     * @param jsonPayload JSON payload containing updated timeslot details
     * @return JSON response with updated timeslot details
     * @throws IOException If API call fails
     */
    public JsonNode updateTimeslot(int timeslotId, String jsonPayload) throws IOException, InterruptedException {
        String url = baseUrl + "/keyclock/timeslot/" + timeslotId + "/update";
        logger.infof("Updating timeslot %d", timeslotId);
        return putRequest(url, jsonPayload);
    }

    /**
     * Delete a timeslot
     *
     * @param timeslotId ID of the timeslot to delete
     * @return JSON response confirming deletion
     * @throws IOException If API call fails
     */
    public JsonNode deleteTimeslot(int timeslotId) throws IOException, InterruptedException {
        String url = baseUrl + "/keyclock/timeslot/" + timeslotId + "/delete";
        logger.infof("Deleting timeslot %d", timeslotId);
        return deleteRequest(url, "{}");
    }

    // -----------------------------------------------------------------
    // User timeslot assignment endpoints
    // -----------------------------------------------------------------

    /**
     * Get timeslots assigned to a user
     *
     * @param userMobile User mobile number
     * @param page       Page number (1-based)
     * @param perPage    Items per page
     * @param sortBy     Sort column name
     * @param sortOrder  Sort order (asc or desc)
     * @return JSON response containing paginated list of assigned timeslots
     * @throws IOException If API call fails
     */
    public JsonNode getUserTimeslots(String userMobile, int page, int perPage, String sortBy, String sortOrder) throws IOException, InterruptedException {
        String encodedUserMobile = URLEncoder.encode(userMobile, StandardCharsets.UTF_8);
        String url = baseUrl + "/keyclock/user/" + encodedUserMobile + "/timeslots?page=" + page + "&per_page=" + perPage + "&sort_by=" + sortBy + "&sort_order=" + sortOrder;
        logger.debugf("Getting timeslots for user %s", userMobile);
        return getRequest(url);
    }

    /**
     * Assign timeslots to a user
     *
     * @param userMobile User mobile number
     * @param jsonPayload JSON payload containing timeslot_ids array
     * @return JSON response with updated assignment list
     * @throws IOException If API call fails
     */
    public JsonNode updateUserTimeslots(String userMobile, String jsonPayload) throws IOException, InterruptedException {
        String encodedUserMobile = URLEncoder.encode(userMobile, StandardCharsets.UTF_8);
        String url = baseUrl + "/keyclock/user/" + encodedUserMobile + "/timeslots/update";
        logger.infof("Updating timeslot assignments for user %s", userMobile);
        return putRequest(url, jsonPayload);
    }
}
