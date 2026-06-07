import { useEffect, useMemo, useState } from "react";
import { useTranslation } from "react-i18next";
import {
  Button,
  EmptyState,
  EmptyStateBody,
  FormSelect,
  FormSelectOption,
  PageSection,
  Toolbar,
  ToolbarContent,
  ToolbarItem,
} from "@patternfly/react-core";
import { Table, Thead, Tbody, Tr, Th, Td } from "@patternfly/react-table";
import { KeycloakSpinner, useAlerts } from "@keycloak/keycloak-ui-shared";
import { useAdminClient } from "../admin-client";
import { useParams } from "../utils/useParams";
import { UserParams } from "./routes/User";
import { useKeyclockidpClient } from "../ivalt-settings/api/keyclockidpClient";
import { getIvaltUserMobile } from "../ivalt-settings/api/userMobile";
import { Geofence } from "../ivalt-settings/api/types";

function asArray<T>(data: any): T[] {
  if (Array.isArray(data)) return data;
  if (Array.isArray(data?.data)) return data.data;
  return [];
}

export default function UserGeofence() {
  const { adminClient } = useAdminClient();
  const { t } = useTranslation();
  const { addAlert, addError } = useAlerts();
  const { id: userId } = useParams<UserParams>();
  const keyclockidpClient = useKeyclockidpClient();

  const [loading, setLoading] = useState(true);
  const [userMobile, setUserMobile] = useState("");
  const [assigned, setAssigned] = useState<Geofence[]>([]);
  const [available, setAvailable] = useState<Geofence[]>([]);
  const [selectedId, setSelectedId] = useState("");
  const [busy, setBusy] = useState(false);

  const refresh = async (mobile: string) => {
    const [assignedRes, activeRes] = await Promise.all([
      keyclockidpClient.getAssignedGeofences(mobile),
      keyclockidpClient.getActiveGeofences(100, 0),
    ]);
    setAssigned(assignedRes.success ? asArray<Geofence>(assignedRes.data) : []);
    setAvailable(activeRes.success ? asArray<Geofence>(activeRes.data) : []);
  };

  useEffect(() => {
    const fetchData = async () => {
      setLoading(true);
      try {
        const mobile = await getIvaltUserMobile(adminClient, userId!);
        setUserMobile(mobile);
        if (mobile) {
          await refresh(mobile);
        }
      } catch (error) {
        addError("geofenceFetchError", error);
      } finally {
        setLoading(false);
      }
    };
    void fetchData();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [userId]);

  const assignableOptions = useMemo(() => {
    const assignedIds = new Set(assigned.map((g) => g.id));
    return available.filter((g) => !assignedIds.has(g.id));
  }, [assigned, available]);

  const handleAssign = async () => {
    if (!selectedId) return;
    setBusy(true);
    const response = await keyclockidpClient.assignGeofence({
      user_mobile: userMobile,
      geofence_id: Number(selectedId),
    });
    if (response.success) {
      addAlert(t("geofenceAssigned"));
      setSelectedId("");
      await refresh(userMobile);
    } else {
      addError("geofenceAssignError", response.error);
    }
    setBusy(false);
  };

  const handleRemove = async (geofenceId: number) => {
    setBusy(true);
    const response = await keyclockidpClient.removeGeofenceAssignment({
      user_mobile: userMobile,
      geofence_id: geofenceId,
    });
    if (response.success) {
      addAlert(t("geofenceUnassigned"));
      await refresh(userMobile);
    } else {
      addError("geofenceUnassignError", response.error);
    }
    setBusy(false);
  };

  if (loading) {
    return <KeycloakSpinner />;
  }

  if (!userMobile) {
    return (
      <PageSection variant="light">
        <EmptyState>
          <EmptyStateBody>{t("ivaltNoUserMobile")}</EmptyStateBody>
        </EmptyState>
      </PageSection>
    );
  }

  return (
    <PageSection variant="light">
      <Toolbar>
        <ToolbarContent>
          <ToolbarItem>
            <FormSelect
              value={selectedId}
              onChange={(_, value) => setSelectedId(value)}
              aria-label={t("assignGeofence")}
              style={{ width: "300px" }}
              isDisabled={busy || assignableOptions.length === 0}
            >
              <FormSelectOption
                value=""
                label={t("selectGeofence")}
                isPlaceholder
              />
              {assignableOptions.map((g) => (
                <FormSelectOption key={g.id} value={g.id} label={g.name} />
              ))}
            </FormSelect>
          </ToolbarItem>
          <ToolbarItem>
            <Button
              variant="primary"
              onClick={handleAssign}
              isDisabled={!selectedId || busy}
            >
              {t("assignGeofence")}
            </Button>
          </ToolbarItem>
        </ToolbarContent>
      </Toolbar>
      <Table aria-label={t("userGeofenceAssignments")} variant="compact">
        <Thead>
          <Tr>
            <Th>{t("name")}</Th>
            <Th>{t("status")}</Th>
            <Th aria-label={t("actions")} />
          </Tr>
        </Thead>
        <Tbody>
          {assigned.length === 0 ? (
            <Tr>
              <Td colSpan={3}>{t("noGeofencesAssigned")}</Td>
            </Tr>
          ) : (
            assigned.map((g) => (
              <Tr key={g.id}>
                <Td>{g.name}</Td>
                <Td>{g.is_active ? t("active") : t("inactive")}</Td>
                <Td isActionCell>
                  <Button
                    variant="link"
                    isDanger
                    onClick={() => handleRemove(g.id)}
                    isDisabled={busy}
                  >
                    {t("remove")}
                  </Button>
                </Td>
              </Tr>
            ))
          )}
        </Tbody>
      </Table>
    </PageSection>
  );
}
