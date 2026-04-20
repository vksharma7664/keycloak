import {
  GeofenceCreateRequest,
  GeofenceListResponse,
  GeofenceUpdateRequest,
  TimeWindowCreateRequest,
  TimeWindowListResponse,
  TimeWindowUpdateRequest,
  ApiResponse,
  AssignRequest,
} from "./types";

const API_BASE_URL = "https://dev.api.ivalt.com/admin/public/api/keyclockidp";
const API_KEY = process.env.KEYCLOCKIDP_API_KEY || "";

class KeyClockIDPClient {
  #headers: Record<string, string> = {
    "Content-Type": "application/json",
    "x-api-key": API_KEY,
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

  // Geofence endpoints
  async getActiveGeofences(
    mobile: string,
    limit = 10,
    offset = 0,
  ): Promise<ApiResponse<GeofenceListResponse>> {
    const params = new URLSearchParams([
      ["mobile", mobile],
      ["limit", limit.toString()],
      ["offset", offset.toString()],
    ]);
    return this.request<GeofenceListResponse>(
      `/geofence/active-list?${params}`,
    );
  }

  async createGeofence(
    request: GeofenceCreateRequest,
  ): Promise<ApiResponse<any>> {
    return this.request("/geofence/create", {
      method: "POST",
      body: JSON.stringify(request),
    });
  }

  async updateGeofence(
    geofenceId: number,
    request: GeofenceUpdateRequest,
  ): Promise<ApiResponse<any>> {
    return this.request(`/geofence/update/${geofenceId}`, {
      method: "PUT",
      body: JSON.stringify(request),
    });
  }

  async deleteGeofence(
    geofenceId: number,
    mobile: string,
  ): Promise<ApiResponse<any>> {
    return this.request(`/geofence/delete/${geofenceId}`, {
      method: "DELETE",
      body: JSON.stringify({ mobile }),
    });
  }

  async getAssignedGeofences(mobile: string): Promise<ApiResponse<any>> {
    const params = new URLSearchParams({ mobile });
    return this.request(`/geofence/assigned-list?${params}`);
  }

  async assignGeofence(request: AssignRequest): Promise<ApiResponse<any>> {
    return this.request("/geofence/assign", {
      method: "POST",
      body: JSON.stringify(request),
    });
  }

  async removeGeofenceAssignment(
    request: AssignRequest,
  ): Promise<ApiResponse<any>> {
    return this.request("/geofence/assigned-delete", {
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
    const params = new URLSearchParams([
      ["mobile", mobile],
      ["limit", limit.toString()],
      ["offset", offset.toString()],
    ]);
    return this.request<TimeWindowListResponse>(
      `/timewindow/active-list?${params}`,
    );
  }

  async createTimeWindow(
    request: TimeWindowCreateRequest,
  ): Promise<ApiResponse<any>> {
    return this.request("/timewindow/create", {
      method: "POST",
      body: JSON.stringify(request),
    });
  }

  async updateTimeWindow(
    timewindowId: number,
    request: TimeWindowUpdateRequest,
  ): Promise<ApiResponse<any>> {
    return this.request(`/timewindow/update/${timewindowId}`, {
      method: "PUT",
      body: JSON.stringify(request),
    });
  }

  async deleteTimeWindow(
    timewindowId: number,
    mobile: string,
  ): Promise<ApiResponse<any>> {
    return this.request(`/timewindow/delete/${timewindowId}`, {
      method: "DELETE",
      body: JSON.stringify({ mobile }),
    });
  }

  async getAssignedTimeWindows(mobile: string): Promise<ApiResponse<any>> {
    const params = new URLSearchParams([["mobile", mobile]]);
    return this.request(`/timewindow/assigned-list?${params}`);
  }

  async assignTimeWindow(request: AssignRequest): Promise<ApiResponse<any>> {
    return this.request("/timewindow/assign", {
      method: "POST",
      body: JSON.stringify(request),
    });
  }

  async removeTimeWindowAssignment(
    request: AssignRequest,
  ): Promise<ApiResponse<any>> {
    return this.request("/timewindow/assigned-delete", {
      method: "DELETE",
      body: JSON.stringify(request),
    });
  }
}

export const keyclockidpClient = new KeyClockIDPClient();
