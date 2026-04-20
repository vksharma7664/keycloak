import { useState } from "react";
import { Tabs, Tab, TabTitleText } from "@patternfly/react-core";
import GeofenceTab from "./GeofenceTab";
import TimeWindowTab from "./TimeWindowTab";

export default function IvaultSettings() {
  const [activeTabKey, setActiveTabKey] = useState(0);

  const handleTabClick = (
    event: React.MouseEvent,
    eventKey: string | number,
  ) => {
    setActiveTabKey(
      typeof eventKey === "number" ? eventKey : parseInt(eventKey, 10),
    );
  };

  return (
    <Tabs activeKey={activeTabKey} onSelect={handleTabClick}>
      <Tab
        eventKey={0}
        title={<TabTitleText>Geofence Management</TabTitleText>}
      >
        {activeTabKey === 0 && <GeofenceTab />}
      </Tab>
      <Tab
        eventKey={1}
        title={<TabTitleText>Time Window Management</TabTitleText>}
      >
        {activeTabKey === 1 && <TimeWindowTab />}
      </Tab>
    </Tabs>
  );
}
