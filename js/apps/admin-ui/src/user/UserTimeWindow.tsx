import { useCallback, useEffect, useMemo, useState } from "react";
import { useTranslation } from "react-i18next";
import {
  Button,
  ButtonVariant,
  FormSelect,
  FormSelectOption,
  PageSection,
  Toolbar,
  ToolbarContent,
  ToolbarItem,
} from "@patternfly/react-core";
import { Table, Thead, Tbody, Tr, Th, Td } from "@patternfly/react-table";
import { KeycloakSpinner, useAlerts } from "@keycloak/keycloak-ui-shared";
import { useConfirmDialog } from "../components/confirm-dialog/ConfirmDialog";
import { useKeyclockidpClient } from "../ivalt-settings/api/keyclockidpClient";
import type { TimeWindow } from "../ivalt-settings/api/types";

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

export default function UserTimeWindow() {
  const { t } = useTranslation();
  const { addAlert, addError } = useAlerts();
  const keyclockidpClient = useKeyclockidpClient();

  const [loading, setLoading] = useState(true);
  const [assigned, setAssigned] = useState<TimeWindow[]>([]);
  const [available, setAvailable] = useState<TimeWindow[]>([]);
  const [selectedId, setSelectedId] = useState("");
  const [busy, setBusy] = useState(false);
  const [removeTargetId, setRemoveTargetId] = useState<number | null>(null);

  const refresh = useCallback(async () => {
    const [assignedRes, activeRes] = await Promise.all([
      keyclockidpClient.getUserTimeslots(),
      keyclockidpClient.getTimeslots(),
    ]);
    setAssigned(
      assignedRes.success ? asArray<TimeWindow>(assignedRes.data) : [],
    );
    setAvailable(activeRes.success ? asArray<TimeWindow>(activeRes.data) : []);
  }, [keyclockidpClient]);

  useEffect(() => {
    const fetchData = async () => {
      setLoading(true);
      try {
        await refresh();
      } catch (error) {
        addError("timeWindowFetchError", error);
      } finally {
        setLoading(false);
      }
    };
    void fetchData();
  }, [addError, refresh]);

  const assignableOptions = useMemo(() => {
    const assignedIds = new Set(assigned.map((w) => w.id));
    return available.filter((w) => !assignedIds.has(w.id));
  }, [assigned, available]);

  const handleAssign = async () => {
    if (!selectedId) return;
    setBusy(true);
    const newAssignedIds = [...assigned.map((w) => w.id), Number(selectedId)];
    const response = await keyclockidpClient.updateUserTimeslots({
      timeslot_ids: newAssignedIds,
    });
    if (response.success) {
      addAlert(t("timeWindowAssigned"));
      setSelectedId("");
      await refresh();
    } else {
      addError("timeWindowAssignError", response.error);
    }
    setBusy(false);
  };

  const removeTarget = assigned.find((w) => w.id === removeTargetId);

  const [toggleRemoveDialog, RemoveConfirm] = useConfirmDialog({
    titleKey: "ivaltTimeWindowUnassignConfirm",
    children: t("ivaltTimeWindowUnassignConfirmDialog", {
      name: removeTarget?.name || `Time Window #${removeTargetId ?? ""}`,
    }),
    continueButtonLabel: "remove",
    continueButtonVariant: ButtonVariant.danger,
    onConfirm: async () => {
      if (removeTargetId === null) return;
      setBusy(true);
      const newAssignedIds = assigned
        .filter((w) => w.id !== removeTargetId)
        .map((w) => w.id);
      const response = await keyclockidpClient.updateUserTimeslots({
        timeslot_ids: newAssignedIds,
      });
      if (response.success) {
        addAlert(t("timeWindowUnassigned"));
        setRemoveTargetId(null);
        await refresh();
      } else {
        addError("timeWindowUnassignError", response.error);
      }
      setBusy(false);
    },
  });

  const handleRemove = (timewindowId: number) => {
    setRemoveTargetId(timewindowId);
    toggleRemoveDialog();
  };

  if (loading) {
    return <KeycloakSpinner />;
  }

  return (
    <PageSection variant="light">
      <RemoveConfirm />
      <Toolbar>
        <ToolbarContent>
          <ToolbarItem>
            <FormSelect
              value={selectedId}
              onChange={(_, value) => setSelectedId(value)}
              aria-label={t("assignTimeWindow")}
              style={{ width: "300px" }}
              isDisabled={busy || assignableOptions.length === 0}
            >
              <FormSelectOption
                value=""
                label={t("selectTimeWindow")}
                isPlaceholder
              />
              {assignableOptions.map((w) => (
                <FormSelectOption
                  key={w.id}
                  value={w.id}
                  label={w.name || `Time Window #${w.id}`}
                />
              ))}
            </FormSelect>
          </ToolbarItem>
          <ToolbarItem>
            <Button
              variant="primary"
              onClick={handleAssign}
              isDisabled={!selectedId || busy}
            >
              {t("assignTimeWindow")}
            </Button>
          </ToolbarItem>
        </ToolbarContent>
      </Toolbar>
      <Table aria-label={t("userTimeWindowAssignments")} variant="compact">
        <Thead>
          <Tr>
            <Th>{t("name")}</Th>
            <Th>{t("ivaltTimeRange")}</Th>
            <Th aria-label={t("actions")} />
          </Tr>
        </Thead>
        <Tbody>
          {assigned.length === 0 ? (
            <Tr>
              <Td colSpan={3}>{t("noTimeWindowsAssigned")}</Td>
            </Tr>
          ) : (
            assigned.map((w) => (
              <Tr key={w.id}>
                <Td>{w.name || `Time Window #${w.id}`}</Td>
                <Td>{`${w.start_time} - ${w.end_time}`}</Td>
                <Td isActionCell>
                  <Button
                    variant="link"
                    isDanger
                    onClick={() => handleRemove(w.id)}
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
