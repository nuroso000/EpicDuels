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
    private final String apiKey;    // server-only credential
    private final String table;     // default "player_stats"
    private final HttpClient http;
    private final Logger logger;

    public SupabaseProvider(String url, String apiKey, String table, Logger logger) {
        this.baseUrl = url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
        this.apiKey = apiKey;
        if (!table.matches("[A-Za-z_][A-Za-z0-9_]*")) throw new IllegalArgumentException("Invalid stats table name");
        this.table = table;
        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.logger = logger;
        testConnection();
    }

    private void testConnection() {
        String uri = baseUrl + "/rest/v1/" + table + "?select=uuid&limit=1";
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(uri))
                .header("apikey", apiKey)
                .header("Authorization", "Bearer " + apiKey)
                .header("Accept", "application/json")
                .GET()
                .timeout(Duration.ofSeconds(10))
                .build();
        http.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    if (response.statusCode() == 200) {
                        logger.info("[Supabase] Connection test OK — table '" + table + "' is reachable.");
                    } else {
                        logger.severe("[Supabase] Connection test FAILED (HTTP " + response.statusCode() + "): " + response.body());
                        logger.severe("[Supabase] Verify server credentials and least-privilege RLS policies; do not disable RLS.");
                    }
                })
                .exceptionally(ex -> {
                    logger.severe("[Supabase] Connection test FAILED — cannot reach " + baseUrl + ": " + ex.getMessage());
                    return null;
                });
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
    public CompletableFuture<Void> recordResult(UUID eventId, UUID playerId, boolean win) {
        com.google.gson.JsonObject data = new com.google.gson.JsonObject();
        data.addProperty("p_event_id", eventId.toString());
        data.addProperty("p_player_id", playerId.toString());
        data.addProperty("p_win", win);
        HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + "/rest/v1/rpc/" + table + "_record_result"))
                .header("apikey", apiKey).header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json").timeout(Duration.ofSeconds(10))
                .POST(HttpRequest.BodyPublishers.ofString(data.toString())).build();
        return http.sendAsync(request, HttpResponse.BodyHandlers.ofString()).thenAccept(response -> {
            if (response.statusCode() < 200 || response.statusCode() >= 300)
                throw new IllegalStateException("Supabase result RPC HTTP " + response.statusCode()
                        + "; verify the server credentials and installed result function.");
        });
    }

    /**
     * Parse the JSON array returned by Supabase. We expect either [] or [{"wins":X,"losses":Y}].
     * Minimal JSON parsing without external libraries.
     */
    private PlayerStats parseResponse(String json) {
        com.google.gson.JsonElement parsed = com.google.gson.JsonParser.parseString(json);
        com.google.gson.JsonArray rows = parsed.getAsJsonArray();
        if (rows.isEmpty()) return null;
        com.google.gson.JsonObject data = rows.get(0).getAsJsonObject();
        int wins = data.has("wins") && !data.get("wins").isJsonNull() ? data.get("wins").getAsInt() : 0;
        int losses = data.has("losses") && !data.get("losses").isJsonNull() ? data.get("losses").getAsInt() : 0;
        if (wins < 0 || losses < 0) throw new IllegalArgumentException("Negative remote statistics");
        return new PlayerStats(wins, losses);
    }
}
