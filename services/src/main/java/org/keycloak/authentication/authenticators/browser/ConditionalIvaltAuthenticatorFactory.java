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
 * Factory for Conditional iVALT Authenticator
 * This authenticator only executes if the user has iVALT configured (self-service)
 */
public class ConditionalIvaltAuthenticatorFactory implements AuthenticatorFactory {

    public static final String PROVIDER_ID = "ivalt-mfa-conditional";

    @Override
    public String getDisplayType() {
        return "iVALT MFA (Self-Service)";
    }

    @Override
    public String getReferenceCategory() {
        return "mfa";
    }

    @Override
    public boolean isConfigurable() {
        return true;
    }

    @Override
    public AuthenticationExecutionModel.Requirement[] getRequirementChoices() {
        // Only allow CONDITIONAL and DISABLED
        // CONDITIONAL means: execute only if user has it configured
        return new AuthenticationExecutionModel.Requirement[]{
            AuthenticationExecutionModel.Requirement.CONDITIONAL,
            AuthenticationExecutionModel.Requirement.ALTERNATIVE,
            AuthenticationExecutionModel.Requirement.DISABLED
        };
    }

    @Override
    public boolean isUserSetupAllowed() {
        return true;
    }

    @Override
    public String getHelpText() {
        return "iVALT MFA authentication (self-service). Only executes if user has iVALT configured. Users can enable from Account Console.";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        List<ProviderConfigProperty> configProperties = new ArrayList<>();

        ProviderConfigProperty apiUrl = new ProviderConfigProperty();
        apiUrl.setName("api.base.url");
        apiUrl.setLabel("API Base URL");
        apiUrl.setType(ProviderConfigProperty.STRING_TYPE);
        apiUrl.setHelpText("Base URL for the iVALT API.");
        apiUrl.setDefaultValue("https://api.ivalt.com");
        configProperties.add(apiUrl);

        ProviderConfigProperty apiKey = new ProviderConfigProperty();
        apiKey.setName("api.key");
        apiKey.setLabel("API Key");
        apiKey.setType(ProviderConfigProperty.STRING_TYPE);
        apiKey.setHelpText("API Key for authenticating with the iVALT API.");
        configProperties.add(apiKey);

        ProviderConfigProperty apiTimeout = new ProviderConfigProperty();
        apiTimeout.setName("api.timeout");
        apiTimeout.setLabel("API Timeout (ms)");
        apiTimeout.setType(ProviderConfigProperty.STRING_TYPE);
        apiTimeout.setHelpText("Timeout for iVALT API requests in milliseconds.");
        apiTimeout.setDefaultValue("300000"); // 5 minutes
        configProperties.add(apiTimeout);

        ProviderConfigProperty pollInterval = new ProviderConfigProperty();
        pollInterval.setName("poll.interval");
        pollInterval.setLabel("Poll Interval (ms)");
        pollInterval.setType(ProviderConfigProperty.STRING_TYPE);
        pollInterval.setHelpText("Interval for polling iVALT API status in milliseconds.");
        pollInterval.setDefaultValue("2000"); // 2 seconds
        configProperties.add(pollInterval);

        return configProperties;
    }

    @Override
    public Authenticator create(KeycloakSession session) {
        return new ConditionalIvaltAuthenticator();
    }

    @Override
    public void init(Config.Scope config) {
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
    }

    @Override
    public void close() {
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }
}
