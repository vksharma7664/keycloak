import { useState } from "react";
import { useKeyclockidpClient } from "../api/keyclockidpClient";
import {
  Geofence,
  GeofenceCreateRequest,
  GeofenceUpdateRequest,
} from "../api/types";
import {
  Modal,
  ModalVariant,
  Form,
  FormGroup,
  TextInput,
  NumberInput,
  Switch,
  Button,
  ActionGroup,
  Alert,
} from "@patternfly/react-core";
import GoogleMapPicker from "./GoogleMapPicker";

interface GeofenceFormProps {
  geofence?: Geofence | null;
  onClose: () => void;
  onSave: () => void;
}

export default function GeofenceForm({
  geofence,
  onClose,
  onSave,
}: GeofenceFormProps) {
  const keyclockidpClient = useKeyclockidpClient();
  const [name, setName] = useState(geofence?.name || "");
  const [latitude, setLatitude] = useState(geofence?.latitude || 0);
  const [longitude, setLongitude] = useState(geofence?.longitude || 0);
  const [radius, setRadius] = useState(geofence?.radius || 100);
  const [isActive, setIsActive] = useState(geofence?.is_active ?? true);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleLocationSelect = (lat: number, lng: number) => {
    setLatitude(lat);
    setLongitude(lng);
  };

  const handleSubmit = async () => {
    if (!name) {
      setError("Name is required");
      return;
    }
    if (latitude < -90 || latitude > 90) {
      setError("Latitude must be between -90 and 90");
      return;
    }
    if (longitude < -180 || longitude > 180) {
      setError("Longitude must be between -180 and 180");
      return;
    }
    if (radius < 1) {
      setError("Radius must be at least 1 meter");
      return;
    }

    setLoading(true);
    setError(null);

    try {
      if (geofence) {
        const request: GeofenceUpdateRequest = {
          name,
          latitude,
          longitude,
          radius,
          is_active: isActive,
        };
        const response = await keyclockidpClient.updateGeofence(
          geofence.id,
          request,
        );
        if (response.success) {
          onSave();
        } else {
          setError(response.error || "Failed to update geofence");
        }
      } else {
        const request: GeofenceCreateRequest = {
          name,
          latitude,
          longitude,
          radius,
          is_active: isActive,
        };
        const response = await keyclockidpClient.createGeofence(request);
        if (response.success) {
          onSave();
        } else {
          setError(response.error || "Failed to create geofence");
        }
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : "Unknown error");
    }

    setLoading(false);
  };

  return (
    <Modal
      variant={ModalVariant.medium}
      title={geofence ? "Edit Geofence" : "Create Geofence"}
      isOpen
      onClose={onClose}
    >
      {error && <Alert variant="danger" isInline title={error} />}
      <Form isHorizontal>
        <FormGroup label="Name" fieldId="name" isRequired>
          <TextInput
            id="name"
            value={name}
            onChange={(_, value) => setName(value)}
          />
        </FormGroup>
        <FormGroup label="Location" fieldId="location" isRequired>
          <GoogleMapPicker
            onLocationSelect={handleLocationSelect}
            initialLat={latitude || undefined}
            initialLng={longitude || undefined}
            googleMapsApiKey=""
          />
          <div style={{ marginTop: "10px" }}>
            <TextInput
              id="latitude"
              value={latitude.toString()}
              onChange={(_, value) => setLatitude(Number(value) || 0)}
              placeholder="Latitude"
              style={{ marginBottom: "5px" }}
            />
            <TextInput
              id="longitude"
              value={longitude.toString()}
              onChange={(_, value) => setLongitude(Number(value) || 0)}
              placeholder="Longitude"
            />
          </div>
        </FormGroup>
        <FormGroup label="Radius (meters)" fieldId="radius" isRequired>
          <NumberInput
            id="radius"
            value={radius}
            min={1}
            onChange={(event) => setRadius(Number(event.currentTarget.value))}
            onMinus={() => setRadius(Math.max(1, radius - 1))}
            onPlus={() => setRadius(radius + 1)}
          />
        </FormGroup>
        <FormGroup label="Active" fieldId="active">
          <Switch
            id="active"
            isChecked={isActive}
            onChange={() => setIsActive(!isActive)}
          />
        </FormGroup>
      </Form>
      <ActionGroup>
        <Button variant="primary" onClick={handleSubmit} isLoading={loading}>
          {geofence ? "Update" : "Create"}
        </Button>
        <Button variant="link" onClick={onClose}>
          Cancel
        </Button>
      </ActionGroup>
    </Modal>
  );
}
