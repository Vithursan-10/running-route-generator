import React, { useEffect } from "react";
import { MapContainer, TileLayer, Polyline, Marker, Popup, useMap } from "react-leaflet";
import './RunMap.css';
import { FaLocationDot } from "react-icons/fa6";
import ReactDOMServer from 'react-dom/server';
import L from 'leaflet';

// Component that recenters the map when position changes
function RecenterMap({ position }) {
  const map = useMap();
  useEffect(() => {
    if (position) {
      map.setView(position, map.getZoom());
    }
  }, [position, map]);
  return null;
}

// Component that fits bounds to the route
function FitBounds({ coordinates }) {
  const map = useMap();
  useEffect(() => {
    if (coordinates && coordinates.length > 1) {
      map.fitBounds(coordinates);
    }
  }, [coordinates, map]);
  return null;
}

const iconHTML = ReactDOMServer.renderToString(<FaLocationDot color="red" size={24} />);


const customIcon = L.divIcon({
  html: iconHTML,
  className: '', // prevent Leaflet default styles
  iconSize: [24, 24],
  iconAnchor: [12, 12],
});

export default function RunMap({ userPosition, routeCoordinates }) {
  const defaultCenter = [51.505, -0.09]; // London fallback

  return (
    <MapContainer
      center={userPosition || defaultCenter}
      zoom={13}
      style={{ height: "500px", width: "100%", position: 'fixed', bottom: '0rem'  }}
    >
      <TileLayer
        attribution='&copy; <a href="https://osm.org/copyright">OpenStreetMap</a>'
        url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
      />

      {/* Keep the map mounted and only move view */}
      {userPosition && <RecenterMap position={userPosition} />}

      {/* Smoothly fit route bounds without remount */}
      {routeCoordinates.length > 1 && (
        <>
          <Polyline positions={routeCoordinates} color="blue" />
          <FitBounds coordinates={routeCoordinates} />
        </>
      )}

      {/* Market on user position */}
      {userPosition && (
        <Marker position={userPosition} icon={customIcon} >
          <Popup>Your location</Popup>
        </Marker>
      )}
    </MapContainer>
  );
}
