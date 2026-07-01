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

export interface GeofenceCreateRequest {
  name: string;
  latitude: number;
  longitude: number;
  radius: number;
  is_active?: boolean;
}

export interface GeofenceUpdateRequest {
  name?: string;
  latitude?: number;
  longitude?: number;
  radius?: number;
  is_active?: boolean;
}

export interface PaginationMeta {
  current_page: number;
  per_page: number;
  total: number;
  last_page: number;
}

export interface GeofenceListResponse {
  success: boolean;
  data: Geofence[];
  meta?: PaginationMeta;
  message?: string;
}

export interface TimeWindow {
  id: number;
  organization_id: number;
  name?: string;
  start_time: string;
  end_time: string;
  timezone: string[];
  status: boolean;
  created_at: string;
  updated_at: string;
}

export interface TimeWindowCreateRequest {
  timezone: string[];
  start_time: string;
  end_time: string;
  status: boolean;
}

export interface TimeWindowUpdateRequest {
  timezone: string[];
  start_time: string;
  end_time: string;
  status: boolean;
}

export interface TimeWindowListResponse {
  success: boolean;
  data: TimeWindow[];
  meta?: PaginationMeta;
  message?: string;
}

export interface AssignRequest {
  orgGeoFence_ids?: number[];
  timeslot_ids?: number[];
}

export interface ApiResponse<T> {
  success: boolean;
  data?: T;
  meta?: PaginationMeta;
  message?: string;
  error?: string;
}

export interface IvaltConfig {
  orgMobile: string;
  orgCode: string;
  userMobile: string;
  apiBaseUrl: string;
  apiKeyConfigured: boolean;
}

export interface IvaltConfigUpdateRequest {
  orgMobile?: string;
  orgCode?: string;
  userMobile?: string;
  apiBaseUrl?: string;
  apiKey?: string;
}
