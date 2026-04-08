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
 * Stats provider backed by a Supabase project (PostgREST).
 *
 * Required table (run in SQL editor):
 * <pre>
 * CREATE TABLE IF NOT EXISTS player_stats (
 *   uuid  TEXT PRIMARY KEY,
 *   wins  INTEGER NOT NULL DEFAULT 0,
 *   losses INTEGER NOT NULL DEFAULT 0
 * );
 * </pre>
 */
public class SupabaseProvider implements StatsProvider {

    private final String baseUrl;   // e.g. https://abc.supabase.co
    private final String apiKey;    // anon / service-role key
    private final String table;     // default "player_stats"
    private final HttpClient http;
    private final Logger logger;

    public SupabaseProvider(String url, String apiKey, String table, Logger logger) {
        this.baseUrl = url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
        this.apiKey = apiKey;
        this.table = table;
        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.logger = logger;
    }

    @Override
    public CompletableFuture<PlayerStats> fetch(UUID playerId) {
        String uri = baseUrl + "/rest/v1/" + table + "?uuid=eq." + playerId + "&select=wins,losses";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(uri))
                .header("apikey", apiKey)
                .header("Authorization", "Bearer " + apiKey)
                .header("Accept", "application/json")
                .GET()
                .timeout(Duration.ofSeconds(10))
                .build();

        return http.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() != 200) {
                        logger.warning("[Supabase] GET failed (" + response.statusCode() + "): " + response.body());
                        return null;
                    }
                    return parseResponse(response.body());
                })
                .exceptionally(ex -> {
                    logger.warning("[Supabase] fetch error for " + playerId + ": " + ex.getMessage());
                    return null;
                });
    }

    @Override
    public CompletableFuture<Void> push(UUID playerId, PlayerStats stats) {
        String uri = baseUrl + "/rest/v1/" + table;
        String json = "{\"uuid\":\"" + playerId + "\",\"wins\":" + stats.getWins() + ",\"losses\":" + stats.getLosses() + "}";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(uri))
                .header("apikey", apiKey)
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .header("Prefer", "resolution=merge-duplicates")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .timeout(Duration.ofSeconds(10))
                .build();

        return http.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    if (response.statusCode() != 200 && response.statusCode() != 201) {
                        logger.warning("[Supabase] UPSERT failed (" + response.statusCode() + "): " + response.body());
                    }
                })
                .exceptionally(ex -> {
                    logger.warning("[Supabase] push error for " + playerId + ": " + ex.getMessage());
                    return null;
                });
    }

    /**
     * Parse the JSON array returned by Supabase. We expect either [] or [{"wins":X,"losses":Y}].
     * Minimal JSON parsing without external libraries.
     */
    private PlayerStats parseResponse(String json) {
        json = json.trim();
        // Empty array = no record
        if (json.equals("[]")) return null;

        // Strip outer [ ]
        if (json.startsWith("[")) json = json.substring(1);
        if (json.endsWith("]")) json = json.substring(0, json.length() - 1);
        json = json.trim();
        if (json.isEmpty()) return null;

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
