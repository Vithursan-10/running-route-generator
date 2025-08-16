package com.example.backend;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RouteService {

    private static final String API_KEY = "eyJvcmciOiI1YjNjZTM1OTc4NTExMTAwMDFjZjYyNDgiLCJpZCI6IjRhYjMyZDNlZjhlYjRhNTFiMDM1MjEyZjFlMTVlMTdkIiwiaCI6Im11cm11cjY0In0=";
    private static final String ISOCHRONES_URL = "https://api.openrouteservice.org/v2/isochrones/foot-walking";
    private static final String DIRECTIONS_URL = "https://api.openrouteservice.org/v2/directions/foot-walking/geojson";
    private static final double MIN_TURNAROUND_DISTANCE_KM = 0.3;

    private final Map<String, List<Map<String, Object>>> routeCache = new ConcurrentHashMap<>();
    private final Map<String, Integer> routeIndex = new ConcurrentHashMap<>();

    public synchronized Map<String, Object> generateBackAndForthRoute(double lat, double lon, double distanceKm, long seed) {
        String key = lat + "," + lon + "," + distanceKm;

        if (routeCache.containsKey(key) && routeCache.get(key).size() >= 6) {
            int idx = routeIndex.getOrDefault(key, 0);
            Map<String, Object> route = routeCache.get(key).get(idx);
            routeIndex.put(key, (idx + 1) % 6);
            return route;
        }

        Map<String, Object> newRoute = createBackAndForth(lat, lon, distanceKm, seed);

        routeCache.computeIfAbsent(key, k -> new ArrayList<>()).add(newRoute);
        routeIndex.putIfAbsent(key, 0);

        return newRoute;
    }

    private Map<String, Object> createBackAndForth(double lat, double lon, double distanceKm, long seed) {
        Random random = new Random(seed);
        double targetLegKm = distanceKm / 2.0;

        // Step 1: Get reachable area
        Set<double[]> reachablePoints = getIsochrones(lat, lon, targetLegKm);

        if (reachablePoints.isEmpty()) {
            throw new RuntimeException("No reachable points from Isochrones API");
        }

        // Step 2: Try strict match (±5%)
        List<double[]> candidates = filterTurnaroundCandidates(reachablePoints, lat, lon, targetLegKm, 0.05);

        // Step 3: Looser match (±30%)
        if (candidates.isEmpty()) {
            candidates = filterTurnaroundCandidates(reachablePoints, lat, lon, targetLegKm, 0.30);
        }

        // Step 4: Fallback — pick farthest point
        if (candidates.isEmpty()) {
            candidates = new ArrayList<>(reachablePoints);
            candidates.sort((a, b) -> Double.compare(
                haversine(lat, lon, b[0], b[1]),
                haversine(lat, lon, a[0], a[1])
            ));
            candidates = Collections.singletonList(candidates.get(0));
            System.out.println("⚠️ Using farthest reachable point as fallback");
        }

        // Step 5: Add directional variety
        double[] turnaround = pickDirectionalCandidate(candidates, lat, lon, random);

        // Step 6: Get out-and-back route
        List<double[]> outRoute = getDirections(lat, lon, turnaround[0], turnaround[1]);
        List<double[]> backRoute = new ArrayList<>(outRoute);
        Collections.reverse(backRoute);

        List<double[]> fullRoute = new ArrayList<>(outRoute);
        fullRoute.addAll(backRoute.subList(1, backRoute.size()));

        double totalDistanceMeters = calculateTotalDistance(fullRoute);

        Map<String, Object> routeData = new HashMap<>();
        routeData.put("coordinates", toCoordinateMap(fullRoute));
        routeData.put("distanceMeters", totalDistanceMeters);
        routeData.put("actualDistanceKm", totalDistanceMeters / 1000.0);
        routeData.put("success", true);
        return routeData;
    }

       
    public String exportRouteAsJSON(Map<String, Object> route) {
        return new JSONObject(route).toString(2);
    }

    //Exports route as gpx
    public String exportRouteAsGPX(Map<String, Object> route) {
        @SuppressWarnings("unchecked")
        List<Map<String, Double>> coords = (List<Map<String, Double>>) route.get("coordinates");

        StringBuilder gpx = new StringBuilder();
        gpx.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        gpx.append("<gpx version=\"1.1\" creator=\"RouteService\" xmlns=\"http://www.topografix.com/GPX/1/1\">\n");
        gpx.append("<trk><name>Generated Route</name><trkseg>\n");

        for (Map<String, Double> point : coords) {
            gpx.append(String.format(Locale.US,
                    "<trkpt lat=\"%.6f\" lon=\"%.6f\"></trkpt>\n",
                    point.get("lat"), point.get("lng")));
        }

        gpx.append("</trkseg></trk>\n</gpx>");
        return gpx.toString();
    }

    private double[] pickDirectionalCandidate(List<double[]> candidates, double lat, double lon, Random random) {
        // Split into sectors (N, NE, E, SE, S, SW, W, NW)
        Map<Integer, List<double[]>> sectors = new HashMap<>();
        for (double[] p : candidates) {
            double angle = Math.atan2(p[1] - lon, p[0] - lat);
            int sector = (int) Math.round(((angle + Math.PI) / (2 * Math.PI)) * 8) % 8;
            sectors.computeIfAbsent(sector, k -> new ArrayList<>()).add(p);
        }

        List<Integer> availableSectors = new ArrayList<>(sectors.keySet());
        int chosenSector = availableSectors.get(random.nextInt(availableSectors.size()));

        List<double[]> sectorCandidates = sectors.get(chosenSector);
        return sectorCandidates.get(random.nextInt(sectorCandidates.size()));
    }

    private List<double[]> filterTurnaroundCandidates(Set<double[]> points, double lat, double lon, double targetLegKm, double tolerance) {
        double minDist = targetLegKm * (1 - tolerance);
        double maxDist = targetLegKm * (1 + tolerance);

        List<double[]> candidates = new ArrayList<>();
        for (double[] p : points) {
            double dist = haversine(lat, lon, p[0], p[1]);
            if (dist >= MIN_TURNAROUND_DISTANCE_KM && dist >= minDist && dist <= maxDist) {
                candidates.add(p);
            }
        }
        return candidates;
    }

    private Set<double[]> getIsochrones(double lat, double lon, double maxDistanceKm) {
        try {
            JSONObject body = new JSONObject();
            JSONArray locations = new JSONArray();
            locations.put(new JSONArray().put(lon).put(lat));
            body.put("locations", locations);

            // Boost range to 1.6x half-distance
            double rangeMeters = maxDistanceKm * 1000 * 1.6;
            body.put("range", new JSONArray().put((int) rangeMeters));
            body.put("range_type", "distance");

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(ISOCHRONES_URL))
                    .header("Content-Type", "application/json")
                    .header("Authorization", API_KEY)
                    .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                    .build();

            HttpClient client = HttpClient.newHttpClient();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new RuntimeException("Isochrones API error: " + response.body());
            }

            JSONObject json = new JSONObject(response.body());
            Set<double[]> points = new HashSet<>();

            JSONArray features = json.getJSONArray("features");
            for (int i = 0; i < features.length(); i++) {
                JSONObject geometry = features.getJSONObject(i).getJSONObject("geometry");
                JSONArray coordinates = geometry.getJSONArray("coordinates");
                for (int j = 0; j < coordinates.length(); j++) {
                    JSONArray polygon = coordinates.getJSONArray(j);
                    for (int k = 0; k < polygon.length(); k++) {
                        JSONArray coord = polygon.getJSONArray(k);
                        points.add(new double[]{coord.getDouble(1), coord.getDouble(0)});
                    }
                }
            }

            return points;

        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Isochrones request failed: " + e.getMessage(), e);
        }
    }

    private List<double[]> getDirections(double startLat, double startLon, double endLat, double endLon) {
        try {
            String body = String.format(Locale.US,
                    "{\"coordinates\":[[%f,%f],[%f,%f]]}",
                    startLon, startLat, endLon, endLat);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(DIRECTIONS_URL))
                    .header("Content-Type", "application/json")
                    .header("Authorization", API_KEY)
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpClient client = HttpClient.newHttpClient();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new RuntimeException("Directions API error: " + response.body());
            }

            JSONObject json = new JSONObject(response.body());
            JSONArray coords = json.getJSONArray("features")
                    .getJSONObject(0)
                    .getJSONObject("geometry")
                    .getJSONArray("coordinates");

            List<double[]> points = new ArrayList<>();
            for (int i = 0; i < coords.length(); i++) {
                JSONArray coord = coords.getJSONArray(i);
                points.add(new double[]{coord.getDouble(1), coord.getDouble(0)});
            }

            return points;

        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Directions request failed: " + e.getMessage(), e);
        }
    }

    private List<Map<String, Double>> toCoordinateMap(List<double[]> coords) {
        List<Map<String, Double>> list = new ArrayList<>();
        for (double[] p : coords) {
            Map<String, Double> m = new HashMap<>();
            m.put("lat", p[0]);
            m.put("lng", p[1]);
            list.add(m);
        }
        return list;
    }

    private double calculateTotalDistance(List<double[]> coords) {
        double dist = 0;
        for (int i = 1; i < coords.size(); i++) {
            dist += haversine(coords.get(i - 1)[0], coords.get(i - 1)[1],
                    coords.get(i)[0], coords.get(i)[1]) * 1000;
        }
        return dist;
    }

    private double haversine(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6371;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                   Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                   Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }


    //Resets cache
    public synchronized void resetCache() {
        routeCache.clear();
        routeIndex.clear();
    }
}
