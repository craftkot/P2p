package dev.opentogether;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

/** Strict, nonblocking Cloudflare code broker client. Game data never uses this channel. */
final class CloudflareApi {
    static final Logger LOG = LogUtils.getLogger();
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();

    private CloudflareApi() {}

    static boolean configured() {
        return !CloudflareConfig.getUrl().isBlank();
    }

    static CompletableFuture<JsonObject> call(String method, String route, String body) {
        String base = CloudflareConfig.getUrl();
        if (base.isEmpty())
            return CompletableFuture.failedFuture(
                    new IllegalStateException("Set an HTTPS Cloudflare URL in OpenTogether settings"));

        boolean create = "POST".equals(method) && "/create".equals(route);
        boolean resolve = "GET".equals(method) && route.matches("/resolve/[A-HJ-NP-Z2-9]{4}-[A-HJ-NP-Z2-9]{4}");
        if (!create && !resolve)
            return CompletableFuture.failedFuture(new IllegalArgumentException("Invalid API operation"));

        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(base + route))
                    .timeout(Duration.ofSeconds(10))
                    .header("Accept", "application/json");
            if (create) {
                builder.header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8));
            } else {
                builder.GET();
            }

            final String operation = create ? "create" : "resolve";
            return HTTP.sendAsync(builder.build(), HttpResponse.BodyHandlers.ofString())
                    .thenApply(response -> {
                        LOG.info("[OpenTogether] Cloudflare operation={} status={}",
                                operation, response.statusCode());
                        if (response.statusCode() < 200 || response.statusCode() >= 300)
                            throw new IllegalStateException("Cloudflare HTTP " + response.statusCode());
                        if (response.body().length() > 8192)
                            throw new IllegalStateException("Cloudflare response too large");
                        JsonObject result = JsonParser.parseString(response.body()).getAsJsonObject();
                        if ((create && !result.has("code")) || (resolve && !result.has("endpoint")))
                            throw new IllegalStateException("Malformed Cloudflare response");
                        return result;
                    });
        } catch (RuntimeException ex) {
            return CompletableFuture.failedFuture(ex);
        }
    }
}
