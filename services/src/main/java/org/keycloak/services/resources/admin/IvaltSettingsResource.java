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

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;
import org.keycloak.authentication.authenticators.browser.KeyClockIDPApiClient;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.services.ErrorResponse;

import java.io.IOException;
import java.util.Map;

/**
 * REST endpoints for iVALT Settings (Geofence and Time Window management)
 * Proxies requests to KeyClockIDP API
 * 
 * @author iVALT Integration Team
 */
public class IvaltSettingsResource {

    private static final Logger logger = Logger.getLogger(IvaltSettingsResource.class);

    private final KeycloakSession session;
    private final RealmModel realm;
    private final KeyClockIDPApiClient apiClient;

    public IvaltSettingsResource(KeycloakSession session, RealmModel realm) {
        this.session = session;
        this.realm = realm;
        
        // Get API client configuration from realm attributes
        Map<String, String> config = Map.of(
            "IVALT_KEYCLOCKIDP_API_BASE_URL", realm.getAttribute("IVALT_KEYCLOCKIDP_API_BASE_URL", 
                "https://dev.api.ivalt.com/admin/public/api/keyclockidp"),
            "IVALT_KEYCLOCKIDP_API_KEY", realm.getAttribute("IVALT_KEYCLOCKIDP_API_KEY", ""),
            "IVALT_KEYCLOCKIDP_API_TIMEOUT", realm.getAttribute("IVALT_KEYCLOCKIDP_API_TIMEOUT", "300000")
        );
        
        this.apiClient = new KeyClockIDPApiClient(config);
    }

    /**
     * Get active geofences for a mobile number
     */
    @GET
    @Path("geofences")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getActiveGeofences(
            @QueryParam("mobile") String mobile,
            @QueryParam("limit") @DefaultValue("10") int limit,
            @QueryParam("offset") @DefaultValue("0") int offset) {
        try {
            logger.debugf("Getting active geofences for mobile: %s", mobile);
            var result = apiClient.getActiveGeofences(mobile, limit, offset);
            return Response.ok(result.toString()).build();
        } catch (Exception e) {
            logger.error("Failed to get active geofences", e);
            return ErrorResponse.error("Failed to get active geofences", Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Create a new geofence
     */
    @POST
    @Path("geofences")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createGeofence(String jsonPayload) {
        try {
            logger.debug("Creating new geofence");
            var result = apiClient.createGeofence(jsonPayload);
            return Response.ok(result.toString()).build();
        } catch (Exception e) {
            logger.error("Failed to create geofence", e);
            return ErrorResponse.error("Failed to create geofence", Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Update an existing geofence
     */
    @PUT
    @Path("geofences/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateGeofence(@PathParam("id") int geofenceId, String jsonPayload) {
        try {
            logger.debugf("Updating geofence: %d", geofenceId);
            var result = apiClient.updateGeofence(geofenceId, jsonPayload);
            return Response.ok(result.toString()).build();
        } catch (Exception e) {
            logger.errorf("Failed to update geofence: %d", geofenceId, e);
            return ErrorResponse.error("Failed to update geofence", Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Delete a geofence
     */
    @DELETE
    @Path("geofences/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteGeofence(@PathParam("id") int geofenceId, @QueryParam("mobile") String mobile) {
        try {
            logger.debugf("Deleting geofence: %d", geofenceId);
            var result = apiClient.deleteGeofence(geofenceId, mobile);
            return Response.ok(result.toString()).build();
        } catch (Exception e) {
            logger.errorf("Failed to delete geofence: %d", geofenceId, e);
            return ErrorResponse.error("Failed to delete geofence", Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Get assigned geofences for a mobile number
     */
    @GET
    @Path("geofences/assigned")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAssignedGeofences(@QueryParam("mobile") String mobile) {
        try {
            logger.debugf("Getting assigned geofences for mobile: %s", mobile);
            var result = apiClient.getAssignedGeofences(mobile);
            return Response.ok(result.toString()).build();
        } catch (Exception e) {
            logger.error("Failed to get assigned geofences", e);
            return ErrorResponse.error("Failed to get assigned geofences", Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Assign geofence to user
     */
    @POST
    @Path("geofences/assign")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response assignGeofence(String jsonPayload) {
        try {
            logger.debug("Assigning geofence to user");
            var result = apiClient.assignGeofence(jsonPayload);
            return Response.ok(result.toString()).build();
        } catch (Exception e) {
            logger.error("Failed to assign geofence", e);
            return ErrorResponse.error("Failed to assign geofence", Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Remove geofence assignment from user
     */
    @DELETE
    @Path("geofences/assign")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response removeGeofenceAssignment(String jsonPayload) {
        try {
            logger.debug("Removing geofence assignment from user");
            var result = apiClient.removeGeofenceAssignment(jsonPayload);
            return Response.ok(result.toString()).build();
        } catch (Exception e) {
            logger.error("Failed to remove geofence assignment", e);
            return ErrorResponse.error("Failed to remove geofence assignment", Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Get active time windows for a mobile number
     */
    @GET
    @Path("timewindows")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getActiveTimeWindows(
            @QueryParam("mobile") String mobile,
            @QueryParam("limit") @DefaultValue("10") int limit,
            @QueryParam("offset") @DefaultValue("0") int offset) {
        try {
            logger.debugf("Getting active time windows for mobile: %s", mobile);
            var result = apiClient.getActiveTimeWindows(mobile, limit, offset);
            return Response.ok(result.toString()).build();
        } catch (Exception e) {
            logger.error("Failed to get active time windows", e);
            return ErrorResponse.error("Failed to get active time windows", Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Create a new time window
     */
    @POST
    @Path("timewindows")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createTimeWindow(String jsonPayload) {
        try {
            logger.debug("Creating new time window");
            var result = apiClient.createTimeWindow(jsonPayload);
            return Response.ok(result.toString()).build();
        } catch (Exception e) {
            logger.error("Failed to create time window", e);
            return ErrorResponse.error("Failed to create time window", Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Update an existing time window
     */
    @PUT
    @Path("timewindows/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateTimeWindow(@PathParam("id") int timewindowId, String jsonPayload) {
        try {
            logger.debugf("Updating time window: %d", timewindowId);
            var result = apiClient.updateTimeWindow(timewindowId, jsonPayload);
            return Response.ok(result.toString()).build();
        } catch (Exception e) {
            logger.errorf("Failed to update time window: %d", timewindowId, e);
            return ErrorResponse.error("Failed to update time window", Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Delete a time window
     */
    @DELETE
    @Path("timewindows/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteTimeWindow(@PathParam("id") int timewindowId, @QueryParam("mobile") String mobile) {
        try {
            logger.debugf("Deleting time window: %d", timewindowId);
            var result = apiClient.deleteTimeWindow(timewindowId, mobile);
            return Response.ok(result.toString()).build();
        } catch (Exception e) {
            logger.errorf("Failed to delete time window: %d", timewindowId, e);
            return ErrorResponse.error("Failed to delete time window", Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Get assigned time windows for a mobile number
     */
    @GET
    @Path("timewindows/assigned")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAssignedTimeWindows(@QueryParam("mobile") String mobile) {
        try {
            logger.debugf("Getting assigned time windows for mobile: %s", mobile);
            var result = apiClient.getAssignedTimeWindows(mobile);
            return Response.ok(result.toString()).build();
        } catch (Exception e) {
            logger.error("Failed to get assigned time windows", e);
            return ErrorResponse.error("Failed to get assigned time windows", Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Assign time window to user
     */
    @POST
    @Path("timewindows/assign")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response assignTimeWindow(String jsonPayload) {
        try {
            logger.debug("Assigning time window to user");
            var result = apiClient.assignTimeWindow(jsonPayload);
            return Response.ok(result.toString()).build();
        } catch (Exception e) {
            logger.error("Failed to assign time window", e);
            return ErrorResponse.error("Failed to assign time window", Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Remove time window assignment from user
     */
    @DELETE
    @Path("timewindows/assign")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response removeTimeWindowAssignment(String jsonPayload) {
        try {
            logger.debug("Removing time window assignment from user");
            var result = apiClient.removeTimeWindowAssignment(jsonPayload);
            return Response.ok(result.toString()).build();
        } catch (Exception e) {
            logger.error("Failed to remove time window assignment", e);
            return ErrorResponse.error("Failed to remove time window assignment", Response.Status.INTERNAL_SERVER_ERROR);
        }
    }
}
