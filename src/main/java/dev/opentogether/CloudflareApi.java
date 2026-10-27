package dev.opentogether;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

final class CloudflareApi {
    static final Logger LOG = LogUtils.getLogger();
    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(6)).build();
    // Configure with JVM flag: -Dopentogether.api=https://<worker>.workers.dev
    static String base() { return System.getProperty("opentogether.api", "").replaceAll("/+$", ""); }
    static boolean configured() { try { var u = URI.create(base()); return "https".equalsIgnoreCase(u.getScheme()) && u.getHost() != null && u.getUserInfo() == null; } catch (IllegalArgumentException e) { return false; } }
    static CompletableFuture<JsonObject> call(String method, String route, String body) {
        if (!configured()) return CompletableFuture.failedFuture(new IllegalStateException("Configure -Dopentogether.api=https://..."));
        HttpRequest.Builder b=HttpRequest.newBuilder(URI.create(base()+route)).timeout(Duration.ofSeconds(12));
        b.header("Accept","application/json");
        if (method.equals("POST"))b.header("Content-Type","application/json").POST(HttpRequest.BodyPublishers.ofString(body,StandardCharsets.UTF_8));
        else b.GET();
        return HTTP.sendAsync(b.build(),HttpResponse.BodyHandlers.ofString()).thenApply(r->{
            LOG.info("[OpenTogether] API method={} operation={} status={}",method,route.startsWith("/create") ? "create" : "resolve",r.statusCode());
            if(r.statusCode()<200||r.statusCode()>299)throw new IllegalStateException("OpenTogether API returned HTTP "+r.statusCode());
            return JsonParser.parseString(r.body()).getAsJsonObject();
        });
    }
}
