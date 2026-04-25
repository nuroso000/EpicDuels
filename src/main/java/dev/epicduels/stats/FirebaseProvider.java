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
 * No authentication token is needed when the database rules allow public read/write,
 * or you can supply a database secret for legacy auth.
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
    public CompletableFuture<Void> push(UUID playerId, PlayerStats stats) {
        String uri = databaseUrl + "/player_stats/" + playerId + ".json" + authParam();
        String json = "{\"wins\":" + stats.getWins() + ",\"losses\":" + stats.getLosses() + "}";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(uri))
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(json))
                .timeout(Duration.ofSeconds(10))
                .build();

        return http.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    if (response.statusCode() != 200) {
                        logger.warning("[Firebase] PUT failed (" + response.statusCode() + "): " + response.body());
                    }
                })
                .exceptionally(ex -> {
                    logger.warning("[Firebase] push error for " + playerId + ": " + ex.getMessage());
                    return null;
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
        if (json == null) return null;
        json = json.trim();
        if (json.equals("null") || json.isEmpty()) return null;

        int wins = extractInt(json, "wins");
        int losses = extractInt(json, "losses");
        return new PlayerStats(wins, losses);
    }

    private int extractInt(String json, String key) {
        String search = "\"" + key + "\":";
        int idx = json.indexOf(search);
        if (idx == -1) return 0;
        idx += search.length();
        StringBuilder sb = new StringBuilder();
        while (idx < json.length() && (Character.isDigit(json.charAt(idx)) || json.charAt(idx) == '-')) {
            sb.append(json.charAt(idx));
            idx++;
        }
        try {
            return Integer.parseInt(sb.toString());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
