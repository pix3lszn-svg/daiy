package dev.dailyemerald.perks.patreon;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * Minimal Patreon v2 API client. Pure Java (no Bukkit imports) so it can be
 * unit-tested off-server. Uses the bundled Gson from the Paper runtime.
 *
 * Docs: https://docs.patreon.com/#apiv2-resources
 */
public final class PatreonClient {

    private static final String API = "https://www.patreon.com/api/oauth2/v2";
    private static final String TOKEN_URL = "https://www.patreon.com/api/oauth2/token";

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    private volatile String accessToken;
    private volatile String refreshToken;
    private final String clientId;
    private final String clientSecret;
    private volatile String campaignId;
    /** Called with (accessToken, refreshToken) after a successful refresh so they can be persisted. */
    private final BiConsumer<String, String> tokenSaver;

    public PatreonClient(String accessToken, String refreshToken, String clientId, String clientSecret,
                         String campaignId, BiConsumer<String, String> tokenSaver) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.campaignId = campaignId == null ? "" : campaignId;
        this.tokenSaver = tokenSaver;
    }

    public boolean isConfigured() {
        return accessToken != null && !accessToken.isBlank();
    }

    /** Fetches every member of the campaign that currently has an active pledge. Blocking — call async. */
    public List<PatreonMember> fetchActiveMembers() throws PatreonException {
        if (!isConfigured()) {
            throw new PatreonException("No Patreon creator access token configured (see config.yml).");
        }
        String campaign = resolveCampaignId();
        String url = API + "/campaigns/" + campaign + "/members"
                + "?include=" + enc("currently_entitled_tiers")
                + "&fields" + enc("[member]") + "=" + enc("email,full_name,patron_status")
                + "&fields" + enc("[tier]") + "=" + enc("title")
                + "&page" + enc("[count]") + "=200";

        List<PatreonMember> active = new ArrayList<>();
        int pages = 0;
        while (url != null && pages++ < 50) { // safety bound: 10k members
            Page page = parseMembersPage(get(url));
            for (PatreonMember m : page.members()) {
                if (m.isActivePatron()) active.add(m);
            }
            url = page.nextUrl();
        }
        return active;
    }

    private String resolveCampaignId() throws PatreonException {
        String id = campaignId;
        if (id != null && !id.isBlank()) return id;
        String body = get(API + "/campaigns?fields" + enc("[campaign]") + "=" + enc("creation_name"));
        id = parseFirstCampaignId(body);
        if (id == null) {
            throw new PatreonException("The Patreon token works but has no campaign attached to it.");
        }
        campaignId = id;
        return id;
    }

    private String get(String url) throws PatreonException {
        String body = send(url, accessToken);
        if (body != null) return body;
        // 401 — try one token refresh, then retry once.
        refreshTokens();
        body = send(url, accessToken);
        if (body == null) {
            throw new PatreonException("Patreon rejected the access token even after refreshing it.");
        }
        return body;
    }

    /** Returns the response body, or null on 401 (so the caller can refresh). */
    private String send(String url, String token) throws PatreonException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .header("Authorization", "Bearer " + token)
                .header("User-Agent", "EmeraldPerks/1.0 (Paper plugin)")
                .timeout(Duration.ofSeconds(30))
                .GET()
                .build();
        HttpResponse<String> response;
        try {
            response = http.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new PatreonException("Could not reach the Patreon API: " + e.getMessage(), e);
        }
        int code = response.statusCode();
        if (code == 401) return null;
        if (code == 429) throw new PatreonException("Patreon rate limit hit (HTTP 429) — will retry on the next sync.");
        if (code < 200 || code >= 300) {
            throw new PatreonException("Patreon API returned HTTP " + code + ": " + truncate(response.body()));
        }
        return response.body();
    }

    private synchronized void refreshTokens() throws PatreonException {
        if (refreshToken == null || refreshToken.isBlank() || clientId.isBlank() || clientSecret.isBlank()) {
            throw new PatreonException("Patreon access token expired and no client-id/client-secret/refresh-token "
                    + "are configured for automatic renewal. Generate a fresh token in the Patreon developer portal.");
        }
        String form = "grant_type=refresh_token"
                + "&refresh_token=" + enc(refreshToken)
                + "&client_id=" + enc(clientId)
                + "&client_secret=" + enc(clientSecret);
        HttpRequest request = HttpRequest.newBuilder(URI.create(TOKEN_URL))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("User-Agent", "EmeraldPerks/1.0 (Paper plugin)")
                .timeout(Duration.ofSeconds(30))
                .POST(HttpRequest.BodyPublishers.ofString(form))
                .build();
        HttpResponse<String> response;
        try {
            response = http.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new PatreonException("Could not reach Patreon to refresh the token: " + e.getMessage(), e);
        }
        if (response.statusCode() != 200) {
            throw new PatreonException("Patreon token refresh failed (HTTP " + response.statusCode() + ").");
        }
        JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
        this.accessToken = json.get("access_token").getAsString();
        if (json.has("refresh_token")) {
            this.refreshToken = json.get("refresh_token").getAsString();
        }
        if (tokenSaver != null) tokenSaver.accept(accessToken, refreshToken);
    }

    // ------------------------------------------------------------------
    // JSON parsing — static so tests can feed fixture payloads.
    // ------------------------------------------------------------------

    public record Page(List<PatreonMember> members, String nextUrl) {}

    public static Page parseMembersPage(String json) {
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();

        // included[] carries the tier objects; map id -> title first.
        Map<String, String> tierTitles = new HashMap<>();
        if (root.has("included") && root.get("included").isJsonArray()) {
            for (JsonElement el : root.getAsJsonArray("included")) {
                JsonObject obj = el.getAsJsonObject();
                if ("tier".equals(str(obj, "type")) && obj.has("attributes")) {
                    String title = str(obj.getAsJsonObject("attributes"), "title");
                    if (title != null) tierTitles.put(str(obj, "id"), title);
                }
            }
        }

        List<PatreonMember> members = new ArrayList<>();
        JsonArray data = root.has("data") && root.get("data").isJsonArray()
                ? root.getAsJsonArray("data") : new JsonArray();
        for (JsonElement el : data) {
            JsonObject obj = el.getAsJsonObject();
            JsonObject attrs = obj.has("attributes") ? obj.getAsJsonObject("attributes") : new JsonObject();
            List<String> titles = new ArrayList<>();
            JsonObject relationships = obj.has("relationships") ? obj.getAsJsonObject("relationships") : null;
            if (relationships != null && relationships.has("currently_entitled_tiers")) {
                JsonObject rel = relationships.getAsJsonObject("currently_entitled_tiers");
                if (rel.has("data") && rel.get("data").isJsonArray()) {
                    for (JsonElement tierRef : rel.getAsJsonArray("data")) {
                        String title = tierTitles.get(str(tierRef.getAsJsonObject(), "id"));
                        if (title != null) titles.add(title);
                    }
                }
            }
            members.add(new PatreonMember(
                    str(obj, "id"),
                    lower(str(attrs, "email")),
                    str(attrs, "full_name"),
                    str(attrs, "patron_status"),
                    titles));
        }

        String next = null;
        if (root.has("links") && root.get("links").isJsonObject()) {
            next = str(root.getAsJsonObject("links"), "next");
        }
        return new Page(members, next);
    }

    public static String parseFirstCampaignId(String json) {
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        if (!root.has("data") || !root.get("data").isJsonArray()) return null;
        JsonArray data = root.getAsJsonArray("data");
        if (data.isEmpty()) return null;
        return str(data.get(0).getAsJsonObject(), "id");
    }

    private static String str(JsonObject obj, String key) {
        JsonElement el = obj.get(key);
        return el == null || el.isJsonNull() ? null : el.getAsString();
    }

    private static String lower(String s) {
        return s == null ? null : s.toLowerCase(java.util.Locale.ROOT);
    }

    private static String enc(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    private static String truncate(String s) {
        if (s == null) return "";
        return s.length() > 200 ? s.substring(0, 200) + "…" : s;
    }
}
