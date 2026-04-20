import { useState, useCallback } from "react";
import { GoogleMap, LoadScript, Marker } from "@react-google-maps/api";

interface GoogleMapPickerProps {
  onLocationSelect: (lat: number, lng: number) => void;
  initialLat?: number;
  initialLng?: number;
}

const containerStyle = {
  width: "100%",
  height: "400px",
};

const defaultCenter = {
  lat: 37.7749,
  lng: -122.4194,
};

export default function GoogleMapPicker({
  onLocationSelect,
  initialLat,
  initialLng,
}: GoogleMapPickerProps) {
  const [marker, setMarker] = useState<google.maps.LatLngLiteral | null>(
    initialLat && initialLng ? { lat: initialLat, lng: initialLng } : null,
  );
  const [center, setCenter] = useState<google.maps.LatLngLiteral>(
    initialLat && initialLng
      ? { lat: initialLat, lng: initialLng }
      : defaultCenter,
  );

  const handleMapClick = useCallback(
    (e: google.maps.MapMouseEvent) => {
      const lat = e.latLng?.lat();
      const lng = e.latLng?.lng();

      if (lat !== undefined && lng !== undefined) {
        setMarker({ lat, lng });
        setCenter({ lat, lng });
        onLocationSelect(lat, lng);
      }
    },
    [onLocationSelect],
  );

  return (
    <LoadScript googleMapsApiKey={process.env.GOOGLE_MAPS_API_KEY || ""}>
      <GoogleMap
        mapContainerStyle={containerStyle}
        center={center}
        zoom={10}
        onClick={handleMapClick}
      >
        {marker && <Marker position={marker} />}
      </GoogleMap>
    </LoadScript>
  );
}
