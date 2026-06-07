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
import { TimeWindow } from "../ivalt-settings/api/types";

function asArray<T>(data: any): T[] {
  if (Array.isArray(data)) return data;
  if (Array.isArray(data?.data)) return data.data;
  return [];
}

export default function UserTimeWindow() {
  const { adminClient } = useAdminClient();
  const { t } = useTranslation();
  const { addAlert, addError } = useAlerts();
  const { id: userId } = useParams<UserParams>();
  const keyclockidpClient = useKeyclockidpClient();

  const [loading, setLoading] = useState(true);
  const [userMobile, setUserMobile] = useState("");
  const [assigned, setAssigned] = useState<TimeWindow[]>([]);
  const [available, setAvailable] = useState<TimeWindow[]>([]);
  const [selectedId, setSelectedId] = useState("");
  const [busy, setBusy] = useState(false);

  const refresh = async (mobile: string) => {
    const [assignedRes, activeRes] = await Promise.all([
      keyclockidpClient.getAssignedTimeWindows(mobile),
      keyclockidpClient.getActiveTimeWindows(100, 0),
    ]);
    setAssigned(
      assignedRes.success ? asArray<TimeWindow>(assignedRes.data) : [],
    );
    setAvailable(activeRes.success ? asArray<TimeWindow>(activeRes.data) : []);
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
        addError("timeWindowFetchError", error);
      } finally {
        setLoading(false);
      }
    };
    void fetchData();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [userId]);

  const assignableOptions = useMemo(() => {
    const assignedIds = new Set(assigned.map((w) => w.id));
    return available.filter((w) => !assignedIds.has(w.id));
  }, [assigned, available]);

  const handleAssign = async () => {
    if (!selectedId) return;
    setBusy(true);
    const response = await keyclockidpClient.assignTimeWindow({
      user_mobile: userMobile,
      timewindow_id: Number(selectedId),
    });
    if (response.success) {
      addAlert(t("timeWindowAssigned"));
      setSelectedId("");
      await refresh(userMobile);
    } else {
      addError("timeWindowAssignError", response.error);
    }
    setBusy(false);
  };

  const handleRemove = async (timewindowId: number) => {
    setBusy(true);
    const response = await keyclockidpClient.removeTimeWindowAssignment({
      user_mobile: userMobile,
      timewindow_id: timewindowId,
    });
    if (response.success) {
      addAlert(t("timeWindowUnassigned"));
      await refresh(userMobile);
    } else {
      addError("timeWindowUnassignError", response.error);
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
                <FormSelectOption key={w.id} value={w.id} label={w.name} />
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
                <Td>{w.name}</Td>
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
