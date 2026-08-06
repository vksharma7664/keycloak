import { PageSection } from "@patternfly/react-core";
import { ViewHeader } from "../components/view-header/ViewHeader";
import TimeWindowList from "./timewindow/TimeWindowList";

export default function TimeWindowSection() {
  return (
    <>
      <ViewHeader titleKey="timeWindows" subKey="timeWindowsHelp" />
      <PageSection variant="light">
        <TimeWindowList />
      </PageSection>
    </>
  );
}
