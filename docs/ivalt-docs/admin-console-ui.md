# Admin Console UI

## Navigation Structure

Three iVALT-related items appear in the admin console left navigation:

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

```typescript
// Manage group (after Events)
<LeftNav title="timeWindows" path="/time-windows" />
<LeftNav title="geoFences" path="/geofences" />

// Configure group (after Realm Settings)
<LeftNav title="ivaltSettings" path="/ivalt-settings" />
```

## Route Structure

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

## iVALT Settings Page (`IvaltConfigSection`)

The config form stores org credentials as realm attributes:

| Field | Realm Attribute | Notes |
|-------|-----------------|-------|
| Org Code | `ivalt.org.code` | Identifies the org in the iVALT API |
| User Mobile | `ivalt.user.mobile` | Owner account for assignments (E.164) |
| API Base URL | `ivalt.api.base.url` | Default: `https://api.ivalt.com` |
| API Key | `ivalt.api.key` | Password field; never echoed back |

**Flow:**

```mermaid
sequenceDiagram
    participant Admin as Admin User
    participant UI as IvaltConfigSection
    participant Client as KeyClockIDPClient (TS)
    participant Backend as IvaltSettingsResource

    Admin->>UI: Open /:realm/ivalt-settings
    UI->>Client: getConfig()
    Client->>Backend: GET /admin/realms/{realm}/ivalt-settings/config
    Backend-->>Client: { orgCode, userMobile, apiBaseUrl, apiKeyConfigured }
    Client-->>UI: Populate form
    Admin->>UI: Edit fields
    Admin->>UI: Click Save
    UI->>Client: updateConfig({ orgCode, userMobile, apiBaseUrl, apiKey? })
    Client->>Backend: PUT /admin/realms/{realm}/ivalt-settings/config
    Backend->>Backend: realm.setAttribute(...)
    Backend-->>Client: Updated config (apiKey still masked)
    Client-->>UI: Success alert
```

## Geofence Management

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

## User Detail Tabs

Each user page in the admin console has **GeoFence** and **TimeWindow** tabs for assigning/unassigning restrictions:

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

**Assign/Unassign flow:**

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
