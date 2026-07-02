import { useState, useEffect } from "react";
import { useTranslation } from "react-i18next";
import { useKeyclockidpClient } from "../api/keyclockidpClient";
import type { Geofence, PaginationMeta } from "../api/types";
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
import GeofenceTable from "./GeofenceTable";
import GeofenceForm from "./GeofenceForm";

export default function GeofenceList() {
  const { t } = useTranslation();
  const keyclockidpClient = useKeyclockidpClient();
  const [geofences, setGeofences] = useState<Geofence[]>([]);
  const [deleteTargetId, setDeleteTargetId] = useState<number | null>(null);
  const [loading, setLoading] = useState(true);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingGeofence, setEditingGeofence] = useState<Geofence | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  const [page, setPage] = useState(1);
  const [perPage, setPerPage] = useState(10);
  const [pagination, setPagination] = useState<PaginationMeta | null>(null);

  // biome-ignore lint/correctness/useExhaustiveDependencies: page and perPage trigger reload
  useEffect(() => {
    void loadGeofences();
  }, [page, perPage]);

  const loadGeofences = async () => {
    setLoading(true);
    setError(null);
    const response = await keyclockidpClient.getGeofences(
      page,
      perPage,
      "created_at",
      "desc",
    );
    if (response.success && response.data) {
      setGeofences(response.data);
      setPagination(response.meta || null);
    } else {
      setError(response.error || "Failed to load geofences");
    }
    setLoading(false);
  };

  const handleCreate = () => {
    setEditingGeofence(null);
    setIsModalOpen(true);
  };

  const handleEdit = (geofence: Geofence) => {
    setEditingGeofence(geofence);
    setIsModalOpen(true);
  };

  const deleteTarget = geofences.find((g) => g.id === deleteTargetId);

  const [toggleDeleteDialog, DeleteConfirm] = useConfirmDialog({
    titleKey: "ivaltGeofenceDeleteConfirm",
    children: t("ivaltGeofenceDeleteConfirmDialog", {
      name: deleteTarget?.name ?? "",
    }),
    continueButtonLabel: "delete",
    continueButtonVariant: ButtonVariant.danger,
    onConfirm: async () => {
      if (deleteTargetId === null) return;
      const response = await keyclockidpClient.deleteGeofence(deleteTargetId);
      if (response.success) {
        setSuccess("Geofence deleted successfully");
        setDeleteTargetId(null);
        void loadGeofences();
        setTimeout(() => setSuccess(null), 3000);
      } else {
        setError(response.error || "Failed to delete geofence");
      }
    },
  });

  const handleDelete = (geofenceId: number) => {
    setDeleteTargetId(geofenceId);
    toggleDeleteDialog();
  };

  const handleModalClose = () => {
    setIsModalOpen(false);
    setEditingGeofence(null);
    setError(null);
  };

  const handleModalSave = () => {
    setIsModalOpen(false);
    setEditingGeofence(null);
    setSuccess(
      editingGeofence
        ? "Geofence updated successfully"
        : "Geofence created successfully",
    );
    void loadGeofences();
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
              Create Geofence
            </Button>
          </ToolbarItem>
        </ToolbarContent>
      </Toolbar>
      <GeofenceTable
        geofences={geofences}
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
        <GeofenceForm
          geofence={editingGeofence}
          onClose={handleModalClose}
          onSave={handleModalSave}
        />
      )}
    </>
  );
}
