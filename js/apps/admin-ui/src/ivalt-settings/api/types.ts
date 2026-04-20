export interface MobileRequest {
  mobile: string;
}

export interface Geofence {
  id: number;
  organization_id: number;
  name: string;
  latitude: number;
  longitude: number;
  radius: number;
  is_active: boolean;
  created_at: string;
  updated_at: string;
}

export interface GeofenceCreateRequest extends MobileRequest {
  name: string;
  latitude: number;
  longitude: number;
  radius: number;
  is_active?: boolean;
}

export interface GeofenceUpdateRequest extends MobileRequest {
  name?: string;
  latitude?: number;
  longitude?: number;
  radius?: number;
  is_active?: boolean;
}

export interface GeofenceListResponse {
  success: boolean;
  data: Geofence[];
  total: number;
  limit: number;
  offset: number;
}

export interface TimeWindow {
  id: number;
  organization_id: number;
  name: string;
  start_time: string;
  end_time: string;
  days_active: string[];
  timezone: string[];
  status: boolean;
  created_at: string;
  updated_at: string;
}

export interface TimeWindowCreateRequest extends MobileRequest {
  name: string;
  start_time: string;
  end_time: string;
  days_active: string[];
  timezone?: string[];
  status?: boolean;
}

export interface TimeWindowUpdateRequest extends MobileRequest {
  name?: string;
  start_time?: string;
  end_time?: string;
  days_active?: string[];
  timezone?: string[];
  status?: boolean;
}

export interface TimeWindowListResponse {
  success: boolean;
  data: TimeWindow[];
  total: number;
  limit: number;
  offset: number;
}

export interface AssignRequest {
  mobile: string;
  user_mobile: string;
  geofence_id?: number;
  timewindow_id?: number;
}

export interface ApiResponse<T> {
  success: boolean;
  data?: T;
  message?: string;
  error?: string;
}

export type DayOfWeek = "Mon" | "Tue" | "Wed" | "Thu" | "Fri" | "Sat" | "Sun";
