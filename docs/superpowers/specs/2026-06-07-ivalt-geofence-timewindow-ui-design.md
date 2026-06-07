# iVALT Geofence & Time Window — Admin Console UI

> Created: 2026-06-07
> Status: Design — awaiting approval
> Branch: feature/ivalt-settings-implementation

## Overview

Wire the already-built iVALT Geofence and Time Window capability into the Keycloak
admin console so it is reachable and usable, matching the target screenshots:

- **Image 1** — realm left-nav gains **Time windows** and **Geo Fences** items, plus a
  **Configure** (iVALT settings) page for org credentials.
- **Image 2** — the user-detail page gains working **GeoFence** and **Timewindow** tabs
  that view and assign/unassign per-user.

The backend proxy (`IvaltSettingsResource` → `KeyClockIDPApiClient` → `api.ivalt.com`)
and the realm-level React components (List/Table/Form/`GoogleMapPicker`) already exist
but are **not wired up**, **not authenticated**, and **hardcode a mobile number**.

## Current State (verified)

| Layer | What exists | Gap |
|-------|-------------|-----|
| Backend resource | `IvaltSettingsResource` registered in `RealmAdminResource` (`@Path("ivalt-settings")`) with geofence/timewindow CRUD + assign endpoints | No admin auth check; API key hardcoded `""`; base URL hardcoded; no config endpoints |
| Backend API client | `KeyClockIDPApiClient` — all 14 endpoints implemented (mobile-based) | none (mobile-based by design) |
| Realm UI components | `GeofenceList/Table/Form`, `TimeWindowList/Table/Form`, `GoogleMapPicker`, `IvaultSettings` tabbed page | Hardcoded mobile `+919530654704`; not routed; not in nav as separate items |
| API client (TS) | `keyclockidpClient` — all endpoints mapped | **No `Authorization` header** → 401; brittle realm-from-URL parsing |
| Routing | `IvaultSettingsRoute` defined in `ivalt-settings/routes/index.tsx` | **Not registered** in `src/routes.tsx` |
| Nav | `<LeftNav title="ivaltSettings" path="/ivalt-settings" />` already in Configure group | Hidden (route not registered); no separate Time windows / Geo Fences items |
| User tabs | `UserGeofence`/`UserTimeWindow` tabs added in `EditUser.tsx` | Stub placeholders; stale-state mobile bug |

## Decisions (from brainstorming)

1. Screenshots are the **target design** to build toward.
2. **Two separate** left-nav items: "Time windows" and "Geo Fences" (Manage group).
3. Realm pages are **org-scoped from saved config** — no manual mobile entry.
4. User tabs support **view + assign/unassign**.
5. Org credentials (org mobile, API key, base URL) stored in a **new iVALT settings
   section**, surfaced via the "Configure" page in Image 1.

## Architecture

### Configuration model — `IvaltConfig` (realm attributes)

Single source of truth for org credentials, stored as realm attributes:

| Attribute | Key | Notes |
|-----------|-----|-------|
| Org mobile | `ivalt.org.mobile` | E.164, e.g. `+1...`. Owner account for geofences/time windows |
| API key | `ivalt.api.key` | `x-api-key`; write-only in UI (never echoed back) |
| API base URL | `ivalt.api.base.url` | default `https://api.ivalt.com` |

Backend reads these via `realm.getAttribute(...)`. This replaces the hardcoded
`Map.of(...)` in `IvaltSettingsResource` and the empty API key.

> Rationale: realm attributes are the standard Keycloak place for per-realm config,
> already serialized to the admin REST realm representation, and avoid burying
> credentials in authentication-flow executor config.

### Backend changes — `IvaltSettingsResource`

1. **Auth gate.** Inject `AdminPermissionEvaluator` (as sibling sub-resources do via
   `RealmAdminResource`) and require `realm.manageRealm()` (write) / `viewRealm()` (read)
   before proxying. Currently there is **no** authz — must be added.
2. **Config-driven client.** Build `KeyClockIDPApiClient` from realm attributes
   (`ivalt.api.base.url`, `ivalt.api.key`, timeout) instead of hardcoded values.
3. **Resolve org mobile server-side.** List/create/update/delete read the org mobile
   from `ivalt.org.mobile` when the caller does not supply one, so the realm pages never
   need to send a mobile. (Keep `mobile` query param optional for per-user/admin override.)
4. **New config endpoints** under the same resource:
   - `GET ivalt-settings/config` → `{ orgMobile, apiBaseUrl, apiKeyConfigured: boolean }`
     (never returns the key itself)
   - `PUT ivalt-settings/config` → persists the three attributes (key only overwritten
     when a non-empty value is sent)
5. **Pass `RealmAdminResource`'s `auth`** into the constructor (signature change:
   `new IvaltSettingsResource(session, realm, auth)`).

### Frontend — authenticated API client

`keyclockidpClient` must call the admin REST API **with the bearer token**. Two options:

- **(Selected) Use `useAdminClient()`'s `adminClient`** — extend the
  `KeycloakAdminClient` request path, or call `fetch` with
  `Authorization: Bearer ${await keycloak.token}` obtained from the admin client.
  The client already wires token refresh; reuse it.
- Rejected: keep raw `fetch` and manually thread tokens — duplicates auth logic.

Refactor `keyclockidpClient` into a hook/factory that receives the `adminClient` (or its
token + baseUrl), adds the auth header, and derives the realm from `RealmContext` rather
than parsing `window.location`.

### Frontend — realm pages & routing

Split the single tabbed `IvaultSettings` into three routed pages so nav items are
independent (matching Image 1):

```
src/ivalt-settings/
  routes/index.tsx        → 3 routes:  /:realm/geofences
                                       /:realm/time-windows
                                       /:realm/ivalt-settings   (config)
  GeoFenceSection.tsx     → <GeofenceList/>   (page wrapper)
  TimeWindowSection.tsx   → <TimeWindowList/> (page wrapper)
  IvaltConfigSection.tsx  → config form (org mobile, API key, base URL)
```

- Register all three in `src/routes.tsx` (access: `view-realm`).
- `GeofenceList`/`TimeWindowList`/forms: remove hardcoded mobile. Org mobile is now
  resolved server-side, so the create/update/delete requests omit `mobile` (or send the
  configured one fetched once via the config endpoint).

### Frontend — navigation (`PageNav.tsx`)

- **Manage** group: add `<LeftNav title="geoFences" path="/geofences" />` and
  `<LeftNav title="timeWindows" path="/time-windows" />` after `events`.
- **Configure** group: keep the existing `<LeftNav title="ivaltSettings" path="/ivalt-settings" />`
  (now routed) as the "Configure" iVALT page from Image 1.

### Frontend — user-detail tabs

Implement `UserGeofence` and `UserTimeWindow` (replace stubs):

- Read the **user mobile** from the user attribute `mobile_number` (fix the stale-state
  bug — derive from the fetched user, not from state set in the same effect).
- **Assigned list:** `getAssignedGeofences(userMobile)` / `getAssignedTimeWindows(userMobile)`
  rendered in a PatternFly table.
- **Assign:** a "+ Assign" action opens a selector populated from the org's active list
  (`getActiveGeofences()` / `getActiveTimeWindows()`); calls `assignGeofence` /
  `assignTimeWindow` with `{ mobile: orgMobile, user_mobile: userMobile, geofence_id|timewindow_id }`.
- **Unassign:** row action calls `removeGeofenceAssignment` / `removeTimeWindowAssignment`.
- Empty state when the user has no `mobile_number` attribute (prompt to set it).

### i18n

Add keys to `messages` bundle: `geoFences`, `timeWindows`, `ivaltSettings` (exists),
`geofence`, `timewindow`, `assignGeofence`, `assignTimeWindow`,
`userGeofenceAssignments`, `userTimeWindowAssignments`, plus error keys
`geofenceFetchError`, `timeWindowFetchError`. Replace inline English strings in the
existing components with `t(...)` where practical.

## Data Flow

```
Admin UI (React, bearer token)
  → /admin/realms/{realm}/ivalt-settings/*   (Keycloak admin REST, authz-checked)
    → IvaltSettingsResource (reads realm attrs: org mobile, api key, base url)
      → KeyClockIDPApiClient → https://api.ivalt.com/admin/public/api/keyclockidp/*
```

Realm pages: org mobile injected server-side.
User tabs: org mobile (config) + user_mobile (user attribute) sent on assign/unassign.

## Error Handling

- Backend: 401/403 when not `manageRealm`/`viewRealm`; 409-style JSON `{error}` when
  iVALT config is missing (org mobile / API key not set) — surfaced as an inline alert
  prompting the admin to open the Configure page.
- Frontend: replace ad-hoc `Alert` state with the shared `useAlerts()` toast pattern used
  elsewhere in admin-ui for consistency.
- User tabs: graceful empty state when `mobile_number` is absent.

## Testing

- **Backend (JUnit):** `IvaltSettingsResource` — authz enforced; config read from realm
  attributes; org mobile resolution; config GET masks the API key; config PUT persists
  and only overwrites the key when provided. Mock `KeyClockIDPApiClient`.
- **Frontend:** request helper adds `Authorization` header and correct realm path;
  config form round-trips; user-tab assign/unassign call the right endpoints with org +
  user mobile; empty state when no mobile.
- **Manual (browser):** nav shows the three items; pages load org data; create geofence
  via map picker; assign/unassign on a user; config page saves and key is never echoed.

## Out of Scope

- Changes to the iVALT MFA authenticator runtime flow.
- Bulk assignment / CSV import.
- Map provider changes (keep `@react-google-maps/api`).
- Group-level (vs user-level) assignment.

## Open Mapping Note for Reviewer

Image 1 shows "Configure" adjacent to Time windows/Geo Fences. This design keeps the
iVALT **Configure** page in the standard **Configure** nav group (next to Realm settings),
while Time windows / Geo Fences go in **Manage**. If you want all three clustered together
in Manage instead, that is a one-line nav change — flag it on review.
