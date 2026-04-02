import React, { useState, useEffect } from 'react';
import { MapContainer, TileLayer, Marker, useMapEvents } from 'react-leaflet';
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';

// Fix default marker icon issue with Leaflet in React
import markerIcon from 'leaflet/dist/images/marker-icon.png';
import markerShadow from 'leaflet/dist/images/marker-shadow.png';

let DefaultIcon = L.icon({
  iconUrl: markerIcon,
  shadowUrl: markerShadow,
  iconSize: [25, 41],
  iconAnchor: [12, 41],
});

L.Marker.prototype.options.icon = DefaultIcon;

function LocationMarker({ position, setPosition }) {
  const map = useMapEvents({
    click(e) {
      setPosition(e.latlng);
    },
  });

  return position === null ? null : (
    <Marker position={position} />
  );
}

const LocationPicker = ({ onLocationSelect, initialPosition, height = '300px' }) => {
  const [position, setPosition] = useState(initialPosition || null);
  const [searchQuery, setSearchQuery] = useState('');

  // Default to a central location if no initial position
  const defaultCenter = initialPosition || { lat: 20.5937, lng: 78.9629 }; // Center of India

  useEffect(() => {
    if (position) {
      onLocationSelect({
        latitude: position.lat,
        longitude: position.lng
      });
    }
  }, [position, onLocationSelect]);

  const getCurrentLocation = () => {
    if (navigator.geolocation) {
      navigator.geolocation.getCurrentPosition(
        (pos) => {
          const newPos = {
            lat: pos.coords.latitude,
            lng: pos.coords.longitude
          };
          setPosition(newPos);
        },
        (error) => {
          alert('Error getting location: ' + error.message);
        }
      );
    } else {
      alert('Geolocation is not supported by your browser');
    }
  };

  return (
    <div className="location-picker">
      <div className="location-controls">
        <button type="button" className="btn btn-secondary" onClick={getCurrentLocation}>
          📍 Use My Current Location
        </button>
        <p className="location-hint">Or click on the map to select a location</p>
      </div>
      
      {position && (
        <div className="selected-coordinates">
          <p><strong>Selected:</strong> Lat: {position.lat.toFixed(6)}, Lng: {position.lng.toFixed(6)}</p>
        </div>
      )}

      <MapContainer
        center={defaultCenter}
        zoom={13}
        style={{ height: height, width: '100%' }}
      >
        <TileLayer
          attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
          url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
        />
        <LocationMarker position={position} setPosition={setPosition} />
      </MapContainer>
    </div>
  );
};

export default LocationPicker;
