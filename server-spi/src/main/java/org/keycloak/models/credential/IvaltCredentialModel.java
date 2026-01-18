package org.keycloak.models.credential;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.keycloak.common.util.Time;
import org.keycloak.credential.CredentialModel;
import org.keycloak.util.JsonSerialization;

import java.io.IOException;

/**
 * Credential model for iVALT MFA
 * Stores mobile number and country code
 * 
 * @author iVALT Integration Team
 */
public class IvaltCredentialModel extends CredentialModel {

    public static final String TYPE = "ivalt";

    private final IvaltCredentialData credentialData;

    private IvaltCredentialModel(IvaltCredentialData credentialData) {
        this.credentialData = credentialData;
    }

    public static IvaltCredentialModel createFromCredentialModel(CredentialModel credentialModel) {
        try {
            IvaltCredentialData credentialData = JsonSerialization.readValue(
                    credentialModel.getCredentialData(),
                    IvaltCredentialData.class);

            IvaltCredentialModel ivaltModel = new IvaltCredentialModel(credentialData);
            ivaltModel.setUserLabel(credentialModel.getUserLabel());
            ivaltModel.setCreatedDate(credentialModel.getCreatedDate());
            ivaltModel.setType(TYPE);
            ivaltModel.setId(credentialModel.getId());
            ivaltModel.setSecretData(credentialModel.getSecretData());
            ivaltModel.setCredentialData(credentialModel.getCredentialData());

            return ivaltModel;
        } catch (IOException e) {
            throw new RuntimeException("Failed to deserialize iVALT credential data", e);
        }
    }

    public static IvaltCredentialModel create(String mobileNumber, String countryCode) {
        IvaltCredentialData credentialData = new IvaltCredentialData(mobileNumber, countryCode);
        IvaltCredentialModel credentialModel = new IvaltCredentialModel(credentialData);

        try {
            credentialModel.setCredentialData(JsonSerialization.writeValueAsString(credentialData));
            credentialModel.setType(TYPE);
            credentialModel.setCreatedDate(Time.currentTimeMillis());
        } catch (IOException e) {
            throw new RuntimeException("Failed to serialize iVALT credential data", e);
        }

        return credentialModel;
    }

    public String getMobileNumber() {
        return credentialData.getMobileNumber();
    }

    public String getCountryCode() {
        return credentialData.getCountryCode();
    }

    public IvaltCredentialData getIvaltCredentialData() {
        return credentialData;
    }

    /**
     * Data class for iVALT credential
     */
    public static class IvaltCredentialData {
        private final String mobileNumber;
        private final String countryCode;

        @JsonCreator
        public IvaltCredentialData(
                @JsonProperty("mobileNumber") String mobileNumber,
                @JsonProperty("countryCode") String countryCode) {
            this.mobileNumber = mobileNumber;
            this.countryCode = countryCode;
        }

        public IvaltCredentialData() {
            this(null, null);
        }

        public String getMobileNumber() {
            return mobileNumber;
        }

        public String getCountryCode() {
            return countryCode;
        }
    }
}
