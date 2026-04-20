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
import { TimeWindow } from "../api/types";

interface TimeWindowTableProps {
  timeWindows: TimeWindow[];
  loading?: boolean;
  onEdit: (timeWindow: TimeWindow) => void;
  onDelete: (timeWindowId: number) => void;
}

export default function TimeWindowTable({
  timeWindows,
  loading,
  onEdit,
  onDelete,
}: TimeWindowTableProps) {
  const columns = ["ID", "Name", "Time Range", "Days", "Status", "Actions"];

  return (
    <Table aria-label="Time Window table" variant="compact">
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
        ) : timeWindows.length === 0 ? (
          <Tr>
            <Td colSpan={6}>No time windows found</Td>
          </Tr>
        ) : (
          timeWindows.map((timeWindow) => (
            <Tr key={timeWindow.id}>
              <Td>{timeWindow.id}</Td>
              <Td>
                <TableText wrapModifier="truncate">{timeWindow.name}</TableText>
              </Td>
              <Td>{`${timeWindow.start_time} - ${timeWindow.end_time}`}</Td>
              <Td>{timeWindow.days_active.join(", ")}</Td>
              <Td>{timeWindow.status ? "Active" : "Inactive"}</Td>
              <Td>
                <Button variant="plain" onClick={() => onEdit(timeWindow)}>
                  Edit
                </Button>
                <Button variant="plain" onClick={() => onDelete(timeWindow.id)}>
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
