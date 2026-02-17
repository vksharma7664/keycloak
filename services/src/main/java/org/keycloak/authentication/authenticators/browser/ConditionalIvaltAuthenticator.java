package org.keycloak.authentication.authenticators.browser;

import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.credential.CredentialProvider;
import org.keycloak.credential.IvaltCredentialProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.credential.IvaltCredentialModel;

/**
 * Conditional iVALT Authenticator - Only executes if user has iVALT configured
 * This makes iVALT work like TOTP - users can self-enable from Account Console
 */
public class ConditionalIvaltAuthenticator extends IvaltAuthenticator {

    @Override
    public void authenticate(AuthenticationFlowContext context) {
        // Check if user has iVALT configured
        if (!isIvaltConfiguredForUser(context)) {
            // User doesn't have iVALT - skip this authenticator
            context.attempted();
            return;
        }
        
        // User has iVALT configured - proceed with authentication
        super.authenticate(context);
    }

    /**
     * Check if user has iVALT credentials configured
     */
    private boolean isIvaltConfiguredForUser(AuthenticationFlowContext context) {
        KeycloakSession session = context.getSession();
        RealmModel realm = context.getRealm();
        UserModel user = context.getUser();
        
        if (user == null) {
            return false;
        }
        
        // Check if user has iVALT credential stored
        return user.credentialManager()
            .getStoredCredentialsByTypeStream(IvaltCredentialModel.TYPE)
            .findAny()
            .isPresent();
    }

    @Override
    public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) {
        // This authenticator is "configured" if the user has iVALT set up
        if (user == null) {
            return false;
        }
        
        return user.credentialManager()
            .getStoredCredentialsByTypeStream(IvaltCredentialModel.TYPE)
            .findAny()
            .isPresent();
    }
}
