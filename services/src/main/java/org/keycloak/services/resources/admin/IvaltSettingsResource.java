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
     * Get active geofences for a mobile number (defaults to the organization mobile).
     */
    @GET
    @Path("geofences")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getActiveGeofences(
            @QueryParam("mobile") String mobile,
            @QueryParam("limit") @DefaultValue("10") int limit,
            @QueryParam("offset") @DefaultValue("0") int offset) {
        auth.realm().requireViewRealm();
        String resolved = resolveMobile(mobile);
        if (resolved.isEmpty()) {
            return missingMobile();
        }
        try {
            var result = apiClient.getActiveGeofences(resolved, limit, offset);
            return Response.ok(result.toString()).build();
        } catch (Exception e) {
            logger.error("Failed to get active geofences", e);
            return serverError("Failed to get active geofences");
        }
    }

    /**
     * Create a new geofence.
     */
    @POST
    @Path("geofences")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createGeofence(String jsonPayload) {
        auth.realm().requireManageRealm();
        try {
            var result = apiClient.createGeofence(withOrgMobile(jsonPayload));
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
        try {
            var result = apiClient.updateGeofence(geofenceId, withOrgMobile(jsonPayload));
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
    public Response deleteGeofence(@PathParam("id") int geofenceId, @QueryParam("mobile") String mobile) {
        auth.realm().requireManageRealm();
        String resolved = resolveMobile(mobile);
        if (resolved.isEmpty()) {
            return missingMobile();
        }
        try {
            var result = apiClient.deleteGeofence(geofenceId, resolved);
            return Response.ok(result.toString()).build();
        } catch (Exception e) {
            logger.errorf(e, "Failed to delete geofence: %d", geofenceId);
            return serverError("Failed to delete geofence");
        }
    }

    /**
     * Get assigned geofences for a user's mobile number.
     */
    @GET
    @Path("geofences/assigned")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAssignedGeofences(@QueryParam("mobile") String mobile) {
        auth.realm().requireViewRealm();
        if (isBlank(mobile)) {
            return missingMobile();
        }
        try {
            var result = apiClient.getAssignedGeofences(mobile);
            return Response.ok(result.toString()).build();
        } catch (Exception e) {
            logger.error("Failed to get assigned geofences", e);
            return serverError("Failed to get assigned geofences");
        }
    }

    /**
     * Assign geofence to user.
     */
    @POST
    @Path("geofences/assign")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response assignGeofence(String jsonPayload) {
        auth.realm().requireManageRealm();
        try {
            var result = apiClient.assignGeofence(withOrgMobile(jsonPayload));
            return Response.ok(result.toString()).build();
        } catch (Exception e) {
            logger.error("Failed to assign geofence", e);
            return serverError("Failed to assign geofence");
        }
    }

    /**
     * Remove geofence assignment from user.
     */
    @DELETE
    @Path("geofences/assign")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response removeGeofenceAssignment(String jsonPayload) {
        auth.realm().requireManageRealm();
        try {
            var result = apiClient.removeGeofenceAssignment(withOrgMobile(jsonPayload));
            return Response.ok(result.toString()).build();
        } catch (Exception e) {
            logger.error("Failed to remove geofence assignment", e);
            return serverError("Failed to remove geofence assignment");
        }
    }

    // ---------------------------------------------------------------------
    // Time window endpoints
    // ---------------------------------------------------------------------

    /**
     * Get active time windows for a mobile number (defaults to the organization mobile).
     */
    @GET
    @Path("timewindows")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getActiveTimeWindows(
            @QueryParam("mobile") String mobile,
            @QueryParam("limit") @DefaultValue("10") int limit,
            @QueryParam("offset") @DefaultValue("0") int offset) {
        auth.realm().requireViewRealm();
        String resolved = resolveMobile(mobile);
        if (resolved.isEmpty()) {
            return missingMobile();
        }
        try {
            var result = apiClient.getActiveTimeWindows(resolved, limit, offset);
            return Response.ok(result.toString()).build();
        } catch (Exception e) {
            logger.error("Failed to get active time windows", e);
            return serverError("Failed to get active time windows");
        }
    }

    /**
     * Create a new time window.
     */
    @POST
    @Path("timewindows")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createTimeWindow(String jsonPayload) {
        auth.realm().requireManageRealm();
        try {
            var result = apiClient.createTimeWindow(withOrgMobile(jsonPayload));
            return Response.ok(result.toString()).build();
        } catch (Exception e) {
            logger.error("Failed to create time window", e);
            return serverError("Failed to create time window");
        }
    }

    /**
     * Update an existing time window.
     */
    @PUT
    @Path("timewindows/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateTimeWindow(@PathParam("id") int timewindowId, String jsonPayload) {
        auth.realm().requireManageRealm();
        try {
            var result = apiClient.updateTimeWindow(timewindowId, withOrgMobile(jsonPayload));
            return Response.ok(result.toString()).build();
        } catch (Exception e) {
            logger.errorf(e, "Failed to update time window: %d", timewindowId);
            return serverError("Failed to update time window");
        }
    }

    /**
     * Delete a time window.
     */
    @DELETE
    @Path("timewindows/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteTimeWindow(@PathParam("id") int timewindowId, @QueryParam("mobile") String mobile) {
        auth.realm().requireManageRealm();
        String resolved = resolveMobile(mobile);
        if (resolved.isEmpty()) {
            return missingMobile();
        }
        try {
            var result = apiClient.deleteTimeWindow(timewindowId, resolved);
            return Response.ok(result.toString()).build();
        } catch (Exception e) {
            logger.errorf(e, "Failed to delete time window: %d", timewindowId);
            return serverError("Failed to delete time window");
        }
    }

    /**
     * Get assigned time windows for a user's mobile number.
     */
    @GET
    @Path("timewindows/assigned")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAssignedTimeWindows(@QueryParam("mobile") String mobile) {
        auth.realm().requireViewRealm();
        if (isBlank(mobile)) {
            return missingMobile();
        }
        try {
            var result = apiClient.getAssignedTimeWindows(mobile);
            return Response.ok(result.toString()).build();
        } catch (Exception e) {
            logger.error("Failed to get assigned time windows", e);
            return serverError("Failed to get assigned time windows");
        }
    }

    /**
     * Assign time window to user.
     */
    @POST
    @Path("timewindows/assign")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response assignTimeWindow(String jsonPayload) {
        auth.realm().requireManageRealm();
        try {
            var result = apiClient.assignTimeWindow(withOrgMobile(jsonPayload));
            return Response.ok(result.toString()).build();
        } catch (Exception e) {
            logger.error("Failed to assign time window", e);
            return serverError("Failed to assign time window");
        }
    }

    /**
     * Remove time window assignment from user.
     */
    @DELETE
    @Path("timewindows/assign")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response removeTimeWindowAssignment(String jsonPayload) {
        auth.realm().requireManageRealm();
        try {
            var result = apiClient.removeTimeWindowAssignment(withOrgMobile(jsonPayload));
            return Response.ok(result.toString()).build();
        } catch (Exception e) {
            logger.error("Failed to remove time window assignment", e);
            return serverError("Failed to remove time window assignment");
        }
    }

    // ---------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------

    private String getBaseUrl() {
        String configured = realm.getAttribute(ATTR_API_BASE_URL);
        return isBlank(configured) ? DEFAULT_API_BASE_URL : configured.trim();
    }

    private String getOrgMobile() {
        return orEmpty(realm.getAttribute(ATTR_ORG_MOBILE)).trim();
    }

    /** Use the supplied mobile when present, otherwise fall back to the org mobile. */
    private String resolveMobile(String mobile) {
        return isBlank(mobile) ? getOrgMobile() : mobile.trim();
    }

    /** Inject the organization mobile into a JSON payload when it is missing or blank. */
    private String withOrgMobile(String jsonPayload) {
        try {
            JsonNode node = MAPPER.readTree(jsonPayload);
            if (!node.isObject()) {
                return jsonPayload;
            }
            ObjectNode obj = (ObjectNode) node;
            if (!obj.hasNonNull("mobile") || obj.get("mobile").asText().trim().isEmpty()) {
                obj.put("mobile", getOrgMobile());
            }
            return obj.toString();
        } catch (Exception e) {
            logger.warn("Could not parse iVALT payload to inject org mobile; passing through", e);
            return jsonPayload;
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static String orEmpty(String value) {
        return value == null ? "" : value;
    }

    private static Response missingMobile() {
        return Response.status(Response.Status.BAD_REQUEST)
                .type(MediaType.APPLICATION_JSON)
                .entity("{\"success\":false,\"error\":\"iVALT organization mobile is not configured. Set it on the iVALT Settings page.\"}")
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
