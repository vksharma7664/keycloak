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

import org.keycloak.Config;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticatorFactory;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;

import java.util.ArrayList;
import java.util.List;

/**
 * Factory for iVALT MFA Authenticator
 * 
 * @author iVALT Integration Team
 */
public class IvaltAuthenticatorFactory implements AuthenticatorFactory {

    public static final String PROVIDER_ID = "ivalt-authenticator";
    private static final IvaltAuthenticator SINGLETON = new IvaltAuthenticator();

    public static final String IVALT_API_BASE_URL = "ivalt.api.base.url";
    public static final String IVALT_API_KEY = "ivalt.api.key";
    public static final String IVALT_API_TIMEOUT = "ivalt.api.timeout";
    public static final String IVALT_POLL_INTERVAL = "ivalt.poll.interval";

    @Override
    public String getDisplayType() {
        return "iVALT MFA";
    }

    @Override
    public String getReferenceCategory() {
        return "ivalt";
    }

    @Override
    public boolean isConfigurable() {
        return true;
    }

    @Override
    public AuthenticationExecutionModel.Requirement[] getRequirementChoices() {
        return REQUIREMENT_CHOICES;
    }

    @Override
    public boolean isUserSetupAllowed() {
        return true;
    }

    @Override
    public String getHelpText() {
        return "Validates user with iVALT push notification MFA.";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        List<ProviderConfigProperty> configProperties = new ArrayList<>();

        ProviderConfigProperty baseUrl = new ProviderConfigProperty();
        baseUrl.setType(ProviderConfigProperty.STRING_TYPE);
        baseUrl.setName(IVALT_API_BASE_URL);
        baseUrl.setLabel("iVALT API Base URL");
        baseUrl.setDefaultValue("https://api.ivalt.com");
        baseUrl.setHelpText("Base URL for iVALT API");
        configProperties.add(baseUrl);

        ProviderConfigProperty apiKey = new ProviderConfigProperty();
        apiKey.setType(ProviderConfigProperty.PASSWORD);
        apiKey.setName(IVALT_API_KEY);
        apiKey.setLabel("API Key (x-api-key)");
        apiKey.setHelpText("API key for authenticating with iVALT service");
        apiKey.setSecret(true);
        configProperties.add(apiKey);

        ProviderConfigProperty timeout = new ProviderConfigProperty();
        timeout.setType(ProviderConfigProperty.STRING_TYPE);
        timeout.setName(IVALT_API_TIMEOUT);
        timeout.setLabel("API Timeout (ms)");
        timeout.setDefaultValue("300000");
        timeout.setHelpText("Timeout for API calls in milliseconds (default: 5 minutes)");
        configProperties.add(timeout);

        ProviderConfigProperty pollInterval = new ProviderConfigProperty();
        pollInterval.setType(ProviderConfigProperty.STRING_TYPE);
        pollInterval.setName(IVALT_POLL_INTERVAL);
        pollInterval.setLabel("Poll Interval (ms)");
        pollInterval.setDefaultValue("2000");
        pollInterval.setHelpText("Interval for polling status in milliseconds");
        configProperties.add(pollInterval);

        return configProperties;
    }

    @Override
    public Authenticator create(KeycloakSession session) {
        return SINGLETON;
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
    public void close() {
        // No cleanup needed
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }
}
