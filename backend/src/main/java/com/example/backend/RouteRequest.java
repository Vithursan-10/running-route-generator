package com.example.backend;

import lombok.Data;

@Data
public class RouteRequest {
    private double lat;
    private double lon;
    private double distanceKm;
    private Long seed;  // Use Long (object) so it can be nullable

    public double getLat() { return lat; }
    public void setLat(double lat) { this.lat = lat; }

    public double getLon() { return lon; }
    public void setLon(double lon) { this.lon = lon; }

    public double getDistanceKm() { return distanceKm; }
    public void setDistanceKm(double distanceKm) { this.distanceKm = distanceKm; }

    public Long getSeed() { return seed; }
    public void setSeed(Long seed) { this.seed = seed; }
}
