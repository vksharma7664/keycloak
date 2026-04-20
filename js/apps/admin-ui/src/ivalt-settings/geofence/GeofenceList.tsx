import { useState, useEffect } from "react";
import { keyclockidpClient } from "../api/keyclockidpClient";
import { Geofence } from "../api/types";
import {
  Button,
  Toolbar,
  ToolbarContent,
  ToolbarItem,
  Alert,
} from "@patternfly/react-core";
import { PlusIcon } from "@patternfly/react-icons";
import GeofenceTable from "./GeofenceTable";
import GeofenceForm from "./GeofenceForm";

export default function GeofenceList() {
  const [geofences, setGeofences] = useState<Geofence[]>([]);
  const [loading, setLoading] = useState(true);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingGeofence, setEditingGeofence] = useState<Geofence | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);

  const mobile = "+919530654704"; // TODO: Get from user context

  useEffect(() => {
    void loadGeofences();
  }, []);

  const loadGeofences = async () => {
    setLoading(true);
    setError(null);
    const response = await keyclockidpClient.getActiveGeofences(mobile);
    if (response.success && response.data) {
      setGeofences(response.data.data);
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

  const handleDelete = async (geofenceId: number) => {
    const response = await keyclockidpClient.deleteGeofence(geofenceId, mobile);
    if (response.success) {
      setSuccess("Geofence deleted successfully");
      void loadGeofences();
      setTimeout(() => setSuccess(null), 3000);
    } else {
      setError(response.error || "Failed to delete geofence");
    }
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
