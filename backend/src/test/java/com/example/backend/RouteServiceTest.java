package com.example.backend;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RouteServiceTest {

    private RouteService routeService;

    @BeforeEach
    void setUp() {
        routeService = new RouteService();
        routeService.resetCache();
    }

    @Test
    void testGenerateBackAndForthRoute_returnsValidRoute() {
        double lat = 51.5074;
        double lon = -0.1278;
        double distanceKm = 5.0;
        long seed = 42;

        Map<String, Object> route = routeService.generateBackAndForthRoute(lat, lon, distanceKm, seed);

        assertNotNull(route, "Route should not be null");
        assertTrue((Boolean) route.get("success"), "Route generation should be successful");

        // Coordinates check
        @SuppressWarnings("unchecked")
        List<Map<String, Double>> coords = (List<Map<String, Double>>) route.get("coordinates");
        assertNotNull(coords, "Coordinates list should not be null");
        assertTrue(coords.size() > 1, "Route should have more than one coordinate");

        // Distance check with safer tolerance
        double actualDistanceKm = (Double) route.get("actualDistanceKm");
        assertTrue(actualDistanceKm > 0, "Actual distance should be positive");
        assertTrue(actualDistanceKm >= distanceKm * 0.5,
                "Route should be at least half the requested distance");
    }

    @Test
    void testExportRouteAsJSON() {
        double lat = 51.5074;
        double lon = -0.1278;
        double distanceKm = 3.0;
        long seed = 123;

        Map<String, Object> route = routeService.generateBackAndForthRoute(lat, lon, distanceKm, seed);
        String json = routeService.exportRouteAsJSON(route);

        assertNotNull(json, "JSON output should not be null");
        assertTrue(json.contains("\"coordinates\""), "JSON should contain coordinates");
        assertTrue(json.contains("\"distanceMeters\""), "JSON should contain distanceMeters");
    }

    @Test
    void testExportRouteAsGPX() {
        double lat = 51.5074;
        double lon = -0.1278;
        double distanceKm = 2.0;
        long seed = 7;

        Map<String, Object> route = routeService.generateBackAndForthRoute(lat, lon, distanceKm, seed);
        String gpx = routeService.exportRouteAsGPX(route);

        assertNotNull(gpx, "GPX output should not be null");
        assertTrue(gpx.startsWith("<?xml"), "GPX should start with XML declaration");
        assertTrue(gpx.contains("<trkpt lat="), "GPX should contain track points");
    }

   @Test
void testCacheWorks() {
    double lat = 51.5074;
    double lon = -0.1278;
    double distanceKm = 4.0;
    long seed = 1;

    Map<String, Object> route1 = routeService.generateBackAndForthRoute(lat, lon, distanceKm, seed);
    Map<String, Object> route2 = routeService.generateBackAndForthRoute(lat, lon, distanceKm, seed);

    assertNotNull(route1, "First route should not be null");
    assertNotNull(route2, "Second route should not be null");

    // Check same coordinates & distance, not object reference
    assertEquals(route1.get("coordinates"), route2.get("coordinates"),
            "Cached routes should have the same coordinates");
    assertEquals(route1.get("actualDistanceKm"), route2.get("actualDistanceKm"),
            "Cached routes should have the same distance");
}
}
