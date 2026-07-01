import type KeycloakAdminClient from "@keycloak/keycloak-admin-client";
import { useMemo } from "react";
import { useAdminClient } from "../../admin-client";
import { getAuthorizationHeaders } from "../../utils/getAuthorizationHeaders";
import { joinPath } from "../../utils/joinPath";
import type {
  Geofence,
  GeofenceCreateRequest,
  GeofenceUpdateRequest,
  TimeWindow,
  TimeWindowCreateRequest,
  TimeWindowUpdateRequest,
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

  // Geofence endpoints
  async getGeofences(
    page?: number,
    perPage?: number,
    sortBy?: string,
    sortOrder?: string,
  ): Promise<ApiResponse<Geofence[]>> {
    const query: Record<string, string> = {};
    if (page) query.page = String(page);
    if (perPage) query.per_page = String(perPage);
    if (sortBy) query.sort_by = sortBy;
    if (sortOrder) query.sort_order = sortOrder;
    return this.#request<Geofence[]>(
      "geofences",
      {},
      Object.keys(query).length ? query : undefined,
    );
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

  async getUserGeofences(
    page?: number,
    perPage?: number,
    sortBy?: string,
    sortOrder?: string,
  ): Promise<ApiResponse<Geofence[]>> {
    const query: Record<string, string> = {};
    if (page) query.page = String(page);
    if (perPage) query.per_page = String(perPage);
    if (sortBy) query.sort_by = sortBy;
    if (sortOrder) query.sort_order = sortOrder;
    return this.#request(
      "geofences/assigned",
      {},
      Object.keys(query).length ? query : undefined,
    );
  }

  async updateUserGeofences(request: AssignRequest): Promise<ApiResponse<any>> {
    return this.#request("geofences/assign", {
      method: "PUT",
      body: JSON.stringify(request),
    });
  }

  // Timeslot endpoints
  async getTimeslots(
    page?: number,
    perPage?: number,
    sortBy?: string,
    sortOrder?: string,
  ): Promise<ApiResponse<TimeWindow[]>> {
    const query: Record<string, string> = {};
    if (page) query.page = String(page);
    if (perPage) query.per_page = String(perPage);
    if (sortBy) query.sort_by = sortBy;
    if (sortOrder) query.sort_order = sortOrder;
    return this.#request<TimeWindow[]>(
      "timewindows",
      {},
      Object.keys(query).length ? query : undefined,
    );
  }

  async createTimeslot(
    request: TimeWindowCreateRequest,
  ): Promise<ApiResponse<any>> {
    return this.#request("timewindows", {
      method: "POST",
      body: JSON.stringify(request),
    });
  }

  async updateTimeslot(
    timewindowId: number,
    request: TimeWindowUpdateRequest,
  ): Promise<ApiResponse<any>> {
    return this.#request(`timewindows/${timewindowId}`, {
      method: "PUT",
      body: JSON.stringify(request),
    });
  }

  async deleteTimeslot(timewindowId: number): Promise<ApiResponse<any>> {
    return this.#request(`timewindows/${timewindowId}`, {
      method: "DELETE",
    });
  }

  async getUserTimeslots(
    page?: number,
    perPage?: number,
    sortBy?: string,
    sortOrder?: string,
  ): Promise<ApiResponse<TimeWindow[]>> {
    const query: Record<string, string> = {};
    if (page) query.page = String(page);
    if (perPage) query.per_page = String(perPage);
    if (sortBy) query.sort_by = sortBy;
    if (sortOrder) query.sort_order = sortOrder;
    return this.#request(
      "timewindows/assigned",
      {},
      Object.keys(query).length ? query : undefined,
    );
  }

  async updateUserTimeslots(request: AssignRequest): Promise<ApiResponse<any>> {
    return this.#request("timewindows/assign", {
      method: "PUT",
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
