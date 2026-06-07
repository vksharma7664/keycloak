import type KeycloakAdminClient from "@keycloak/keycloak-admin-client";
import { useMemo } from "react";
import { useAdminClient } from "../../admin-client";
import { getAuthorizationHeaders } from "../../utils/getAuthorizationHeaders";
import { joinPath } from "../../utils/joinPath";
import {
  GeofenceCreateRequest,
  GeofenceUpdateRequest,
  GeofenceListResponse,
  TimeWindowCreateRequest,
  TimeWindowUpdateRequest,
  TimeWindowListResponse,
  AssignRequest,
  ApiResponse,
  IvaltConfig,
  IvaltConfigUpdateRequest,
} from "./types";

/**
 * Client for the iVALT Settings admin REST endpoints. All requests are
 * authenticated with the admin console bearer token and scoped to the realm
 * managed by the supplied admin client.
 */
export class KeyClockIDPClient {
  #adminClient: KeycloakAdminClient;

  constructor(adminClient: KeycloakAdminClient) {
    this.#adminClient = adminClient;
  }

  async #request<T>(
    endpoint: string,
    options: RequestInit = {},
    query?: Record<string, string>,
  ): Promise<ApiResponse<T>> {
    try {
      const accessToken = await this.#adminClient.getAccessToken();
      const url =
        joinPath(
          this.#adminClient.baseUrl,
          "admin/realms",
          encodeURIComponent(this.#adminClient.realmName),
          "ivalt-settings",
          endpoint,
        ) + (query ? "?" + new URLSearchParams(query) : "");

      const response = await fetch(url, {
        ...options,
        headers: {
          "Content-Type": "application/json",
          ...getAuthorizationHeaders(accessToken),
        },
      });

      const data = await response.json().catch(() => ({}));

      if (!response.ok) {
        return { success: false, error: data.error || "Request failed" };
      }

      return data;
    } catch (error) {
      return {
        success: false,
        error: error instanceof Error ? error.message : "Unknown error",
      };
    }
  }

  // Configuration
  async getConfig(): Promise<ApiResponse<IvaltConfig>> {
    return this.#request<IvaltConfig>("config");
  }

  async updateConfig(
    request: IvaltConfigUpdateRequest,
  ): Promise<ApiResponse<IvaltConfig>> {
    return this.#request<IvaltConfig>("config", {
      method: "PUT",
      body: JSON.stringify(request),
    });
  }

  // Geofence endpoints. Realm-level calls omit the mobile so the backend uses
  // the configured organization mobile.
  async getActiveGeofences(
    limit = 10,
    offset = 0,
  ): Promise<ApiResponse<GeofenceListResponse>> {
    return this.#request<GeofenceListResponse>("geofences", undefined, {
      limit: limit.toString(),
      offset: offset.toString(),
    });
  }

  async createGeofence(
    request: GeofenceCreateRequest,
  ): Promise<ApiResponse<any>> {
    return this.#request("geofences", {
      method: "POST",
      body: JSON.stringify(request),
    });
  }

  async updateGeofence(
    geofenceId: number,
    request: GeofenceUpdateRequest,
  ): Promise<ApiResponse<any>> {
    return this.#request(`geofences/${geofenceId}`, {
      method: "PUT",
      body: JSON.stringify(request),
    });
  }

  async deleteGeofence(geofenceId: number): Promise<ApiResponse<any>> {
    return this.#request(`geofences/${geofenceId}`, {
      method: "DELETE",
    });
  }

  async getAssignedGeofences(mobile: string): Promise<ApiResponse<any>> {
    return this.#request("geofences/assigned", undefined, { mobile });
  }

  async assignGeofence(request: AssignRequest): Promise<ApiResponse<any>> {
    return this.#request("geofences/assign", {
      method: "POST",
      body: JSON.stringify(request),
    });
  }

  async removeGeofenceAssignment(
    request: AssignRequest,
  ): Promise<ApiResponse<any>> {
    return this.#request("geofences/assign", {
      method: "DELETE",
      body: JSON.stringify(request),
    });
  }

  // Time Window endpoints
  async getActiveTimeWindows(
    limit = 10,
    offset = 0,
  ): Promise<ApiResponse<TimeWindowListResponse>> {
    return this.#request<TimeWindowListResponse>("timewindows", undefined, {
      limit: limit.toString(),
      offset: offset.toString(),
    });
  }

  async createTimeWindow(
    request: TimeWindowCreateRequest,
  ): Promise<ApiResponse<any>> {
    return this.#request("timewindows", {
      method: "POST",
      body: JSON.stringify(request),
    });
  }

  async updateTimeWindow(
    timewindowId: number,
    request: TimeWindowUpdateRequest,
  ): Promise<ApiResponse<any>> {
    return this.#request(`timewindows/${timewindowId}`, {
      method: "PUT",
      body: JSON.stringify(request),
    });
  }

  async deleteTimeWindow(timewindowId: number): Promise<ApiResponse<any>> {
    return this.#request(`timewindows/${timewindowId}`, {
      method: "DELETE",
    });
  }

  async getAssignedTimeWindows(mobile: string): Promise<ApiResponse<any>> {
    return this.#request("timewindows/assigned", undefined, { mobile });
  }

  async assignTimeWindow(request: AssignRequest): Promise<ApiResponse<any>> {
    return this.#request("timewindows/assign", {
      method: "POST",
      body: JSON.stringify(request),
    });
  }

  async removeTimeWindowAssignment(
    request: AssignRequest,
  ): Promise<ApiResponse<any>> {
    return this.#request("timewindows/assign", {
      method: "DELETE",
      body: JSON.stringify(request),
    });
  }
}

/**
 * React hook returning an authenticated iVALT Settings client bound to the
 * current realm.
 */
export function useKeyclockidpClient(): KeyClockIDPClient {
  const { adminClient } = useAdminClient();
  return useMemo(() => new KeyClockIDPClient(adminClient), [adminClient]);
}
