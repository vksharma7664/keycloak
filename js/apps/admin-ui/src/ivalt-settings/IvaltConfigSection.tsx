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
  const [orgMobile, setOrgMobile] = useState("");
  const [apiBaseUrl, setApiBaseUrl] = useState("");
  const [apiKey, setApiKey] = useState("");
  const [apiKeyConfigured, setApiKeyConfigured] = useState(false);

  const loadConfig = async () => {
    setLoading(true);
    const response = await keyclockidpClient.getConfig();
    if (response.success && response.data) {
      setOrgMobile(response.data.orgMobile);
      setApiBaseUrl(response.data.apiBaseUrl);
      setApiKeyConfigured(response.data.apiKeyConfigured);
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
      orgMobile,
      apiBaseUrl,
      // Only send the key when the admin entered a new value.
      ...(apiKey ? { apiKey } : {}),
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
              label={t("ivaltOrgMobile")}
              fieldId="ivalt-org-mobile"
              isRequired
              labelIcon={
                <HelpItem
                  helpText={t("ivaltOrgMobileHelp")}
                  fieldLabelId="ivaltOrgMobile"
                />
              }
            >
              <TextInput
                id="ivalt-org-mobile"
                value={orgMobile}
                placeholder="+1..."
                onChange={(_, value) => setOrgMobile(value)}
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
