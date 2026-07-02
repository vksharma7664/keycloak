import { useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import {
  ActionGroup,
  Button,
  Form,
  FormGroup,
  PageSection,
  TextInput,
} from "@patternfly/react-core";
import {
  KeycloakSpinner,
  HelpItem,
  useAlerts,
} from "@keycloak/keycloak-ui-shared";
import { ViewHeader } from "../components/view-header/ViewHeader";
import { useKeyclockidpClient } from "./api/keyclockidpClient";

export default function IvaltConfigSection() {
  const { t } = useTranslation();
  const { addAlert, addError } = useAlerts();
  const keyclockidpClient = useKeyclockidpClient();

  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [orgCode, setOrgCode] = useState("");
  const [userMobile, setUserMobile] = useState("");
  const [apiBaseUrl, setApiBaseUrl] = useState("");
  const [apiKey, setApiKey] = useState("");
  const [apiKeyConfigured, setApiKeyConfigured] = useState(false);
  const [googleMapsApiKey, setGoogleMapsApiKey] = useState("");

  const loadConfig = async () => {
    setLoading(true);
    const response = await keyclockidpClient.getConfig();
    if (response.success && response.data) {
      setOrgCode(response.data.orgCode);
      setUserMobile(response.data.userMobile);
      setApiBaseUrl(response.data.apiBaseUrl);
      setApiKeyConfigured(response.data.apiKeyConfigured);
      setGoogleMapsApiKey(response.data.googleMapsApiKey || "");
    } else {
      addError("ivaltConfigLoadError", response.error);
    }
    setLoading(false);
  };

  useEffect(() => {
    void loadConfig();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const handleSave = async () => {
    setSaving(true);
    const response = await keyclockidpClient.updateConfig({
      orgCode,
      userMobile,
      apiBaseUrl,
      // Only send the key when the admin entered a new value.
      ...(apiKey ? { apiKey } : {}),
      googleMapsApiKey,
    });
    if (response.success && response.data) {
      addAlert(t("ivaltConfigSaved"));
      setApiKey("");
      setApiKeyConfigured(response.data.apiKeyConfigured);
    } else {
      addError("ivaltConfigSaveError", response.error);
    }
    setSaving(false);
  };

  return (
    <>
      <ViewHeader titleKey="ivaltSettings" subKey="ivaltSettingsHelp" />
      <PageSection variant="light">
        {loading ? (
          <KeycloakSpinner />
        ) : (
          <Form isHorizontal onSubmit={(e) => e.preventDefault()}>
            <FormGroup
              label={t("ivaltOrgCode")}
              fieldId="ivalt-org-code"
              isRequired
              labelIcon={
                <HelpItem
                  helpText={t("ivaltOrgCodeHelp")}
                  fieldLabelId="ivaltOrgCode"
                />
              }
            >
              <TextInput
                id="ivalt-org-code"
                value={orgCode}
                placeholder="e.g. iVALT"
                onChange={(_, value) => setOrgCode(value)}
              />
            </FormGroup>
            <FormGroup
              label={t("ivaltUserMobile")}
              fieldId="ivalt-user-mobile"
              isRequired
              labelIcon={
                <HelpItem
                  helpText={t("ivaltUserMobileHelp")}
                  fieldLabelId="ivaltUserMobile"
                />
              }
            >
              <TextInput
                id="ivalt-user-mobile"
                value={userMobile}
                placeholder="e.g. +91..."
                onChange={(_, value) => setUserMobile(value)}
              />
            </FormGroup>
            <FormGroup
              label={t("ivaltApiBaseUrl")}
              fieldId="ivalt-api-base-url"
              labelIcon={
                <HelpItem
                  helpText={t("ivaltApiBaseUrlHelp")}
                  fieldLabelId="ivaltApiBaseUrl"
                />
              }
            >
              <TextInput
                id="ivalt-api-base-url"
                value={apiBaseUrl}
                placeholder="https://api.ivalt.com"
                onChange={(_, value) => setApiBaseUrl(value)}
              />
            </FormGroup>
            <FormGroup
              label={t("ivaltApiKey")}
              fieldId="ivalt-api-key"
              labelIcon={
                <HelpItem
                  helpText={t("ivaltApiKeyHelp")}
                  fieldLabelId="ivaltApiKey"
                />
              }
            >
              <TextInput
                id="ivalt-api-key"
                type="password"
                value={apiKey}
                placeholder={
                  apiKeyConfigured
                    ? t("ivaltApiKeyConfigured")
                    : t("ivaltApiKeyNotConfigured")
                }
                onChange={(_, value) => setApiKey(value)}
              />
            </FormGroup>
            <FormGroup
              label={t("ivaltGoogleMapsApiKey")}
              fieldId="ivalt-google-maps-api-key"
              labelIcon={
                <HelpItem
                  helpText={t("ivaltGoogleMapsApiKeyHelp")}
                  fieldLabelId="ivaltGoogleMapsApiKey"
                />
              }
            >
              <TextInput
                id="ivalt-google-maps-api-key"
                type="password"
                value={googleMapsApiKey}
                placeholder={t("ivaltGoogleMapsApiKeyPlaceholder")}
                onChange={(_, value) => setGoogleMapsApiKey(value)}
              />
            </FormGroup>
            <ActionGroup>
              <Button
                variant="primary"
                onClick={handleSave}
                isLoading={saving}
                isDisabled={saving}
              >
                {t("save")}
              </Button>
              <Button
                variant="link"
                onClick={() => void loadConfig()}
                isDisabled={saving}
              >
                {t("reload")}
              </Button>
            </ActionGroup>
          </Form>
        )}
      </PageSection>
    </>
  );
}
