import {
  GeofenceCreateRequest,
  GeofenceUpdateRequest,
  GeofenceListResponse,
  TimeWindowCreateRequest,
  TimeWindowUpdateRequest,
  TimeWindowListResponse,
  AssignRequest,
  ApiResponse,
} from "./types";

const API_BASE_URL = "/admin"; // Keycloak admin REST base URL

class KeyClockIDPClient {
  #headers: Record<string, string> = {
    "Content-Type": "application/json",
  };

  async request<T>(
    endpoint: string,
    options: RequestInit = {},
  ): Promise<ApiResponse<T>> {
    try {
      const headers: Record<string, string> = { ...this.#headers };
      if (options.headers) {
        Object.assign(headers, options.headers);
      }
      const response = await fetch(`${API_BASE_URL}${endpoint}`, {
        ...options,
        headers,
      });

      const data = await response.json();

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

  getRealmPath(): string {
    // Get realm from current URL path
    const pathParts = window.location.pathname.split("/");
    const realmIndex = pathParts.indexOf("console") - 1;
    return realmIndex >= 0 ? pathParts[realmIndex] : "master";
  }

  // Geofence endpoints
  async getActiveGeofences(
    mobile: string,
    limit = 10,
    offset = 0,
  ): Promise<ApiResponse<GeofenceListResponse>> {
    const realm = this.getRealmPath();
    const params = new URLSearchParams([
      ["mobile", mobile],
      ["limit", limit.toString()],
      ["offset", offset.toString()],
    ]);
    return this.request<GeofenceListResponse>(
      `/realms/${realm}/ivalt-settings/geofences?${params}`,
    );
  }

  async createGeofence(
    request: GeofenceCreateRequest,
  ): Promise<ApiResponse<any>> {
    const realm = this.getRealmPath();
    return this.request(`/realms/${realm}/ivalt-settings/geofences`, {
      method: "POST",
      body: JSON.stringify(request),
    });
  }

  async updateGeofence(
    geofenceId: number,
    request: GeofenceUpdateRequest,
  ): Promise<ApiResponse<any>> {
    const realm = this.getRealmPath();
    return this.request(
      `/realms/${realm}/ivalt-settings/geofences/${geofenceId}`,
      {
        method: "PUT",
        body: JSON.stringify(request),
      },
    );
  }

  async deleteGeofence(
    geofenceId: number,
    mobile: string,
  ): Promise<ApiResponse<any>> {
    const realm = this.getRealmPath();
    return this.request(
      `/realms/${realm}/ivalt-settings/geofences/${geofenceId}?mobile=${mobile}`,
      {
        method: "DELETE",
      },
    );
  }

  async getAssignedGeofences(mobile: string): Promise<ApiResponse<any>> {
    const realm = this.getRealmPath();
    return this.request(
      `/realms/${realm}/ivalt-settings/geofences/assigned?mobile=${mobile}`,
    );
  }

  async assignGeofence(request: AssignRequest): Promise<ApiResponse<any>> {
    const realm = this.getRealmPath();
    return this.request(`/realms/${realm}/ivalt-settings/geofences/assign`, {
      method: "POST",
      body: JSON.stringify(request),
    });
  }

  async removeGeofenceAssignment(
    request: AssignRequest,
  ): Promise<ApiResponse<any>> {
    const realm = this.getRealmPath();
    return this.request(`/realms/${realm}/ivalt-settings/geofences/assign`, {
      method: "DELETE",
      body: JSON.stringify(request),
    });
  }

  // Time Window endpoints
  async getActiveTimeWindows(
    mobile: string,
    limit = 10,
    offset = 0,
  ): Promise<ApiResponse<TimeWindowListResponse>> {
    const realm = this.getRealmPath();
    const params = new URLSearchParams([
      ["mobile", mobile],
      ["limit", limit.toString()],
      ["offset", offset.toString()],
    ]);
    return this.request<TimeWindowListResponse>(
      `/realms/${realm}/ivalt-settings/timewindows?${params}`,
    );
  }

  async createTimeWindow(
    request: TimeWindowCreateRequest,
  ): Promise<ApiResponse<any>> {
    const realm = this.getRealmPath();
    return this.request(`/realms/${realm}/ivalt-settings/timewindows`, {
      method: "POST",
      body: JSON.stringify(request),
    });
  }

  async updateTimeWindow(
    timewindowId: number,
    request: TimeWindowUpdateRequest,
  ): Promise<ApiResponse<any>> {
    const realm = this.getRealmPath();
    return this.request(
      `/realms/${realm}/ivalt-settings/timewindows/${timewindowId}`,
      {
        method: "PUT",
        body: JSON.stringify(request),
      },
    );
  }

  async deleteTimeWindow(
    timewindowId: number,
    mobile: string,
  ): Promise<ApiResponse<any>> {
    const realm = this.getRealmPath();
    return this.request(
      `/realms/${realm}/ivalt-settings/timewindows/${timewindowId}?mobile=${mobile}`,
      {
        method: "DELETE",
      },
    );
  }

  async getAssignedTimeWindows(mobile: string): Promise<ApiResponse<any>> {
    const realm = this.getRealmPath();
    return this.request(
      `/realms/${realm}/ivalt-settings/timewindows/assigned?mobile=${mobile}`,
    );
  }

  async assignTimeWindow(request: AssignRequest): Promise<ApiResponse<any>> {
    const realm = this.getRealmPath();
    return this.request(`/realms/${realm}/ivalt-settings/timewindows/assign`, {
      method: "POST",
      body: JSON.stringify(request),
    });
  }

  async removeTimeWindowAssignment(
    request: AssignRequest,
  ): Promise<ApiResponse<any>> {
    const realm = this.getRealmPath();
    return this.request(`/realms/${realm}/ivalt-settings/timewindows/assign`, {
      method: "DELETE",
      body: JSON.stringify(request),
    });
  }
}

export const keyclockidpClient = new KeyClockIDPClient();
