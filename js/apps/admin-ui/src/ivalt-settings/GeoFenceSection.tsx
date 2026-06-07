import { PageSection } from "@patternfly/react-core";
import { ViewHeader } from "../components/view-header/ViewHeader";
import GeofenceList from "./geofence/GeofenceList";

export default function GeoFenceSection() {
  return (
    <>
      <ViewHeader titleKey="geoFences" subKey="geoFencesHelp" />
      <PageSection variant="light">
        <GeofenceList />
      </PageSection>
    </>
  );
}
