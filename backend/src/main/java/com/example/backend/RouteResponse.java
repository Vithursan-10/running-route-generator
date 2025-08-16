package com.example.backend;

import java.util.List;

public class RouteResponse {

    private List<LatLng> coordinates;

    // === Constructor ===
    public RouteResponse(List<LatLng> coordinates) {
        this.coordinates = coordinates;
    }

    // === Getter ===
    public List<LatLng> getCoordinates() {
        return coordinates;
    }

    // === Setter ===
    public void setCoordinates(List<LatLng> coordinates) {
        this.coordinates = coordinates;
    }

    // === Optional: toString for debugging ===
    @Override
    public String toString() {
        return "RouteResponse{" +
                "coordinates=" + coordinates +
                '}';
    }

    // === Static inner class for LatLng ===
    public static class LatLng {
        private double lat;
        private double lng;

        // === Constructor ===
        public LatLng(double lat, double lng) {
            this.lat = lat;
            this.lng = lng;
        }

        // === Getters ===
        public double getLat() {
            return lat;
        }

        public double getLng() {
            return lng;
        }

        // === Setters ===
        public void setLat(double lat) {
            this.lat = lat;
        }

        public void setLng(double lng) {
            this.lng = lng;
        }

        // === Optional: toString ===
        @Override
        public String toString() {
            return "LatLng{" +
                    "lat=" + lat +
                    ", lng=" + lng +
                    '}';
        }
    }
}
