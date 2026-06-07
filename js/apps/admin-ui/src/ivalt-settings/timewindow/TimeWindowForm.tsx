import { useState } from "react";
import { useKeyclockidpClient } from "../api/keyclockidpClient";
import {
  TimeWindow,
  TimeWindowCreateRequest,
  TimeWindowUpdateRequest,
  DayOfWeek,
} from "../api/types";
import {
  Modal,
  ModalVariant,
  Form,
  FormGroup,
  TextInput,
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

const DAYS: DayOfWeek[] = ["Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"];

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
  const [name, setName] = useState(timeWindow?.name || "");
  const [startTime, setStartTime] = useState(timeWindow?.start_time || "");
  const [endTime, setEndTime] = useState(timeWindow?.end_time || "");
  const [daysActive, setDaysActive] = useState<DayOfWeek[]>(
    timeWindow?.days_active.filter((d): d is DayOfWeek =>
      DAYS.includes(d as DayOfWeek),
    ) || [],
  );
  const [timezone, setTimezone] = useState(timeWindow?.timezone?.[0] || "UTC");
  const [isActive, setIsActive] = useState(timeWindow?.status ?? true);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [isTimezoneOpen, setIsTimezoneOpen] = useState(false);

  const handleDayToggle = (day: DayOfWeek) => {
    setDaysActive((prev) =>
      prev.includes(day) ? prev.filter((d) => d !== day) : [...prev, day],
    );
  };

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
    if (!name) {
      setError("Name is required");
      return;
    }
    if (!startTime) {
      setError("Start time is required");
      return;
    }
    if (!endTime) {
      setError("End time is required");
      return;
    }
    if (daysActive.length === 0) {
      setError("At least one day must be selected");
      return;
    }

    setLoading(true);
    setError(null);

    try {
      if (timeWindow) {
        const request: TimeWindowUpdateRequest = {
          name,
          start_time: startTime,
          end_time: endTime,
          days_active: daysActive,
          timezone: [timezone],
          status: isActive,
        };
        const response = await keyclockidpClient.updateTimeWindow(
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
          name,
          start_time: startTime,
          end_time: endTime,
          days_active: daysActive,
          timezone: [timezone],
          status: isActive,
        };
        const response = await keyclockidpClient.createTimeWindow(request);
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
        <FormGroup label="Name" fieldId="name" isRequired>
          <TextInput
            id="name"
            value={name}
            onChange={(_, value) => setName(value)}
          />
        </FormGroup>
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
        <FormGroup label="Active Days" fieldId="daysActive" isRequired>
          {DAYS.map((day) => (
            <Checkbox
              key={day}
              id={`day-${day}`}
              label={day}
              isChecked={daysActive.includes(day)}
              onChange={() => handleDayToggle(day)}
            />
          ))}
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
