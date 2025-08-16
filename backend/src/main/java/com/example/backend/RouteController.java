package com.example.backend;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@RestController
@RequestMapping("/api/routes")
public class RouteController {

    private final RouteService routeService;

    public RouteController(RouteService routeService) {
        this.routeService = routeService;
    }

    @PostMapping
    public ResponseEntity<?> createRoute(@RequestBody RouteRequest request) {
        try {
            validateRequest(request);

            long seed = ThreadLocalRandom.current().nextLong();
            Map<String, Object> route = routeService.generateBackAndForthRoute(
                    request.getLat(),
                    request.getLon(),
                    request.getDistanceKm(),
                    seed
            );

            return ResponseEntity.ok(route);

        } catch (IllegalArgumentException e) {
            return buildErrorResponse(HttpStatus.BAD_REQUEST, "Invalid request parameters", e.getMessage());
        } catch (RuntimeException e) {
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Route generation failed", e.getMessage());
        }
    }

    @PostMapping("/gpx")
    public ResponseEntity<?> downloadGPX(@RequestBody RouteRequest request) {
        try {
            validateRequest(request);

            long seed = ThreadLocalRandom.current().nextLong();
            Map<String, Object> route = routeService.generateBackAndForthRoute(
                    request.getLat(),
                    request.getLon(),
                    request.getDistanceKm(),
                    seed
            );

            String gpxContent = routeService.exportRouteAsGPX(route);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", "route.gpx");
            headers.setContentLength(gpxContent.getBytes(StandardCharsets.UTF_8).length);

            return new ResponseEntity<>(gpxContent, headers, HttpStatus.OK);

        } catch (IllegalArgumentException e) {
            return buildErrorResponse(HttpStatus.BAD_REQUEST, "Invalid request parameters", e.getMessage());
        } catch (RuntimeException e) {
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Route generation failed", e.getMessage());
        }
    }

    @PostMapping("/reset-cache")
    public ResponseEntity<?> resetCache() {
        routeService.resetCache();
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Route cache has been reset");
        response.put("success", true);
        response.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(response);
    }

    // ---------------- Helper Methods ----------------
    private void validateRequest(RouteRequest request) {
        if (request == null) throw new IllegalArgumentException("Request cannot be null");
        if (request.getLat() < -90 || request.getLat() > 90)
            throw new IllegalArgumentException("Latitude must be between -90 and 90");
        if (request.getLon() < -180 || request.getLon() > 180)
            throw new IllegalArgumentException("Longitude must be between -180 and 180");
        if (request.getDistanceKm() <= 0)
            throw new IllegalArgumentException("Distance must be greater than 0 km");
        if (request.getDistanceKm() > 50)
            throw new IllegalArgumentException("Distance cannot exceed 50 km");
    }

    private ResponseEntity<Map<String, Object>> buildErrorResponse(HttpStatus status, String error, String details) {
        Map<String, Object> body = new HashMap<>();
        body.put("success", false);
        body.put("error", error);
        body.put("details", details);
        body.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.status(status).body(body);
    }
}
