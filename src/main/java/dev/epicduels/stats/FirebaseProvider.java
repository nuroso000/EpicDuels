package dev.epicduels.stats;

import dev.epicduels.model.PlayerStats;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

/**
 * Stats provider backed by Firebase Realtime Database (REST API).
 *
 * Data structure at the database URL:
 * <pre>
 * /player_stats/{uuid}: { "wins": 0, "losses": 0 }
 * </pre>
 *
 * Use server-only authentication and restrictive database rules; never allow public writes.
 */
public class FirebaseProvider implements StatsProvider {

    private final String databaseUrl;  // e.g. https://project-default-rtdb.firebaseio.com
    private final String authToken;    // optional — database secret or empty
    private final HttpClient http;
    private final Logger logger;

    public FirebaseProvider(String databaseUrl, String authToken, Logger logger) {
        this.databaseUrl = databaseUrl.endsWith("/")
                ? databaseUrl.substring(0, databaseUrl.length() - 1) : databaseUrl;
        this.authToken = authToken;
        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.logger = logger;
    }

    @Override
    public CompletableFuture<PlayerStats> fetch(UUID playerId) {
        String uri = databaseUrl + "/player_stats/" + playerId + ".json" + authParam();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(uri))
                .GET()
                .timeout(Duration.ofSeconds(10))
                .build();

        return http.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() != 200) {
                        logger.warning("[Firebase] GET failed (" + response.statusCode() + "): " + response.body());
                        return null;
                    }
                    return parseResponse(response.body());
                })
                .exceptionally(ex -> {
                    logger.warning("[Firebase] fetch error for " + playerId + ": " + ex.getMessage());
                    return null;
                });
    }

    @Override
    public CompletableFuture<Void> recordResult(UUID eventId, UUID playerId, boolean win) {
        return commitResult(eventId, playerId, win, 0);
    }

    private CompletableFuture<Void> commitResult(UUID eventId, UUID playerId, boolean win, int attempt) {
        String uri = databaseUrl + "/player_stats/" + playerId + ".json" + authParam();
        HttpRequest read = HttpRequest.newBuilder(URI.create(uri)).header("X-Firebase-ETag", "true")
                .timeout(Duration.ofSeconds(10)).GET().build();
        return http.sendAsync(read, HttpResponse.BodyHandlers.ofString()).thenCompose(response -> {
            if (response.statusCode() != 200) return CompletableFuture.failedFuture(
                    new IllegalStateException("Firebase transaction read HTTP " + response.statusCode()));
            com.google.gson.JsonElement parsed = com.google.gson.JsonParser.parseString(response.body());
            com.google.gson.JsonObject data = parsed.isJsonNull() ? new com.google.gson.JsonObject() : parsed.getAsJsonObject();
            com.google.gson.JsonObject events = data.has("events") ? data.getAsJsonObject("events") : new com.google.gson.JsonObject();
            if (events.has(eventId.toString())) return CompletableFuture.completedFuture(null);
            String field = win ? "wins" : "losses";
            int value = data.has(field) ? data.get(field).getAsInt() : 0;
            data.addProperty(field, Math.addExact(value, 1));
            events.addProperty(eventId.toString(), true);
            data.add("events", events);
            String etag = response.headers().firstValue("ETag").orElseThrow();
            HttpRequest write = HttpRequest.newBuilder(URI.create(uri)).header("Content-Type", "application/json")
                    .header("If-Match", etag).timeout(Duration.ofSeconds(10))
                    .PUT(HttpRequest.BodyPublishers.ofString(data.toString())).build();
            return http.sendAsync(write, HttpResponse.BodyHandlers.ofString()).thenCompose(result -> {
                if (result.statusCode() == 200) return CompletableFuture.completedFuture(null);
                if (result.statusCode() == 412 && attempt < 8) return commitResult(eventId, playerId, win, attempt + 1);
                return CompletableFuture.failedFuture(new IllegalStateException("Firebase transaction HTTP " + result.statusCode()));
            });
        });
    }

    private String authParam() {
        if (authToken == null || authToken.isEmpty()) return "";
        return "?auth=" + authToken;
    }

    /**
     * Parse a Firebase JSON object: {"wins":0,"losses":0} or "null".
     */
    private PlayerStats parseResponse(String json) {
        com.google.gson.JsonElement parsed = com.google.gson.JsonParser.parseString(json);
        if (parsed.isJsonNull()) return null;
        com.google.gson.JsonObject data = parsed.getAsJsonObject();
        int wins = data.get("wins").getAsInt();
        int losses = data.get("losses").getAsInt();
        if (wins < 0 || losses < 0) throw new IllegalArgumentException("Negative remote statistics");
        return new PlayerStats(wins, losses);
    }
}
