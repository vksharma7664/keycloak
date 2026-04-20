import {
  PageSection,
  PageSectionVariants,
  Title,
} from "@patternfly/react-core";
import IvaultSettings from "./routes/IvaultSettings";

export default function IvaultSettingsSection() {
  return (
    <PageSection variant={PageSectionVariants.light}>
      <Title headingLevel="h1" size="lg">
        iVALT Settings
      </Title>
      <IvaultSettings />
    </PageSection>
  );
}
