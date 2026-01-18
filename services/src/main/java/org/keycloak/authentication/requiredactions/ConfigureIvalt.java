/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.authentication.requiredactions;

import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.authentication.InitiatedActionSupport;
import org.keycloak.authentication.RequiredActionContext;
import org.keycloak.authentication.RequiredActionFactory;
import org.keycloak.authentication.RequiredActionProvider;
import org.keycloak.authentication.authenticators.browser.IvaltApiClient;
import org.keycloak.credential.CredentialProvider;
import org.keycloak.credential.IvaltCredentialProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.UserModel;

import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import java.util.Map;

/**
 * Required action for configuring iVALT MFA
 * Handles mobile number entry and verification
 * 
 * @author iVALT Integration Team
 */
public class ConfigureIvalt implements RequiredActionProvider, RequiredActionFactory {

    private static final Logger logger = Logger.getLogger(ConfigureIvalt.class);

    public static final String PROVIDER_ID = "CONFIGURE_IVALT";
    private static final String IVALT_VERIFICATION_TRANSACTION_ID = "ivaltVerificationTransactionId";

    @Override
    public InitiatedActionSupport initiatedActionSupport() {
        return InitiatedActionSupport.SUPPORTED;
    }

    @Override
    public void evaluateTriggers(RequiredActionContext context) {
        // This is called to determine if the action should be triggered
        // We don't auto-trigger, user must explicitly enable iVALT
    }

    @Override
    public void requiredActionChallenge(RequiredActionContext context) {
        // Show the mobile number entry form
        Response challenge = context.form()
                .setAttribute("countryCodes", getCountryCodes())
                .createForm("ivalt-setup.ftl");
        context.challenge(challenge);
    }

    @Override
    public void processAction(RequiredActionContext context) {
        MultivaluedMap<String, String> formData = context.getHttpRequest().getDecodedFormParameters();
        String action = formData.getFirst("action");

        if ("cancel".equals(action)) {
            context.getUser().removeRequiredAction(PROVIDER_ID);
            context.success();
            return;
        }

        // Check if this is verification step FIRST (before reading mobile number)
        String transactionId = context.getAuthenticationSession().getAuthNote(IVALT_VERIFICATION_TRANSACTION_ID);

        if (transactionId == null) {
            // First step: Send verification notification - need mobile number
            String mobileNumber = formData.getFirst("mobileNumber");
            String countryCode = formData.getFirst("countryCode");

            // Validate input
            if (mobileNumber == null || mobileNumber.trim().isEmpty()) {
                Response challenge = context.form()
                        .setError("ivaltMobileNumberRequired")
                        .setAttribute("countryCodes", getCountryCodes())
                        .createForm("ivalt-setup.ftl");
                context.challenge(challenge);
                return;
            }

            if (countryCode == null || countryCode.trim().isEmpty()) {
                Response challenge = context.form()
                        .setError("ivaltCountryCodeRequired")
                        .setAttribute("countryCodes", getCountryCodes())
                        .createForm("ivalt-setup.ftl");
                context.challenge(challenge);
                return;
            }

            try {
                logger.infof("Looking for iVALT authenticator config for user %s", context.getUser().getUsername());

                Map<String, String> config = context.getRealm().getAuthenticatorConfigsStream()
                        .peek(c -> logger.infof("Found config: alias=%s, hasApiKey=%s",
                                c.getAlias(), c.getConfig().containsKey("ivalt.api.key")))
                        .filter(c -> c.getConfig().containsKey("ivalt.api.key"))
                        .findFirst()
                        .map(c -> c.getConfig())
                        .orElseThrow(() -> new RuntimeException("iVALT authenticator not configured with API key"));

                logger.infof("Found iVALT config with API key, creating API client");
                IvaltApiClient apiClient = new IvaltApiClient(config);

                String fullMobileNumber = countryCode + mobileNumber;
                logger.infof("Sending iVALT notification to %s", fullMobileNumber);

                String newTransactionId = apiClient.sendNotification(
                        fullMobileNumber,
                        context.getUser().getUsername(),
                        context.getRealm().getName());

                logger.infof("iVALT notification sent successfully, transaction ID: %s", newTransactionId);

                context.getAuthenticationSession().setAuthNote(IVALT_VERIFICATION_TRANSACTION_ID, newTransactionId);
                context.getAuthenticationSession().setAuthNote("ivaltVerificationStartTime", String.valueOf(System.currentTimeMillis()));
                context.getAuthenticationSession().setAuthNote("ivaltMobileNumber", mobileNumber);
                context.getAuthenticationSession().setAuthNote("ivaltCountryCode", countryCode);

                // Show verification waiting page
                Response challenge = context.form()
                        .setAttribute("mobileNumber", maskMobileNumber(countryCode + mobileNumber))
                        .createForm("ivalt-setup-verify.ftl");
                context.challenge(challenge);

            } catch (Exception e) {
                logger.errorf(e, "Failed to send iVALT verification for user %s: %s",
                        context.getUser().getUsername(), e.getMessage());
                Response challenge = context.form()
                        .setError("ivaltVerificationFailed")
                        .setAttribute("countryCodes", getCountryCodes())
                        .createForm("ivalt-setup.ftl");
                context.challenge(challenge);
            }
        } else {
            // Second step: Check verification status

            // Check if verification has timed out (1 minute = 60000ms)
            String startTimeStr = context.getAuthenticationSession().getAuthNote("ivaltVerificationStartTime");
            if (startTimeStr != null) {
                long startTime = Long.parseLong(startTimeStr);
                long elapsedTime = System.currentTimeMillis() - startTime;
                long timeoutMs = 60000; // 1 minute

                if (elapsedTime > timeoutMs) {
                    // Verification timed out
                    logger.warnf("iVALT verification timed out for user %s after %d ms",
                            context.getUser().getUsername(), elapsedTime);
                    context.getAuthenticationSession().removeAuthNote(IVALT_VERIFICATION_TRANSACTION_ID);
                    context.getAuthenticationSession().removeAuthNote("ivaltVerificationStartTime");
                    Response challenge = context.form()
                            .setError("ivaltTimeout")
                            .setAttribute("countryCodes", getCountryCodes())
                            .createForm("ivalt-setup.ftl");
                    context.challenge(challenge);
                    return;
                }
            }

            try {
                IvaltApiClient apiClient = new IvaltApiClient(
                        context.getRealm().getAuthenticatorConfigsStream()
                                .filter(config -> config.getConfig().containsKey("ivalt.api.key"))
                                .findFirst()
                                .map(config -> config.getConfig())
                                .orElseThrow(
                                        () -> new RuntimeException("iVALT authenticator not configured with API key")));

                IvaltApiClient.NotificationStatus status = apiClient.getStatus(transactionId);
                logger.infof("Verification status check for transaction %s: status=%s", transactionId, status);

                if (status == IvaltApiClient.NotificationStatus.APPROVED) {
                    // Verification successful, save credential
                    String savedMobileNumber = context.getAuthenticationSession().getAuthNote("ivaltMobileNumber");
                    String savedCountryCode = context.getAuthenticationSession().getAuthNote("ivaltCountryCode");

                    IvaltCredentialProvider credentialProvider = (IvaltCredentialProvider) context.getSession()
                            .getProvider(CredentialProvider.class, "keycloak-ivalt");

                    credentialProvider.createOrUpdateCredential(
                            context.getRealm(),
                            context.getUser(),
                            savedMobileNumber,
                            savedCountryCode);

                    logger.infof("iVALT configured successfully for user %s", context.getUser().getUsername());
                    context.getUser().removeRequiredAction(PROVIDER_ID);
                    context.success();

                } else if (status == IvaltApiClient.NotificationStatus.REJECTED) {
                    // Verification rejected
                    context.getAuthenticationSession().removeAuthNote(IVALT_VERIFICATION_TRANSACTION_ID);
                    Response challenge = context.form()
                            .setError("ivaltVerificationRejected")
                            .setAttribute("countryCodes", getCountryCodes())
                            .createForm("ivalt-setup.ftl");
                    context.challenge(challenge);

                } else if (status == IvaltApiClient.NotificationStatus.INVALID_TIMEZONE) {
                    // Invalid timezone error from iVALT API
                    logger.warnf("iVALT API returned INVALID_TIMEZONE for user %s", context.getUser().getUsername());
                    context.getAuthenticationSession().removeAuthNote(IVALT_VERIFICATION_TRANSACTION_ID);
                    Response challenge = context.form()
                            .setError("ivaltInvalidTimezone")
                            .setAttribute("countryCodes", getCountryCodes())
                            .createForm("ivalt-setup.ftl");
                    context.challenge(challenge);

                } else if (status == IvaltApiClient.NotificationStatus.INVALID_GEOFENCE) {
                    // Invalid geofence error from iVALT API
                    logger.warnf("iVALT API returned INVALID_GEOFENCE for user %s", context.getUser().getUsername());
                    context.getAuthenticationSession().removeAuthNote(IVALT_VERIFICATION_TRANSACTION_ID);
                    Response challenge = context.form()
                            .setError("ivaltInvalidGeofence")
                            .setAttribute("countryCodes", getCountryCodes())
                            .createForm("ivalt-setup.ftl");
                    context.challenge(challenge);

                } else {
                    // Still pending, show waiting page again
                    Response challenge = context.form()
                            .setAttribute("mobileNumber", maskMobileNumber(
                                    context.getAuthenticationSession().getAuthNote("ivaltCountryCode") +
                                            context.getAuthenticationSession().getAuthNote("ivaltMobileNumber")))
                            .createForm("ivalt-setup-verify.ftl");
                    context.challenge(challenge);
                }

            } catch (Exception e) {
                logger.errorf(e, "Failed to verify iVALT for user %s", context.getUser().getUsername());
                context.getAuthenticationSession().removeAuthNote(IVALT_VERIFICATION_TRANSACTION_ID);
                Response challenge = context.form()
                        .setError("ivaltVerificationFailed")
                        .setAttribute("countryCodes", getCountryCodes())
                        .createForm("ivalt-setup.ftl");
                context.challenge(challenge);
            }
        }
    }

    @Override
    public void close() {
        // No cleanup needed
    }

    @Override
    public RequiredActionProvider create(KeycloakSession session) {
        return this;
    }

    @Override
    public void init(Config.Scope config) {
        // No initialization needed
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        // No post-initialization needed
    }

    @Override
    public String getDisplayText() {
        return "Configure iVALT MFA";
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public boolean isOneTimeAction() {
        return true;
    }

    /**
     * Get list of common country codes
     */
    private String[] getCountryCodes() {
        return new String[] {
                "+1:United States/Canada",
                "+44:United Kingdom",
                "+91:India",
                "+86:China",
                "+81:Japan",
                "+49:Germany",
                "+33:France",
                "+39:Italy",
                "+34:Spain",
                "+61:Australia",
                "+55:Brazil",
                "+7:Russia",
                "+82:South Korea",
                "+52:Mexico",
                "+31:Netherlands",
                "+46:Sweden",
                "+47:Norway",
                "+45:Denmark",
                "+41:Switzerland",
                "+65:Singapore"
        };
    }

    /**
     * Mask mobile number for display
     */
    private String maskMobileNumber(String fullNumber) {
        if (fullNumber == null || fullNumber.length() < 4) {
            return "****";
        }
        int length = fullNumber.length();
        return "****" + fullNumber.substring(length - 4);
    }
}
