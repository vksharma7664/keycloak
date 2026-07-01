import { useState } from "react";
import { useKeyclockidpClient } from "../api/keyclockidpClient";
import type {
  TimeWindow,
  TimeWindowCreateRequest,
  TimeWindowUpdateRequest,
} from "../api/types";
import {
  Modal,
  ModalVariant,
  Form,
  FormGroup,
  TimePicker,
  Checkbox,
  Button,
  ActionGroup,
  Alert,
  Select,
  SelectOption,
} from "@patternfly/react-core";

interface TimeWindowFormProps {
  timeWindow?: TimeWindow | null;
  onClose: () => void;
  onSave: () => void;
}

const COMMON_TIMEZONES = [
  "America/Los_Angeles",
  "America/New_York",
  "America/Chicago",
  "America/Denver",
  "Europe/London",
  "Europe/Paris",
  "Asia/Tokyo",
  "Asia/Shanghai",
  "Australia/Sydney",
  "UTC",
];

export default function TimeWindowForm({
  timeWindow,
  onClose,
  onSave,
}: TimeWindowFormProps) {
  const keyclockidpClient = useKeyclockidpClient();
  const [startTime, setStartTime] = useState(timeWindow?.start_time || "");
  const [endTime, setEndTime] = useState(timeWindow?.end_time || "");
  const [timezone, setTimezone] = useState(timeWindow?.timezone?.[0] || "UTC");
  const [isActive, setIsActive] = useState(timeWindow?.status ?? true);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [isTimezoneOpen, setIsTimezoneOpen] = useState(false);

  const handleTimezoneSelect = (
    _event?: React.MouseEvent,
    selection?: string | number,
  ) => {
    if (typeof selection === "string") {
      setTimezone(selection);
      setIsTimezoneOpen(false);
    }
  };

  const handleSubmit = async () => {
    if (!startTime) {
      setError("Start time is required");
      return;
    }
    if (!endTime) {
      setError("End time is required");
      return;
    }
    setLoading(true);
    setError(null);

    try {
      if (timeWindow) {
        const request: TimeWindowUpdateRequest = {
          start_time: startTime,
          end_time: endTime,
          timezone: [timezone],
          status: isActive,
        };
        const response = await keyclockidpClient.updateTimeslot(
          timeWindow.id,
          request,
        );
        if (response.success) {
          onSave();
        } else {
          setError(response.error || "Failed to update time window");
        }
      } else {
        const request: TimeWindowCreateRequest = {
          start_time: startTime,
          end_time: endTime,
          timezone: [timezone],
          status: isActive,
        };
        const response = await keyclockidpClient.createTimeslot(request);
        if (response.success) {
          onSave();
        } else {
          setError(response.error || "Failed to create time window");
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
      title={timeWindow ? "Edit Time Window" : "Create Time Window"}
      isOpen
      onClose={onClose}
    >
      {error && <Alert variant="danger" isInline title={error} />}
      <Form isHorizontal>
        <FormGroup label="Start Time" fieldId="startTime" isRequired>
          <TimePicker
            id="startTime"
            value={startTime}
            onChange={(_, value) => setStartTime(value)}
          />
        </FormGroup>
        <FormGroup label="End Time" fieldId="endTime" isRequired>
          <TimePicker
            id="endTime"
            value={endTime}
            onChange={(_, value) => setEndTime(value)}
          />
        </FormGroup>
        <FormGroup label="Timezone" fieldId="timezone">
          <Select
            isOpen={isTimezoneOpen}
            toggle={(ref) => (
              <button
                type="button"
                ref={ref as React.RefObject<HTMLButtonElement>}
                onClick={() => setIsTimezoneOpen(!isTimezoneOpen)}
              >
                {timezone}
              </button>
            )}
            onSelect={handleTimezoneSelect}
          >
            {COMMON_TIMEZONES.map((tz) => (
              <SelectOption key={tz} value={tz}>
                {tz}
              </SelectOption>
            ))}
          </Select>
        </FormGroup>
        <FormGroup label="Active" fieldId="active">
          <Checkbox
            id="active"
            isChecked={isActive}
            onChange={() => setIsActive(!isActive)}
          />
        </FormGroup>
      </Form>
      <ActionGroup>
        <Button variant="primary" onClick={handleSubmit} isLoading={loading}>
          {timeWindow ? "Update" : "Create"}
        </Button>
        <Button variant="link" onClick={onClose}>
          Cancel
        </Button>
      </ActionGroup>
    </Modal>
  );
}
