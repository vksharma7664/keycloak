# Authentication Flow

## Login Flow (MFA Challenge)

When a user reaches the iVALT authenticator during login:

```mermaid
sequenceDiagram
    participant User as User Browser
    participant KC as Keycloak
    participant IvaltAPI as iVALT API
    participant App as iVALT Mobile App

    User->>KC: Login request
    KC->>KC: Check if user has iVALT credential
    alt No credential & REQUIRED
        KC->>User: Redirect to enrollment (CONFIGURE_IVALT)
    else No credential & CONDITIONAL
        KC->>User: Skip (continue without iVALT)
    else Has credential
        KC->>KC: Retrieve mobile number from IvaltCredentialProvider
        KC->>IvaltAPI: POST /send/global/notification
        IvaltAPI-->>App: Push notification
        IvaltAPI-->>KC: 200 OK (notification sent)
        KC->>User: Render ivalt-auth.ftl (polling page)
        loop Every 3s (up to ~60s)
            User->>KC: POST (check status)
            App->>IvaltAPI: User approves/rejects biometrically
            KC->>IvaltAPI: POST /validate-geo-fence-auth
            alt APPROVED
                IvaltAPI-->>KC: { status: "APPROVED" }
                KC->>User: context.success() → login proceeds
            else REJECTED
                IvaltAPI-->>KC: { status: "REJECTED" }
                KC->>User: Show error
            else INVALID_TIMEZONE
                IvaltAPI-->>KC: { status: "INVALID_TIMEZONE" }
                KC->>User: Show timezone error
            else INVALID_GEOFENCE
                IvaltAPI-->>KC: { status: "INVALID_GEOFENCE" }
                KC->>User: Show geofence error
            else PENDING
                IvaltAPI-->>KC: { status: "PENDING" }
                KC->>User: Continue polling
            end
        end
        opt Timeout (30 polls)
            KC->>User: Show timeout error
        end
    end
```

### Authenticator Provider IDs

| Provider ID | Class | Requirement |
|-------------|-------|-------------|
| `ivalt-authenticator` | `IvaltAuthenticator` | REQUIRED — always executes, forces enrollment |
| `ivalt-mfa-conditional` | `ConditionalIvaltAuthenticator` | CONDITIONAL — skips if user hasn't configured iVALT |

## Enrollment Flow (Required Action)

When a user needs to set up iVALT MFA:

```mermaid
sequenceDiagram
    participant User as User Browser
    participant KC as Keycloak
    participant IvaltAPI as iVALT API
    participant App as iVALT Mobile App

    User->>KC: Trigger CONFIGURE_IVALT required action
    KC->>User: Render ivalt-setup.ftl (country code + mobile input)
    User->>KC: Submit mobile number + country code
    KC->>KC: Validate input
    KC->>IvaltAPI: POST /send/global/notification (verification)
    IvaltAPI-->>App: Push notification
    IvaltAPI-->>KC: 200 OK
    KC->>User: Render ivalt-setup-verify.ftl (polling page)
    loop Every 10s (up to ~60s)
        User->>KC: POST (check verification)
        App->>IvaltAPI: User approves on device
        KC->>IvaltAPI: POST /validate-geo-fence-auth
        alt APPROVED
            IvaltAPI-->>KC: { status: "APPROVED" }
            KC->>KC: Save credential via IvaltCredentialProvider
            KC->>User: Enrollment complete → proceed
        else PENDING
            IvaltAPI-->>KC: { status: "PENDING" }
            KC->>User: Continue polling
        end
    end
    opt Timeout
        KC->>User: Show timeout error
    end
```

## Credential Storage

The `IvaltCredentialProvider` stores the mobile number and country code as a typed Keycloak credential:

- **Credential type:** `ivalt`
- **Stored data:** JSON with `mobileNumber` and `countryCode`
- **Provider ID:** `keycloak-ivalt`

```mermaid
graph LR
    subgraph KeycloakDB["Keycloak Database"]
        CRED_TABLE["credential table<br/>type='ivalt'"]
    end

    subgraph User["User"]
        MOBILE["Mobile: +919876543210<br/>Country: +91"]
    end

    User-- enrolled via -->ConfigureIvalt
    ConfigureIvalt-- saves -->CRED_TABLE
    IvaltAuthenticator-- reads -->CRED_TABLE
    CRED_TABLE-->MOBILE
```

## Key Files

| File | Role |
|------|------|
| `services/.../browser/IvaltAuthenticator.java` | Main authenticator — sends push, polls for status, renders `ivalt-auth.ftl` |
| `services/.../browser/IvaltAuthenticatorFactory.java` | Factory (provider ID: `ivalt-authenticator`) |
| `services/.../browser/ConditionalIvaltAuthenticator.java` | Conditional variant — checks credential before executing |
| `services/.../browser/ConditionalIvaltAuthenticatorFactory.java` | Factory (provider ID: `ivalt-mfa-conditional`) |
| `services/.../browser/IvaltApiClient.java` | Auth API client — `/send/global/notification`, `/validate-geo-fence-auth` |
| `services/.../requiredactions/ConfigureIvalt.java` | Required action — enrollment form + verification |
| `services/.../credential/IvaltCredentialProvider.java` | Credential provider — store/retrieve mobile number |
| `themes/.../login/ivalt-auth.ftl` | Auth waiting page (polls every 3s) |
| `themes/.../login/ivalt-setup.ftl` | Enrollment form (country code + mobile input) |
| `themes/.../login/ivalt-setup-verify.ftl` | Verification waiting page (polls every 10s) |
| `themes/.../login/messages/messages_en.properties` | i18n strings for all iVALT flows |
