# Admin Console UI

This document describes the React-based admin console interface for managing iVALT settings, geofences, time windows, and user assignments. It covers the navigation structure, route registration, config page, geofence and time window management, and user detail tabs.

---

## Navigation Structure

Three iVALT-related items appear in the Keycloak admin console's left navigation panel, distributed across two groups.

In the **Manage** group (which contains operational settings like Events), two items are added: **Geo Fences** (`/:realm/geofences`) and **Time Windows** (`/:realm/time-windows`). These allow administrators to create, edit, and delete location-based and time-based policies.

In the **Configure** group (which contains structural settings like Realm Settings and Authentication), one item is added: **iVALT Settings** (`/:realm/ivalt-settings`). This is where administrators manage the organization-level credentials needed to communicate with the iVALT Cloud API.

```mermaid
graph TB
    subgraph Nav["Left Navigation"]
        subgraph Manage["Manage Group"]
            EVENTS["Events"]
            TW["⏰ Time Windows<br/>/:realm/time-windows"]
            GF["📍 Geo Fences<br/>/:realm/geofences"]
        end

        subgraph Configure["Configure Group"]
            RS["Realm Settings"]
            IS["🔧 iVALT Settings<br/>/:realm/ivalt-settings"]
            AUTH["Authentication"]
        end
    end
```

### Nav Registration (`PageNav.tsx`)

The navigation items are registered in `PageNav.tsx` using the `<LeftNav>` component. Each entry specifies the display title (referenced from the i18n messages file) and the route path.

```typescript
// Manage group (after Events)
<LeftNav title="timeWindows" path="/time-windows" />
<LeftNav title="geoFences" path="/geofences" />

// Configure group (after Realm Settings)
<LeftNav title="ivaltSettings" path="/ivalt-settings" />
```

---

## Route Structure

Each navigation item maps to a route registered in `src/routes.tsx`. These routes define the URL pattern, the loader function, and the React component to render.

The `/:realm/ivalt-settings` route renders `IvaltConfigSection`, which contains the org-level configuration form. The `/:realm/geofences` route renders `GeoFenceSection`, which contains the geofence list, table, create/edit form, and the Google Map picker. The `/:realm/time-windows` route renders `TimeWindowSection`, which contains the time window list, table, and create/edit form.

```mermaid
graph LR
    subgraph Routes["Registered in src/routes.tsx"]
        R1["/:realm/geofences"]
        R2["/:realm/time-windows"]
        R3["/:realm/ivalt-settings"]
    end

    R1-->GFS["GeoFenceSection"]
    R2-->TWS["TimeWindowSection"]
    R3-->ICS["IvaltConfigSection"]

    subgraph Components
        GFS-->GL["GeofenceList"]
        GL-->GT["GeofenceTable"]
        GL-->GFR["GeofenceForm"]
        GFR-->GMP["GoogleMapPicker"]

        TWS-->TL["TimeWindowList"]
        TL-->TT["TimeWindowTable"]
        TL-->TFR["TimeWindowForm"]

        ICS-->CF["Config Form<br/>orgCode, userMobile,<br/>apiBaseUrl, apiKey"]
    end
```

---

## iVALT Settings Page (`IvaltConfigSection`)

The iVALT Settings page is where administrators configure the organization's credentials for the iVALT Cloud API. These values are stored as realm attributes in Keycloak's database, making them specific to each realm.

Five fields are configurable:

| Field | Realm Attribute | Notes |
|-------|-----------------|-------|
| Org Code | `ivalt.org.code` | Identifies the org in the iVALT API |
| User Mobile | `ivalt.user.mobile` | Owner account for assignments (E.164) |
| API Base URL | `ivalt.api.base.url` | Default: `https://api.ivalt.com` |
| API Key | `ivalt.api.key` | Password field; never echoed back |
| Google Maps API Key | `ivalt.google.maps.api.key` | Used for map picker in geofence creation |

The **API Key** is write-only — when the config is fetched, the backend returns `apiKeyConfigured: boolean` rather than the key value itself. This means the key can only be set or overwritten, never read back.

**Flow:** When the admin opens the iVALT Settings page, the `IvaltConfigSection` component calls `KeyClockIDPClient.getConfig()`, which sends a GET request to the backend. The backend reads the realm attributes and returns them (with the API key masked). The admin edits the fields and clicks Save, triggering a PUT request to the backend which updates the realm attributes.

```mermaid
sequenceDiagram
    participant Admin as Admin User
    participant UI as IvaltConfigSection
    participant Client as KeyClockIDPClient (TS)
    participant Backend as IvaltSettingsResource

    Admin->>UI: Open /:realm/ivalt-settings
    UI->>Client: getConfig()
    Client->>Backend: GET /admin/realms/{realm}/ivalt-settings/config
    Backend-->>Client: { orgCode, userMobile, apiBaseUrl, apiKeyConfigured, googleMapsApiKey }
    Client-->>UI: Populate form
    Admin->>UI: Edit fields
    Admin->>UI: Click Save
    UI->>Client: updateConfig({ orgCode, userMobile, apiBaseUrl, apiKey?, googleMapsApiKey })
    Client->>Backend: PUT /admin/realms/{realm}/ivalt-settings/config
    Backend->>Backend: realm.setAttribute(...)
    Backend-->>Client: Updated config (apiKey still masked)
    Client-->>UI: Success alert
```

---

## Geofence Management

The geofence management page allows administrators to create, view, edit, and delete geofences. A geofence is defined by a name, a center point (latitude/longitude selected on a Google Map), and a radius in meters.

When the admin opens the Geo Fences page, `GeofenceList` fetches the paginated list from the backend, which proxies the request to the iVALT Cloud API's KeyClockIDP endpoint. The response is transformed from the iVALT format to the frontend format before rendering in a table.

Creating a geofence opens a modal (`GeofenceForm`) that includes the `GoogleMapPicker` component — an interactive Google Map where the admin clicks to set the center point and adjusts a radius slider. The Google Maps API key is loaded from the iVALT settings configuration. On submit, the data is sent to the backend, which creates the geofence via the iVALT API and returns the result.

```mermaid
sequenceDiagram
    participant Admin as Admin User
    participant GF as GeofenceList
    participant Client as KeyClockIDPClient
    participant Backend as IvaltSettingsResource
    participant IvaltAPI as iVALT API

    Admin->>GF: Open /:realm/geofences
    GF->>Client: getGeofences(page, perPage)
    Client->>Backend: GET /ivalt-settings/geofences
    Backend->>IvaltAPI: GET /keyclock/organization/{orgCode}/geo-fence
    IvaltAPI-->>Backend: Geofence list
    Backend-->>Client: Transformed response
    Client-->>GF: Render table

    Admin->>GF: Click Create Geofence
    GF->>GF: Open GeofenceForm modal
    Admin->>GF: Fill name, pick location on map, set radius
    Admin->>GF: Submit
    GF->>Client: createGeofence({ name, latitude, longitude, radius })
    Client->>Backend: POST /ivalt-settings/geofences
    Backend->>IvaltAPI: POST /keyclock/organization/{orgCode}/geo-fence
    IvaltAPI-->>Backend: Created
    Backend-->>Client: Success
    Client-->>GF: Refresh list
```

---

## User Detail Tabs

Each user detail page in the admin console (`/:realm/users/{id}`) includes two additional tabs: **GeoFence** and **TimeWindow**. These tabs allow administrators to assign or unassign geofences and time windows to individual users.

The **GeoFence Tab** (`UserGeofence`) shows a dropdown of available geofences (all geofences created for the org, minus ones already assigned to this user) with an Assign button, and a list of currently assigned geofences with Remove buttons.

The **TimeWindow Tab** (`UserTimeWindow`) works identically — a dropdown of available time windows, an Assign button, and an assigned list with Remove buttons.

```mermaid
graph TB
    subgraph UserPage["User Detail (/:realm/users/{id})"]
        subgraph Tabs["Tabs"]
            T1["Details"]
            T2["Credentials"]
            T3["📍 GeoFence"]
            T4["⏰ TimeWindow"]
        end

        subgraph UserGeofenceTab["GeoFence Tab"]
            T3-->UG["UserGeofence"]
            UG-->UGA["Assign dropdown<br/>+ Assign button"]
            UG-->UGT["Assigned list<br/>with Remove button"]
        end

        subgraph UserTimeWindowTab["TimeWindow Tab"]
            T4-->UTW["UserTimeWindow"]
            UTW-->UTWA["Assign dropdown<br/>+ Assign button"]
            UTW-->UTWT["Assigned list<br/>with Remove button"]
        end
    end
```

**Assign/Unassign flow:** When the admin opens a user's GeoFence tab, the tab fetches both the currently assigned geofences and the full list of available geofences. The admin selects a geofence from the dropdown and clicks Assign, which sends a PUT request with the updated assignment list. The backend proxies this to the iVALT API. The same flow applies to time windows.

```mermaid
sequenceDiagram
    participant Admin as Admin User
    participant UG as UserGeofence Tab
    participant Client as KeyClockIDPClient
    participant Backend as IvaltSettingsResource
    participant IvaltAPI as iVALT API

    Admin->>UG: Open user GeoFence tab
    UG->>Client: getUserGeofences() + getGeofences()
    Client->>Backend: GET /ivalt-settings/geofences/assigned
    Client->>Backend: GET /ivalt-settings/geofences
    Backend-->>Client: Assigned + available lists
    Client-->>UG: Render

    Admin->>UG: Select geofence from dropdown
    Admin->>UG: Click Assign
    UG->>Client: updateUserGeofences({ orgGeoFence_ids: [...] })
    Client->>Backend: PUT /ivalt-settings/geofences/assign
    Backend->>IvaltAPI: PUT /keyclock/.../assign-multiple-geo-fence
    IvaltAPI-->>Backend: Updated
    Backend-->>Client: Success
    Client-->>UG: Refresh

    Admin->>UG: Click Remove on assigned item
    UG->>Client: updateUserGeofences({ orgGeoFence_ids: [...] })
    Client->>Backend: PUT /ivalt-settings/geofences/assign
    Backend-->>Client: Success
    Client-->>UG: Refresh
```

---

## Key Files

| File | Role |
|------|------|
| `js/apps/admin-ui/src/ivalt-settings/IvaltConfigSection.tsx` | Config form (org code, mobile, API key, base URL) |
| `js/apps/admin-ui/src/ivalt-settings/GeoFenceSection.tsx` | Geofence page wrapper |
| `js/apps/admin-ui/src/ivalt-settings/TimeWindowSection.tsx` | Time window page wrapper |
| `js/apps/admin-ui/src/ivalt-settings/geofence/GeofenceList.tsx` | Geofence list with pagination |
| `js/apps/admin-ui/src/ivalt-settings/geofence/GeofenceTable.tsx` | Geofence table view |
| `js/apps/admin-ui/src/ivalt-settings/geofence/GeofenceForm.tsx` | Geofence create/edit form (modal) |
| `js/apps/admin-ui/src/ivalt-settings/geofence/GoogleMapPicker.tsx` | Map-based location/radius picker |
| `js/apps/admin-ui/src/ivalt-settings/timewindow/TimeWindowList.tsx` | Time window list with pagination |
| `js/apps/admin-ui/src/ivalt-settings/timewindow/TimeWindowTable.tsx` | Time window table view |
| `js/apps/admin-ui/src/ivalt-settings/timewindow/TimeWindowForm.tsx` | Time window create/edit form (modal) |
| `js/apps/admin-ui/src/ivalt-settings/routes/index.tsx` | Route definitions (3 routes) |
| `js/apps/admin-ui/src/ivalt-settings/api/keyclockidpClient.ts` | Authenticated API client |
| `js/apps/admin-ui/src/ivalt-settings/api/types.ts` | TypeScript types |
| `js/apps/admin-ui/src/user/UserGeofence.tsx` | User detail geofence tab |
| `js/apps/admin-ui/src/user/UserTimeWindow.tsx` | User detail time window tab |
| `js/apps/admin-ui/src/PageNav.tsx` | Left nav (lines 138-139, 146) |
| `js/apps/admin-ui/src/routes.tsx` | Route registry (lines 14, 50) |
