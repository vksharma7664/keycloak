# iVALT MFA — Keycloak Integration

iVALT provides push-notification-based biometric authentication as a Keycloak MFA provider. During login, a push notification is sent to the user's mobile device, and they approve/reject biometrically.

## Architecture

```mermaid
graph TB
    subgraph Browser["User Browser"]
        KC_AC["Account Console<br/>(enrollment)"]
        KC_ADMIN["Admin Console<br/>(configuration)"]
        LOGIN["Login Page"]
    end

    subgraph Keycloak["Keycloak Server"]
        AUTH["IvaltAuthenticator<br/>(login flow)"]
        REQ["ConfigureIvalt<br/>(required action)"]
        CRED["IvaltCredentialProvider<br/>(store mobile number)"]
        ADMIN_API["IvaltSettingsResource<br/>(REST API)"]
        UI_CFG["IvaltConfigSection<br/>(admin config form)"]
        UI_GF["GeoFenceSection<br/>TimeWindowSection"]
        UI_USER["UserGeofence<br/>UserTimeWindow<br/>(user detail tabs)"]
    end

    subgraph IvaltAPI["iVALT Cloud API<br/>api.ivalt.com"]
        PUSH["Push Notification<br/>/send/global/notification"]
        VALIDATE["Auth Validation<br/>/validate-geo-fence-auth"]
        KEYCLOCK["KeyClockIDP Admin API<br/>/keyclock/organization/*"]
    end

    subgraph Mobile["User Mobile"]
        IVALT_APP["iVALT Mobile App"]
    end

    LOGIN --> AUTH
    AUTH --> PUSH
    PUSH --> IVALT_APP
    IVALT_APP --> VALIDATE
    AUTH --> VALIDATE

    KC_AC --> REQ
    REQ --> PUSH
    REQ --> CRED

    KC_ADMIN --> UI_CFG
    KC_ADMIN --> UI_GF
    KC_ADMIN --> UI_USER
    UI_CFG --> ADMIN_API
    UI_GF --> ADMIN_API
    UI_USER --> ADMIN_API
    ADMIN_API --> KEYCLOCK
```

## Key Components

| Component | Type | Location | Purpose |
|-----------|------|----------|---------|
| `IvaltAuthenticator` | Java SPI | `services/.../browser/` | Login flow — sends push, polls for approval |
| `ConditionalIvaltAuthenticator` | Java SPI | `services/.../browser/` | Self-service variant — only runs if user has iVALT configured |
| `ConfigureIvalt` | Java SPI | `services/.../requiredactions/` | User enrollment — collects mobile number, verifies via push |
| `IvaltCredentialProvider` | Java SPI | `services/.../credential/` | Stores mobile number + country code as a Keycloak credential |
| `IvaltSettingsResource` | Java REST | `services/.../admin/` | Admin API — geofence/timewindow CRUD, config management |
| `IvaltApiClient` | Java HTTP | `services/.../browser/` | Auth API client — sends push, validates auth |
| `KeyClockIDPApiClient` | Java HTTP | `services/.../browser/` | Admin API client — geofence/timewindow CRUD |
| `IvaltConfigSection` | React | `js/apps/admin-ui/src/ivalt-settings/` | Config form (org code, mobile, API key, base URL) |
| `GeoFenceSection` | React | `js/apps/admin-ui/src/ivalt-settings/` | Geofence management page |
| `TimeWindowSection` | React | `js/apps/admin-ui/src/ivalt-settings/` | Time window management page |
| `GeofenceList/Table/Form` | React | `js/apps/admin-ui/src/ivalt-settings/geofence/` | Geofence CRUD components |
| `TimeWindowList/Table/Form` | React | `js/apps/admin-ui/src/ivalt-settings/timewindow/` | Time window CRUD components |
| `GoogleMapPicker` | React | `js/apps/admin-ui/src/ivalt-settings/geofence/` | Map-based radius picker for geofences |
| `UserGeofence` | React | `js/apps/admin-ui/src/user/` | User detail tab — assign/unassign geofences |
| `UserTimeWindow` | React | `js/apps/admin-ui/src/user/` | User detail tab — assign/unassign time windows |
| `KeyClockIDPClient` | TypeScript | `js/apps/admin-ui/src/ivalt-settings/api/` | Authenticated API client for admin REST endpoints |

## Docs

| Doc | Description |
|-----|-------------|
| [Authentication Flow](authentication-flow.md) | Login flow and enrollment sequence diagrams |
| [Admin Console UI](admin-console-ui.md) | Admin UI components, routing, navigation |
| [Backend API](backend-api.md) | REST API endpoints, config model, data flow |
| [Setup Guide](setup-guide.md) | Building, deployment, and configuration |
