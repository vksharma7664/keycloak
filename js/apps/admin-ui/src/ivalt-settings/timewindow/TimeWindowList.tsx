import { useState, useEffect } from "react";
import { useTranslation } from "react-i18next";
import { useKeyclockidpClient } from "../api/keyclockidpClient";
import type { TimeWindow, PaginationMeta } from "../api/types";
import {
  Button,
  ButtonVariant,
  Toolbar,
  ToolbarContent,
  ToolbarItem,
  Alert,
  Pagination,
} from "@patternfly/react-core";
import { PlusIcon } from "@patternfly/react-icons";
import { useConfirmDialog } from "../../components/confirm-dialog/ConfirmDialog";
import TimeWindowTable from "./TimeWindowTable";
import TimeWindowForm from "./TimeWindowForm";

export default function TimeWindowList() {
  const { t } = useTranslation();
  const keyclockidpClient = useKeyclockidpClient();
  const [timeWindows, setTimeWindows] = useState<TimeWindow[]>([]);
  const [deleteTargetId, setDeleteTargetId] = useState<number | null>(null);
  const [loading, setLoading] = useState(true);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingTimeWindow, setEditingTimeWindow] = useState<TimeWindow | null>(
    null,
  );
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  const [page, setPage] = useState(1);
  const [perPage, setPerPage] = useState(10);
  const [pagination, setPagination] = useState<PaginationMeta | null>(null);

  // biome-ignore lint/correctness/useExhaustiveDependencies: page and perPage trigger reload
  useEffect(() => {
    void loadTimeWindows();
  }, [page, perPage]);

  const loadTimeWindows = async () => {
    setLoading(true);
    setError(null);
    const response = await keyclockidpClient.getTimeslots(
      page,
      perPage,
      "created_at",
      "desc",
    );
    if (response.success && response.data) {
      setTimeWindows(response.data);
      setPagination(response.meta || null);
    } else {
      setError(response.error || "Failed to load time windows");
    }
    setLoading(false);
  };

  const handleCreate = () => {
    setEditingTimeWindow(null);
    setIsModalOpen(true);
  };

  const handleEdit = (timeWindow: TimeWindow) => {
    setEditingTimeWindow(timeWindow);
    setIsModalOpen(true);
  };

  const deleteTarget = timeWindows.find((w) => w.id === deleteTargetId);

  const [toggleDeleteDialog, DeleteConfirm] = useConfirmDialog({
    titleKey: "ivaltTimeWindowDeleteConfirm",
    children: t("ivaltTimeWindowDeleteConfirmDialog", {
      name: (() => {
        if (Array.isArray(deleteTarget?.timezone)) {
          return deleteTarget.timezone.join(", ");
        }
        if (typeof deleteTarget?.timezone === "string") {
          try {
            const parsed = JSON.parse(deleteTarget.timezone);
            return Array.isArray(parsed)
              ? parsed.join(", ")
              : deleteTarget.timezone;
          } catch {
            return deleteTarget.timezone;
          }
        }
        return `Time Window #${deleteTargetId ?? ""}`;
      })(),
    }),
    continueButtonLabel: "delete",
    continueButtonVariant: ButtonVariant.danger,
    onConfirm: async () => {
      if (deleteTargetId === null) return;
      const response = await keyclockidpClient.deleteTimeslot(deleteTargetId);
      if (response.success) {
        setSuccess("Time window deleted successfully");
        setDeleteTargetId(null);
        void loadTimeWindows();
        setTimeout(() => setSuccess(null), 3000);
      } else {
        setError(response.error || "Failed to delete time window");
      }
    },
  });

  const handleDelete = (timeWindowId: number) => {
    setDeleteTargetId(timeWindowId);
    toggleDeleteDialog();
  };

  const handleModalClose = () => {
    setIsModalOpen(false);
    setEditingTimeWindow(null);
    setError(null);
  };

  const handleModalSave = () => {
    setIsModalOpen(false);
    setEditingTimeWindow(null);
    setSuccess(
      editingTimeWindow
        ? "Time window updated successfully"
        : "Time window created successfully",
    );
    void loadTimeWindows();
    setTimeout(() => setSuccess(null), 3000);
  };

  return (
    <>
      <DeleteConfirm />
      {error && <Alert variant="danger" isInline title={error} />}
      {success && <Alert variant="success" isInline title={success} />}
      <Toolbar>
        <ToolbarContent>
          <ToolbarItem>
            <Button
              variant="primary"
              icon={<PlusIcon />}
              onClick={handleCreate}
            >
              Create Time Window
            </Button>
          </ToolbarItem>
        </ToolbarContent>
      </Toolbar>
      <TimeWindowTable
        timeWindows={timeWindows}
        loading={loading}
        onEdit={handleEdit}
        onDelete={handleDelete}
      />
      {pagination && (
        <Pagination
          itemCount={pagination.total}
          perPage={perPage}
          page={page}
          perPageOptions={[
            { title: "10", value: 10 },
            { title: "20", value: 20 },
            { title: "50", value: 50 },
            { title: "100", value: 100 },
          ]}
          onSetPage={(_, newPage) => setPage(newPage)}
          onPerPageSelect={(_, newPerPage) => {
            setPerPage(newPerPage);
            setPage(1);
          }}
          variant="bottom"
        />
      )}
      {isModalOpen && (
        <TimeWindowForm
          timeWindow={editingTimeWindow}
          onClose={handleModalClose}
          onSave={handleModalSave}
        />
      )}
    </>
  );
}
