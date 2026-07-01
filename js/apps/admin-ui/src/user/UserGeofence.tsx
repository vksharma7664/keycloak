import { useCallback, useEffect, useMemo, useState } from "react";
import { useTranslation } from "react-i18next";
import {
  Button,
  FormSelect,
  FormSelectOption,
  PageSection,
  Toolbar,
  ToolbarContent,
  ToolbarItem,
} from "@patternfly/react-core";
import { Table, Thead, Tbody, Tr, Th, Td } from "@patternfly/react-table";
import { KeycloakSpinner, useAlerts } from "@keycloak/keycloak-ui-shared";
import { useKeyclockidpClient } from "../ivalt-settings/api/keyclockidpClient";
import type { Geofence } from "../ivalt-settings/api/types";

function asArray<T>(data: unknown): T[] {
  if (Array.isArray(data)) return data as T[];
  if (
    data &&
    typeof data === "object" &&
    Array.isArray((data as Record<string, unknown>)["data"])
  ) {
    return (data as Record<string, unknown>)["data"] as T[];
  }
  return [];
}

export default function UserGeofence() {
  const { t } = useTranslation();
  const { addAlert, addError } = useAlerts();
  const keyclockidpClient = useKeyclockidpClient();

  const [loading, setLoading] = useState(true);
  const [assigned, setAssigned] = useState<Geofence[]>([]);
  const [available, setAvailable] = useState<Geofence[]>([]);
  const [selectedId, setSelectedId] = useState("");
  const [busy, setBusy] = useState(false);

  const refresh = useCallback(async () => {
    const [assignedRes, activeRes] = await Promise.all([
      keyclockidpClient.getUserGeofences(),
      keyclockidpClient.getGeofences(),
    ]);
    setAssigned(assignedRes.success ? asArray<Geofence>(assignedRes.data) : []);
    setAvailable(activeRes.success ? asArray<Geofence>(activeRes.data) : []);
  }, [keyclockidpClient]);

  useEffect(() => {
    const fetchData = async () => {
      setLoading(true);
      try {
        await refresh();
      } catch (error) {
        addError("geofenceFetchError", error);
      } finally {
        setLoading(false);
      }
    };
    void fetchData();
  }, [addError, refresh]);

  const assignableOptions = useMemo(() => {
    const assignedIds = new Set(assigned.map((g) => g.id));
    return available.filter((g) => !assignedIds.has(g.id));
  }, [assigned, available]);

  const handleAssign = async () => {
    if (!selectedId) return;
    setBusy(true);
    const newAssignedIds = [...assigned.map((g) => g.id), Number(selectedId)];
    const response = await keyclockidpClient.updateUserGeofences({
      orgGeoFence_ids: newAssignedIds,
    });
    if (response.success) {
      addAlert(t("geofenceAssigned"));
      setSelectedId("");
      await refresh();
    } else {
      addError("geofenceAssignError", response.error);
    }
    setBusy(false);
  };

  const handleRemove = async (geofenceId: number) => {
    setBusy(true);
    const newAssignedIds = assigned
      .filter((g) => g.id !== geofenceId)
      .map((g) => g.id);
    const response = await keyclockidpClient.updateUserGeofences({
      orgGeoFence_ids: newAssignedIds,
    });
    if (response.success) {
      addAlert(t("geofenceUnassigned"));
      await refresh();
    } else {
      addError("geofenceUnassignError", response.error);
    }
    setBusy(false);
  };

  if (loading) {
    return <KeycloakSpinner />;
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
