import type KeycloakAdminClient from "@keycloak/keycloak-admin-client";

/**
 * Resolves a user's iVALT mobile number (country code + mobile) from their
 * iVALT credential. The mobile is stored in the credential's credentialData,
 * not as a user attribute.
 */
export async function getIvaltUserMobile(
  adminClient: KeycloakAdminClient,
  userId: string,
): Promise<string> {
  const credentials = await adminClient.users.getCredentials({ id: userId });
  const ivalt = credentials.find((c) => c.type === "ivalt");
  if (!ivalt?.credentialData) {
    return "";
  }
  try {
    const data = JSON.parse(ivalt.credentialData);
    return `${data.countryCode ?? ""}${data.mobileNumber ?? ""}`;
  } catch {
    return "";
  }
}
