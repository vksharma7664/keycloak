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

package org.keycloak.authentication.authenticators.browser;

import org.jboss.logging.Logger;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.CredentialValidator;
import org.keycloak.credential.CredentialProvider;
import org.keycloak.credential.IvaltCredentialProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.credential.IvaltCredentialModel;
import org.keycloak.models.credential.OTPCredentialModel;
import org.keycloak.sessions.AuthenticationSessionModel;

import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import java.util.Map;

/**
 * iVALT MFA Authenticator
 * Handles authentication using iVALT push notifications
 * 
 * @author iVALT Integration Team
 */
public class IvaltAuthenticator implements Authenticator, CredentialValidator<IvaltCredentialProvider> {

    private static final Logger logger = Logger.getLogger(IvaltAuthenticator.class);

    public static final String IVALT_TRANSACTION_ID = "ivaltTransactionId";
    public static final String IVALT_POLL_COUNT = "ivaltPollCount";
    public static final int MAX_POLL_ATTEMPTS = 30; // 30 attempts * 2 seconds = 60 seconds max

    @Override
    public void authenticate(AuthenticationFlowContext context) {
        UserModel user = context.getUser();

        if (!configuredFor(context.getSession(), context.getRealm(), user)) {
            // User doesn't have iVALT configured, skip or require setup
            if (context.getExecution().isRequired()) {
                context.getAuthenticationSession().addRequiredAction("CONFIGURE_IVALT");
            }
            context.attempted();
            return;
        }

        // Get iVALT credential (mobile number)
        IvaltCredentialProvider credentialProvider = getCredentialProvider(context.getSession());
        String mobileNumber = credentialProvider.getMobileNumber(context.getRealm(), user);
        String countryCode = credentialProvider.getCountryCode(context.getRealm(), user);

        if (mobileNumber == null || countryCode == null) {
            logger.errorf("iVALT credential not found for user %s", user.getUsername());
            context.failureChallenge(AuthenticationFlowError.CREDENTIAL_SETUP_REQUIRED,
                    context.form().setError("ivaltNotConfigured").createErrorPage(Response.Status.UNAUTHORIZED));
            return;
        }

        // Send notification via iVALT API
        try {
            Map<String, String> config = context.getAuthenticatorConfig().getConfig();
            IvaltApiClient apiClient = new IvaltApiClient(config);

            String transactionId = apiClient.sendNotification(
                    countryCode + mobileNumber,
                    user.getUsername(),
                    context.getRealm().getName());

            // Store transaction ID in authentication session
            context.getAuthenticationSession().setAuthNote(IVALT_TRANSACTION_ID, transactionId);
            context.getAuthenticationSession().setAuthNote(IVALT_POLL_COUNT, "0");

            // Show waiting page
            Response challenge = context.form()
                    .setAttribute("username", user.getUsername())
                    .setAttribute("mobileNumber", maskMobileNumber(countryCode + mobileNumber))
                    .createForm("ivalt-auth.ftl");
            context.challenge(challenge);

        } catch (Exception e) {
            logger.errorf(e, "Failed to send iVALT notification for user %s", user.getUsername());
            context.failureChallenge(AuthenticationFlowError.INTERNAL_ERROR,
                    context.form().setError("ivaltSendFailed").createErrorPage(Response.Status.INTERNAL_SERVER_ERROR));
        }
    }

    @Override
    public void action(AuthenticationFlowContext context) {
        // This is called when user submits the form or when polling for status
        MultivaluedMap<String, String> formData = context.getHttpRequest().getDecodedFormParameters();
        String action = formData.getFirst("action");

        if ("cancel".equals(action)) {
            context.resetFlow();
            return;
        }

        // Check status via API
        String transactionId = context.getAuthenticationSession().getAuthNote(IVALT_TRANSACTION_ID);
        if (transactionId == null) {
            logger.error("Transaction ID not found in authentication session");
            context.failureChallenge(AuthenticationFlowError.INTERNAL_ERROR,
                    context.form().setError("ivaltInternalError")
                            .createErrorPage(Response.Status.INTERNAL_SERVER_ERROR));
            return;
        }

        try {
            Map<String, String> config = context.getAuthenticatorConfig().getConfig();
            IvaltApiClient apiClient = new IvaltApiClient(config);

            IvaltApiClient.NotificationStatus status = apiClient.getStatus(transactionId);

            switch (status) {
                case APPROVED:
                    logger.infof("iVALT authentication approved for user %s", context.getUser().getUsername());
                    context.success();
                    break;

                case REJECTED:
                    logger.warnf("iVALT authentication rejected for user %s", context.getUser().getUsername());
                    context.failureChallenge(AuthenticationFlowError.INVALID_CREDENTIALS,
                            context.form().setError("ivaltRejected").createErrorPage(Response.Status.UNAUTHORIZED));
                    break;

                case INVALID_TIMEZONE:
                    logger.warnf("iVALT authentication failed - Invalid timezone for user %s",
                            context.getUser().getUsername());
                    context.failureChallenge(AuthenticationFlowError.INVALID_CREDENTIALS,
                            context.form().setError("ivaltInvalidTimezone")
                                    .createErrorPage(Response.Status.UNAUTHORIZED));
                    break;

                case INVALID_GEOFENCE:
                    logger.warnf("iVALT authentication failed - Invalid geofence for user %s",
                            context.getUser().getUsername());
                    context.failureChallenge(AuthenticationFlowError.INVALID_CREDENTIALS,
                            context.form().setError("ivaltInvalidGeofence")
                                    .createErrorPage(Response.Status.UNAUTHORIZED));
                    break;

                case PENDING:
                    // Increment poll count
                    int pollCount = Integer.parseInt(context.getAuthenticationSession().getAuthNote(IVALT_POLL_COUNT));
                    pollCount++;

                    if (pollCount >= MAX_POLL_ATTEMPTS) {
                        logger.warnf("iVALT authentication timeout for user %s", context.getUser().getUsername());
                        context.failureChallenge(AuthenticationFlowError.EXPIRED_CODE,
                                context.form().setError("ivaltTimeout").createErrorPage(Response.Status.UNAUTHORIZED));
                    } else {
                        context.getAuthenticationSession().setAuthNote(IVALT_POLL_COUNT, String.valueOf(pollCount));
                        // Continue showing waiting page
                        Response challenge = context.form()
                                .setAttribute("username", context.getUser().getUsername())
                                .setAttribute("mobileNumber", maskMobileNumber(
                                        getCredentialProvider(context.getSession())
                                                .getCountryCode(context.getRealm(), context.getUser()) +
                                                getCredentialProvider(context.getSession())
                                                        .getMobileNumber(context.getRealm(), context.getUser())))
                                .createForm("ivalt-auth.ftl");
                        context.challenge(challenge);
                    }
                    break;

                default:
                    logger.errorf("Unknown iVALT status: %s", status);
                    context.failureChallenge(AuthenticationFlowError.INTERNAL_ERROR,
                            context.form().setError("ivaltInternalError")
                                    .createErrorPage(Response.Status.INTERNAL_SERVER_ERROR));
            }

        } catch (Exception e) {
            logger.errorf(e, "Failed to check iVALT status for user %s", context.getUser().getUsername());
            context.failureChallenge(AuthenticationFlowError.INTERNAL_ERROR,
                    context.form().setError("ivaltStatusFailed")
                            .createErrorPage(Response.Status.INTERNAL_SERVER_ERROR));
        }
    }

    @Override
    public boolean requiresUser() {
        return true;
    }

    @Override
    public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) {
        return getCredentialProvider(session).isConfiguredFor(realm, user, IvaltCredentialModel.TYPE);
    }

    @Override
    public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user) {
        // Add required action to configure iVALT if not already configured
        if (!configuredFor(session, realm, user)) {
            user.addRequiredAction("CONFIGURE_IVALT");
        }
    }

    @Override
    public void close() {
        // No cleanup needed
    }

    @Override
    public IvaltCredentialProvider getCredentialProvider(KeycloakSession session) {
        return (IvaltCredentialProvider) session.getProvider(CredentialProvider.class, "keycloak-ivalt");
    }

    /**
     * Mask mobile number for display (show only last 4 digits)
     */
    private String maskMobileNumber(String fullNumber) {
        if (fullNumber == null || fullNumber.length() < 4) {
            return "****";
        }
        int length = fullNumber.length();
        return "****" + fullNumber.substring(length - 4);
    }
}
