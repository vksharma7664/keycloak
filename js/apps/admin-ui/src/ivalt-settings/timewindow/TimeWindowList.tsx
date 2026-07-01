import { useState, useEffect } from "react";
import { useKeyclockidpClient } from "../api/keyclockidpClient";
import type { TimeWindow, PaginationMeta } from "../api/types";
import {
  Button,
  Toolbar,
  ToolbarContent,
  ToolbarItem,
  Alert,
  Pagination,
} from "@patternfly/react-core";
import { PlusIcon } from "@patternfly/react-icons";
import TimeWindowTable from "./TimeWindowTable";
import TimeWindowForm from "./TimeWindowForm";

export default function TimeWindowList() {
  const keyclockidpClient = useKeyclockidpClient();
  const [timeWindows, setTimeWindows] = useState<TimeWindow[]>([]);
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

  const handleDelete = async (timeWindowId: number) => {
    const response = await keyclockidpClient.deleteTimeslot(timeWindowId);
    if (response.success) {
      setSuccess("Time window deleted successfully");
      void loadTimeWindows();
      setTimeout(() => setSuccess(null), 3000);
    } else {
      setError(response.error || "Failed to delete time window");
    }
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
