import { useState, useEffect } from "react";
import { useAlerts } from "@keycloak/keycloak-ui-shared";
import { PageSection } from "@patternfly/react-core";
import { useTranslation } from "react-i18next";
import { useAdminClient } from "../admin-client";
import { KeycloakSpinner } from "@keycloak/keycloak-ui-shared";
import { useRealm } from "../context/realm-context/RealmContext";
import { useParams } from "../utils/useParams";
import { UserParams } from "./routes/User";
import { keyclockidpClient } from "../ivalt-settings/api/keyclockidpClient";

export default function UserTimeWindow() {
  const { adminClient } = useAdminClient();
  const { t } = useTranslation();
  const { addError } = useAlerts();
  const { id: userId } = useParams<UserParams>();
  const { realm: realmName } = useRealm();

  const [loading, setLoading] = useState(true);
  const [assignedTimeWindows, setAssignedTimeWindows] = useState<any[]>([]);
  const [userMobileNumber, setUserMobileNumber] = useState<string>("");

  useEffect(() => {
    const fetchData = async () => {
      try {
        setLoading(true);

        // Get user details to find mobile number
        const user = await adminClient.users.findOne({ id: userId! });
        if (user?.attributes) {
          const mobile = user.attributes.mobile_number?.[0] || "";
          setUserMobileNumber(mobile);
        }

        // Get assigned time windows for the user
        const timeWindowsResponse =
          await keyclockidpClient.getAssignedTimeWindows(userMobileNumber);
        if (timeWindowsResponse.success && timeWindowsResponse.data) {
          setAssignedTimeWindows(timeWindowsResponse.data);
        } else {
          setAssignedTimeWindows([]);
        }
      } catch (error) {
        addError("timeWindowFetchError", error);
        setAssignedTimeWindows([]);
      } finally {
        setLoading(false);
      }
    };

    void fetchData();
  }, [userId, realmName, adminClient, addError]);

  if (loading) {
    return <KeycloakSpinner />;
  }

  return (
    <PageSection>
      <h2>{t("userTimeWindowAssignments")}</h2>
      {/* Time window assignment UI will be implemented here */}
      <p>Assigned time windows: {assignedTimeWindows.length}</p>
      <p>User mobile number: {userMobileNumber || "Not set"}</p>
    </PageSection>
  );
}
