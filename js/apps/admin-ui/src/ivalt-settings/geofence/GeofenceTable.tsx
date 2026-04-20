import {
  Table,
  Thead,
  Tbody,
  Tr,
  Th,
  Td,
  TableText,
} from "@patternfly/react-table";
import { Button } from "@patternfly/react-core";
import { Geofence } from "../api/types";

interface GeofenceTableProps {
  geofences: Geofence[];
  loading?: boolean;
  onEdit: (geofence: Geofence) => void;
  onDelete: (geofenceId: number) => void;
}

export default function GeofenceTable({
  geofences,
  loading,
  onEdit,
  onDelete,
}: GeofenceTableProps) {
  const columns = [
    "ID",
    "Name",
    "Location (Lat, Lng)",
    "Radius",
    "Status",
    "Actions",
  ];

  return (
    <Table aria-label="Geofence table" variant="compact">
      <Thead>
        <Tr>
          {columns.map((column) => (
            <Th key={column}>{column}</Th>
          ))}
        </Tr>
      </Thead>
      <Tbody>
        {loading ? (
          <Tr>
            <Td colSpan={6}>Loading...</Td>
          </Tr>
        ) : geofences.length === 0 ? (
          <Tr>
            <Td colSpan={6}>No geofences found</Td>
          </Tr>
        ) : (
          geofences.map((geofence) => (
            <Tr key={geofence.id}>
              <Td>{geofence.id}</Td>
              <Td>
                <TableText wrapModifier="truncate">{geofence.name}</TableText>
              </Td>
              <Td>{`${geofence.latitude}, ${geofence.longitude}`}</Td>
              <Td>{geofence.radius}m</Td>
              <Td>{geofence.is_active ? "Active" : "Inactive"}</Td>
              <Td>
                <Button variant="plain" onClick={() => onEdit(geofence)}>
                  Edit
                </Button>
                <Button variant="plain" onClick={() => onDelete(geofence.id)}>
                  Delete
                </Button>
              </Td>
            </Tr>
          ))
        )}
      </Tbody>
    </Table>
  );
}
