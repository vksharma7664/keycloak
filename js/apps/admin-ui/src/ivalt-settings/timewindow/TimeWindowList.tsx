import { useState, useEffect } from "react";
import { useKeyclockidpClient } from "../api/keyclockidpClient";
import { TimeWindow } from "../api/types";
import {
  Button,
  Toolbar,
  ToolbarContent,
  ToolbarItem,
  Alert,
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

  useEffect(() => {
    void loadTimeWindows();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const loadTimeWindows = async () => {
    setLoading(true);
    setError(null);
    const response = await keyclockidpClient.getTimeslots();
    if (response.success && response.data) {
      setTimeWindows(response.data.data);
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
