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

package org.keycloak.services.resources.admin;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;
import org.keycloak.authentication.authenticators.browser.IvaltAuthenticatorFactory;
import org.keycloak.authentication.authenticators.browser.KeyClockIDPApiClient;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.services.resources.admin.fgap.AdminPermissionEvaluator;

import java.util.HashMap;
import java.util.Map;

/**
 * REST endpoints for iVALT Settings (Geofence and Time Window management)
 * Proxies requests to KeyClockIDP API.
 *
 * <p>Credentials (organization mobile, API key, API base URL) are stored as realm
 * attributes and configured through the iVALT Settings page in the admin console.
 * The organization mobile is resolved server-side so that realm-level pages never
 * need to send a mobile number.</p>
 *
 * @author iVALT Integration Team
 */
public class IvaltSettingsResource {

    private static final Logger logger = Logger.getLogger(IvaltSettingsResource.class);

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** Realm attribute keys for iVALT configuration. */
    public static final String ATTR_ORG_MOBILE = "ivalt.org.mobile";
    public static final String ATTR_ORG_ID = "ivalt.org.id";
    public static final String ATTR_USER_ID = "ivalt.user.id";
    public static final String ATTR_API_KEY = "ivalt.api.key";
    public static final String ATTR_API_BASE_URL = "ivalt.api.base.url";

    private static final String DEFAULT_API_BASE_URL = "https://api.ivalt.com";
    private static final String DEFAULT_API_TIMEOUT = "300000";

    private final KeycloakSession session;
    private final RealmModel realm;
    private final AdminPermissionEvaluator auth;
    private final KeyClockIDPApiClient apiClient;

    public IvaltSettingsResource(KeycloakSession session, RealmModel realm, AdminPermissionEvaluator auth) {
        this.session = session;
        this.realm = realm;
        this.auth = auth;

        // Build the API client from realm-level configuration.
        Map<String, String> config = new HashMap<>();
        config.put(IvaltAuthenticatorFactory.IVALT_API_BASE_URL, getBaseUrl());
        config.put(IvaltAuthenticatorFactory.IVALT_API_KEY, orEmpty(realm.getAttribute(ATTR_API_KEY)));
        config.put(IvaltAuthenticatorFactory.IVALT_API_TIMEOUT, DEFAULT_API_TIMEOUT);

        this.apiClient = new KeyClockIDPApiClient(config);
    }

    // ---------------------------------------------------------------------
    // Configuration endpoints
    // ---------------------------------------------------------------------

    /**
     * Get the iVALT configuration for this realm. The API key is never returned;
     * only a flag indicating whether it has been configured.
     */
    @GET
    @Path("config")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getConfig() {
        auth.realm().requireViewRealm();
        ObjectNode data = MAPPER.createObjectNode();
        data.put("orgMobile", orEmpty(realm.getAttribute(ATTR_ORG_MOBILE)));
        data.put("orgId", orEmpty(realm.getAttribute(ATTR_ORG_ID)));
        data.put("userId", orEmpty(realm.getAttribute(ATTR_USER_ID)));
        data.put("apiBaseUrl", getBaseUrl());
        data.put("apiKeyConfigured", !orEmpty(realm.getAttribute(ATTR_API_KEY)).isEmpty());

        ObjectNode body = MAPPER.createObjectNode();
        body.put("success", true);
        body.set("data", data);
        return Response.ok(body.toString()).build();
    }

    /**
     * Update the iVALT configuration for this realm. The API key is only overwritten
     * when a non-empty value is supplied, so the secret is never wiped accidentally.
     */
    @PUT
    @Path("config")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateConfig(String jsonPayload) {
        auth.realm().requireManageRealm();
        try {
            JsonNode node = MAPPER.readTree(jsonPayload);

            if (node.hasNonNull("orgMobile")) {
                realm.setAttribute(ATTR_ORG_MOBILE, node.get("orgMobile").asText().trim());
            }
            if (node.hasNonNull("orgId")) {
                realm.setAttribute(ATTR_ORG_ID, node.get("orgId").asText().trim());
            }
            if (node.hasNonNull("userId")) {
                realm.setAttribute(ATTR_USER_ID, node.get("userId").asText().trim());
            }
            if (node.hasNonNull("apiBaseUrl")) {
                String baseUrl = node.get("apiBaseUrl").asText().trim();
                realm.setAttribute(ATTR_API_BASE_URL, baseUrl.isEmpty() ? DEFAULT_API_BASE_URL : baseUrl);
            }
            // Only overwrite the API key when a non-empty value is provided.
            if (node.hasNonNull("apiKey")) {
                String apiKey = node.get("apiKey").asText();
                if (!apiKey.trim().isEmpty()) {
                    realm.setAttribute(ATTR_API_KEY, apiKey.trim());
                }
            }
            return getConfig();
        } catch (Exception e) {
            logger.error("Failed to update iVALT configuration", e);
            return badRequest("Failed to update iVALT configuration");
        }
    }

    // ---------------------------------------------------------------------
    // Geofence endpoints
    // ---------------------------------------------------------------------

    /**
     * Get all geofences for the configured organization.
     */
    @GET
    @Path("geofences")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getGeofences() {
        auth.realm().requireViewRealm();
        String orgId = getOrgId();
        if (orgId.isEmpty()) {
            return missingConfig("iVALT organization ID is not configured. Set it on the iVALT Settings page.");
        }
        try {
            var result = apiClient.getGeofences(orgId);
            return Response.ok(result.toString()).build();
        } catch (Exception e) {
            logger.error("Failed to get geofences", e);
            return serverError("Failed to get geofences");
        }
    }

    /**
     * Create a new geofence for the configured organization.
     */
    @POST
    @Path("geofences")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createGeofence(String jsonPayload) {
        auth.realm().requireManageRealm();
        String orgId = getOrgId();
        if (orgId.isEmpty()) {
            return missingConfig("iVALT organization ID is not configured.");
        }
        try {
            var result = apiClient.createGeofence(orgId, jsonPayload);
            return Response.ok(result.toString()).build();
        } catch (Exception e) {
            logger.error("Failed to create geofence", e);
            return serverError("Failed to create geofence");
        }
    }

    /**
     * Update an existing geofence.
     */
    @PUT
    @Path("geofences/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateGeofence(@PathParam("id") int geofenceId, String jsonPayload) {
        auth.realm().requireManageRealm();
        String orgId = getOrgId();
        if (orgId.isEmpty()) {
            return missingConfig("iVALT organization ID is not configured.");
        }
        try {
            var result = apiClient.updateGeofence(orgId, geofenceId, jsonPayload);
            return Response.ok(result.toString()).build();
        } catch (Exception e) {
            logger.errorf(e, "Failed to update geofence: %d", geofenceId);
            return serverError("Failed to update geofence");
        }
    }

    /**
     * Delete a geofence.
     */
    @DELETE
    @Path("geofences/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteGeofence(@PathParam("id") int geofenceId) {
        auth.realm().requireManageRealm();
        String orgId = getOrgId();
        if (orgId.isEmpty()) {
            return missingConfig("iVALT organization ID is not configured.");
        }
        try {
            var result = apiClient.deleteGeofence(orgId, geofenceId);
            return Response.ok(result.toString()).build();
        } catch (Exception e) {
            logger.errorf(e, "Failed to delete geofence: %d", geofenceId);
            return serverError("Failed to delete geofence");
        }
    }

    /**
     * Get geofences assigned to the configured user.
     */
    @GET
    @Path("geofences/assigned")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getUserGeofences() {
        auth.realm().requireViewRealm();
        String userId = getUserId();
        if (userId.isEmpty()) {
            return missingConfig("iVALT user ID is not configured. Set it on the iVALT Settings page.");
        }
        try {
            var result = apiClient.getUserGeofences(userId);
            return Response.ok(result.toString()).build();
        } catch (Exception e) {
            logger.error("Failed to get user geofences", e);
            return serverError("Failed to get user geofences");
        }
    }

    /**
     * Assign geofences to the configured user.
     */
    @PUT
    @Path("geofences/assign")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateUserGeofences(String jsonPayload) {
        auth.realm().requireManageRealm();
        String userId = getUserId();
        if (userId.isEmpty()) {
            return missingConfig("iVALT user ID is not configured.");
        }
        try {
            var result = apiClient.updateUserGeofences(userId, jsonPayload);
            return Response.ok(result.toString()).build();
        } catch (Exception e) {
            logger.error("Failed to update user geofence assignments", e);
            return serverError("Failed to update user geofence assignments");
        }
    }


    // ---------------------------------------------------------------------
    // Time window endpoints
    // ---------------------------------------------------------------------

    /**
     * Get all timeslots for the configured organization.
     */
    @GET
    @Path("timewindows")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getTimeslots() {
        auth.realm().requireViewRealm();
        String orgId = getOrgId();
        if (orgId.isEmpty()) {
            return missingConfig("iVALT organization ID is not configured. Set it on the iVALT Settings page.");
        }
        try {
            var result = apiClient.getTimeslots(orgId);
            return Response.ok(result.toString()).build();
        } catch (Exception e) {
            logger.error("Failed to get timeslots", e);
            return serverError("Failed to get timeslots");
        }
    }

    /**
     * Create a new timeslot for the configured organization.
     */
    @POST
    @Path("timewindows")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createTimeslot(String jsonPayload) {
        auth.realm().requireManageRealm();
        String orgId = getOrgId();
        if (orgId.isEmpty()) {
            return missingConfig("iVALT organization ID is not configured.");
        }
        try {
            var result = apiClient.createTimeslot(orgId, jsonPayload);
            return Response.ok(result.toString()).build();
        } catch (Exception e) {
            logger.error("Failed to create timeslot", e);
            return serverError("Failed to create timeslot");
        }
    }

    /**
     * Update an existing timeslot.
     */
    @PUT
    @Path("timewindows/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateTimeslot(@PathParam("id") int timewindowId, String jsonPayload) {
        auth.realm().requireManageRealm();
        try {
            var result = apiClient.updateTimeslot(timewindowId, jsonPayload);
            return Response.ok(result.toString()).build();
        } catch (Exception e) {
            logger.errorf(e, "Failed to update timeslot: %d", timewindowId);
            return serverError("Failed to update timeslot");
        }
    }

    /**
     * Delete a timeslot.
     */
    @DELETE
    @Path("timewindows/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteTimeslot(@PathParam("id") int timewindowId) {
        auth.realm().requireManageRealm();
        try {
            var result = apiClient.deleteTimeslot(timewindowId);
            return Response.ok(result.toString()).build();
        } catch (Exception e) {
            logger.errorf(e, "Failed to delete timeslot: %d", timewindowId);
            return serverError("Failed to delete timeslot");
        }
    }

    /**
     * Get timeslots assigned to the configured user.
     */
    @GET
    @Path("timewindows/assigned")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getUserTimeslots() {
        auth.realm().requireViewRealm();
        String userId = getUserId();
        if (userId.isEmpty()) {
            return missingConfig("iVALT user ID is not configured. Set it on the iVALT Settings page.");
        }
        try {
            var result = apiClient.getUserTimeslots(userId);
            return Response.ok(result.toString()).build();
        } catch (Exception e) {
            logger.error("Failed to get user timeslots", e);
            return serverError("Failed to get user timeslots");
        }
    }

    /**
     * Assign timeslots to the configured user.
     */
    @PUT
    @Path("timewindows/assign")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateUserTimeslots(String jsonPayload) {
        auth.realm().requireManageRealm();
        String userId = getUserId();
        if (userId.isEmpty()) {
            return missingConfig("iVALT user ID is not configured.");
        }
        try {
            var result = apiClient.updateUserTimeslots(userId, jsonPayload);
            return Response.ok(result.toString()).build();
        } catch (Exception e) {
            logger.error("Failed to update user timeslot assignments", e);
            return serverError("Failed to update user timeslot assignments");
        }
    }


    // ---------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------

    private String getBaseUrl() {
        String configured = realm.getAttribute(ATTR_API_BASE_URL);
        return isBlank(configured) ? DEFAULT_API_BASE_URL : configured.trim();
    }

    private String getOrgId() {
        return orEmpty(realm.getAttribute(ATTR_ORG_ID)).trim();
    }

    private String getUserId() {
        return orEmpty(realm.getAttribute(ATTR_USER_ID)).trim();
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static String orEmpty(String value) {
        return value == null ? "" : value;
    }

    private static Response missingConfig(String message) {
        return Response.status(Response.Status.BAD_REQUEST)
                .type(MediaType.APPLICATION_JSON)
                .entity(String.format("{\"success\":false,\"error\":\"%s\"}", message))
                .build();
    }

    private static Response badRequest(String message) {
        return Response.status(Response.Status.BAD_REQUEST)
                .type(MediaType.APPLICATION_JSON)
                .entity(String.format("{\"success\":false,\"error\":\"%s\"}", message))
                .build();
    }

    private static Response serverError(String message) {
        return Response.serverError()
                .type(MediaType.APPLICATION_JSON)
                .entity(String.format("{\"success\":false,\"error\":\"%s\"}", message))
                .build();
    }
}
