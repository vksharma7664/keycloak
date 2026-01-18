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

package org.keycloak.credential;

import org.jboss.logging.Logger;
import org.keycloak.common.util.Time;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.credential.IvaltCredentialModel;

/**
 * Credential provider for iVALT MFA
 * Manages storage and retrieval of mobile numbers
 * 
 * @author iVALT Integration Team
 */
public class IvaltCredentialProvider implements CredentialProvider<IvaltCredentialModel> {

    private static final Logger logger = Logger.getLogger(IvaltCredentialProvider.class);

    protected KeycloakSession session;

    public IvaltCredentialProvider(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public CredentialModel createCredential(RealmModel realm, UserModel user, IvaltCredentialModel credentialModel) {
        if (credentialModel.getCreatedDate() == null) {
            credentialModel.setCreatedDate(Time.currentTimeMillis());
        }
        return user.credentialManager().createStoredCredential(credentialModel);
    }

    @Override
    public boolean deleteCredential(RealmModel realm, UserModel user, String credentialId) {
        return user.credentialManager().removeStoredCredentialById(credentialId);
    }

    @Override
    public IvaltCredentialModel getCredentialFromModel(CredentialModel model) {
        return IvaltCredentialModel.createFromCredentialModel(model);
    }

    @Override
    public boolean supportsCredentialType(String credentialType) {
        return IvaltCredentialModel.TYPE.equals(credentialType);
    }

    public boolean isConfiguredFor(RealmModel realm, UserModel user, String credentialType) {
        if (!supportsCredentialType(credentialType))
            return false;
        return user.credentialManager().getStoredCredentialsByTypeStream(credentialType).findAny().isPresent();
    }

    @Override
    public String getType() {
        return IvaltCredentialModel.TYPE;
    }

    @Override
    public CredentialTypeMetadata getCredentialTypeMetadata(CredentialTypeMetadataContext metadataContext) {
        return CredentialTypeMetadata.builder()
                .type(getType())
                .category(CredentialTypeMetadata.Category.TWO_FACTOR)
                .displayName("iVALT Authenticator")
                .helpText("Biometric authentication using your mobile device with iVALT app")
                .iconCssClass("kcAuthenticatorIvaltClass")
                .createAction("CONFIGURE_IVALT")
                .removeable(true)
                .build(session);
    }

    /**
     * Get the mobile number for a user
     */
    public String getMobileNumber(RealmModel realm, UserModel user) {
        return user.credentialManager().getStoredCredentialsByTypeStream(IvaltCredentialModel.TYPE)
                .findFirst()
                .map(this::getCredentialFromModel)
                .map(IvaltCredentialModel::getMobileNumber)
                .orElse(null);
    }

    /**
     * Get the country code for a user
     */
    public String getCountryCode(RealmModel realm, UserModel user) {
        return user.credentialManager().getStoredCredentialsByTypeStream(IvaltCredentialModel.TYPE)
                .findFirst()
                .map(this::getCredentialFromModel)
                .map(IvaltCredentialModel::getCountryCode)
                .orElse(null);
    }

    /**
     * Create or update iVALT credential with mobile number and country code
     */
    public void createOrUpdateCredential(RealmModel realm, UserModel user, String mobileNumber, String countryCode) {
        // Remove existing credential if any
        user.credentialManager().getStoredCredentialsByTypeStream(IvaltCredentialModel.TYPE)
                .forEach(credential -> deleteCredential(realm, user, credential.getId()));

        // Create new credential
        IvaltCredentialModel credentialModel = IvaltCredentialModel.create(mobileNumber, countryCode);
        credentialModel.setUserLabel("iVALT Authenticator");
        createCredential(realm, user, credentialModel);

        logger.infof("Created iVALT credential for user %s with mobile %s%s",
                user.getUsername(), countryCode, mobileNumber);
    }
}
