import type KeycloakAdminClient from "@keycloak/keycloak-admin-client";

/**
 * Resolves a user's iVALT mobile number.
 *
 * Priority:
 * 1. iVALT credential (countryCode + mobileNumber from credentialData)
 * 2. User attribute `mobile_number` (fallback for users not yet enrolled)
 */
export async function getIvaltUserMobile(
  adminClient: KeycloakAdminClient,
  userId: string,
): Promise<string> {
  // 1. Try iVALT credential first
  const credentials = await adminClient.users.getCredentials({ id: userId });
  const ivalt = credentials.find((c) => c.type === "ivalt");
  if (ivalt?.credentialData) {
    try {
      const data = JSON.parse(ivalt.credentialData);
      const mobile = `${data.countryCode ?? ""}${data.mobileNumber ?? ""}`;
      if (mobile) return mobile;
    } catch {
      // ignore parse error, fall through to attribute
    }
  }

  // 2. Fall back to user attribute
  const user = await adminClient.users.findOne({ id: userId });
  return user?.attributes?.mobile_number?.[0] ?? "";
}
