import React, { useState, useEffect } from 'react';
import RunMap from "./RunMap";
import 'leaflet/dist/leaflet.css';
import { FaRoute, FaInfoCircle } from "react-icons/fa";
import axios from 'axios';

function App() {
  const [userLocation, setUserLocation] = useState(null);
  const [distanceKm, setDistanceKm] = useState(5);
  const [routeCoordinates, setRouteCoordinates] = useState([]);
  const [isInfoOpen, setIsInfoOpen] = useState(false);
  const [loadingRoute, setLoadingRoute] = useState(false);


   //Get User Location
  useEffect(() => {
    if (!navigator.geolocation) {
      alert('Geolocation not supported by your browser');
      return;
    }
    navigator.geolocation.getCurrentPosition(
      (pos) => setUserLocation({ lat: pos.coords.latitude, lon: pos.coords.longitude }),
      () => alert('Unable to retrieve your location')
    );
  }, []);

    //Generate Route 
  const generateRoute = async () => {
    if (!userLocation) {
      alert('User location not set');
      return;
    }
    setLoadingRoute(true);
    try {
      const response = await axios.post('http://localhost:8080/api/routes', {
        lat: userLocation.lat,
        lon: userLocation.lon,
        distanceKm,
      });
      setRouteCoordinates(response.data.coordinates);
    } catch (error) {
      const msg = error.response?.data;
      alert(typeof msg === "object" ? JSON.stringify(msg, null, 2) : msg);
    } finally {
    setLoadingRoute(false);
    }
  };

   //Resets route cache 
  const handleReset = async () => {
    try {
      const res = await fetch("http://localhost:8080/api/routes/reset-cache", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
      });
      if (!res.ok) throw new Error("Failed to reset cache");
      const data = await res.json();
      console.log("Cache reset:", data.message);
    } catch (error) {
      console.error("Error resetting cache:", error);
      alert("Failed to reset cache. Check backend logs.");
    }
  };

  //Download GPX 
  const downloadGPX = async () => {
    if (!userLocation) return alert("User location not set");

    try {
      const response = await axios.post(
        'http://localhost:8080/api/routes/gpx',
        {
          lat: userLocation.lat,
          lon: userLocation.lon,
          distanceKm,
        },
        { responseType: 'blob' } // important for file downloads
      );

      const url = window.URL.createObjectURL(new Blob([response.data]));
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', 'route.gpx');
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.URL.revokeObjectURL(url);
    } catch (err) {
      console.error(err);
      alert("Failed to download GPX. Check backend logs.");
    }
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center' }}>
      <h1>Route<FaRoute />Findr</h1>

      <div style={{
        position: "fixed",
          top: "4rem",
          left: 0,
          right: 0,
           flexWrap: 'wrap',
          display: "flex",
          alignItems: "center",
          justifyContent: "center",
          gap: "0.5rem",
          padding: "0.5rem",
          background: "rgba(255, 255, 255, 0.9)",
          borderTop: "1px solid #ccc",
          zIndex: 100,
      }}>
        
          {/* Distance Input */}
        <label style={{ display: "flex", alignItems: "center", gap: "0.3rem" }}>
          Distance/km:
          <input
            type="number"
            value={distanceKm}
            onChange={(e) => setDistanceKm(Number(e.target.value))}
            min={1}
            max={20}
            step={0.1}
            className="input"
          />
        </label>

        {/* Generate Route Button */}
        <button
          onClick={generateRoute}
          disabled={loadingRoute || !userLocation}
          style={{
            backgroundColor: "#089208ff",
            color: "white",
            border: "none",
            padding: "10px 20px",
            borderRadius: "6px",
            cursor: "pointer",
            fontSize: "15px",
            fontWeight: 500,
            flex: '1 1 auto',
            minWidth: '120px',
            boxShadow: "0 2px 4px rgba(0,0,0,0.1)",
            transition: "all 0.2s ease",
          }}
          onMouseEnter={(e) => {
            e.currentTarget.style.transform = "translateY(-2px)";
            e.currentTarget.style.boxShadow = "0 4px 8px rgba(0,0,0,0.15)";
          }}
          onMouseLeave={(e) => {
            e.currentTarget.style.transform = "translateY(0)";
            e.currentTarget.style.boxShadow = "0 2px 4px rgba(0,0,0,0.1)";
          }}
        >
          {loadingRoute ? "Generating..." : "Generate Route"}
        </button>


       {/* Reset Button */}
  <button style={{
    backgroundColor: '#dc3545',
    color: 'white',
    border: 'none',
    padding: '8px 16px',
    borderRadius: '6px',
    cursor: 'pointer',
    fontSize: '14px',
    fontWeight: 500,
    flex: '1 1 auto',
    minWidth: '120px',
    transition: 'all 0.2s ease'
  }}
  onMouseEnter={(e) => e.currentTarget.style.backgroundColor = '#c82333'}
  onMouseLeave={(e) => e.currentTarget.style.backgroundColor = '#dc3545'}
  onClick={handleReset}
  >
    Reset  
  </button>

   {/* DownloadGPX Button */}
  <button style={{
    backgroundColor: '#1976d2',
    color: 'white',
    border: 'none',
    padding: '8px 16px',
    borderRadius: '6px',
    cursor: 'pointer',
    fontSize: '14px',
    fontWeight: 500,
    flex: '1 1 auto',
    minWidth: '120px',
    transition: 'all 0.2s ease'
  }}
  onMouseEnter={(e) => e.currentTarget.style.backgroundColor = '#115eaaff'}
  onMouseLeave={(e) => e.currentTarget.style.backgroundColor = '#1976d2'}
  onClick={downloadGPX}
  >Download GPX</button>

  
  {/* Info toggle button */}
  <button 
    onClick={() => setIsInfoOpen(!isInfoOpen)}
    style={{
      background: "none",
      border: "none",
      cursor: "pointer",
      fontSize: "20px",
      display: "flex",
      alignItems: "center",
      color: "#555",
      minWidth: "40px"
    }}
  >
    <FaInfoCircle />
  </button>


{/* Info box */}
{isInfoOpen && (
  <div
    style={{
      position: 'absolute',
      marginTop: '8px',
      padding: '15px 20px',
      backgroundColor: 'lightgray',
      border: '1px solid #ccc',
      borderRadius: '6px',
      width: '500px',
      zIndex: 1000,
      boxShadow: '0 2px 8px rgba(0,0,0,0.15)',
    }}
  >
    {/* Close button (X) */}
    <button
      onClick={() => setIsInfoOpen(false)}
      style={{
        position: 'absolute',
        top: '5px',
        right: '5px',
        background: 'none',
        border: 'none',
        fontSize: '20px',
        fontWeight: 'bold',
        cursor: 'pointer',
        lineHeight: '1',
      }}
    >
      ×
    </button>

    {/* Info content */}
    <p>
      Input distance and then press generate route to generate routes.
      Press reset to get a new batch of routes.
    </p>
  </div>
)}


      </div>
     
       {/* Leaflet Map */}
      <RunMap
        userPosition={userLocation ? [userLocation.lat, userLocation.lon] : [51.505, -0.09]}
        routeCoordinates={routeCoordinates.map(p => [p.lat, p.lng])}
      />
    </div>
  );
}

export default App;
