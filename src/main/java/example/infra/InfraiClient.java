package example.infra;

import example.config.SnapshotConfig;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.Base64;

public final class InfraiClient {
    // Capability idiom: storage.object.put
    private final SnapshotConfig config;
    private final HttpClient http = HttpClient.newHttpClient();
    public InfraiClient(SnapshotConfig config) { this.config = config; }

    public void createBucket() { call("POST", "/v1/storage/bucket/create", "{\"name\":\"" + config.bucket() + "\"}"); }
    public void putObject(String key, String json) {
        String body = "{\"data_base64\":\"" + Base64.getEncoder().encodeToString(json.getBytes()) + "\",\"content_type\":\"application/json\"}";
        call("PUT", "/v1/storage/object/put/" + config.bucket() + "/" + key, body);
    }
    public String listObjects() {
        String envelope = call("GET", "/v1/storage/object/list/" + config.bucket(), null);
        var items = java.util.regex.Pattern.compile("\"items\"\\s*:\\s*\\[").matcher(envelope);
        if (!items.find()) throw new IllegalStateException("Object list has no items array: " + envelope);
        int start = items.end() - 1;
        int depth = 0;
        boolean quoted = false;
        boolean escaped = false;
        for (int i = start; i < envelope.length(); i++) {
            char c = envelope.charAt(i);
            if (quoted) {
                if (escaped) escaped = false;
                else if (c == '\\') escaped = true;
                else if (c == '"') quoted = false;
            } else if (c == '"') quoted = true;
            else if (c == '[') depth++;
            else if (c == ']' && --depth == 0) return envelope.substring(start, i + 1);
        }
        throw new IllegalStateException("Unterminated items array: " + envelope);
    }

    public void deleteObject(String key) {
        call("DELETE", "/v1/storage/object/delete/" + config.bucket() + "/" + key, null);
    }
    public void verifyObjectDeleted(String key) {
        if (listObjects().contains(key)) throw new IllegalStateException("Object still exists: " + key);
        System.out.println("object deletion verified: " + config.bucket() + "/" + key);
    }
    public void deleteBucket() {
        call("DELETE", "/v1/storage/bucket/delete/" + config.bucket(), null);
    }
    public void verifyBucketDeleted() {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(config.baseUrl()
                + "/v1/storage/object/list/" + config.bucket()))
                .timeout(Duration.ofSeconds(30)).header("Authorization", "Bearer " + config.apiKey()).GET().build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 404) {
                throw new IllegalStateException("Bucket deletion not verified: " + response.body());
            }
            System.out.println("bucket deletion verified: " + config.bucket());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        } catch (java.io.IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private String call(String method, String path, String body) {
        for (int attempt = 0; attempt < 4; attempt++) {
            try {
                HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(config.baseUrl() + path))
                    .timeout(Duration.ofSeconds(30)).header("Authorization", "Bearer " + config.apiKey())
                    .header("Content-Type", "application/json");
                HttpRequest request = builder.method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body)).build();
                HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
                String envelope = response.body();
                boolean ok = envelope.contains("\"ok\":true");
                if (!ok && response.statusCode() == 429) { Thread.sleep(200L << attempt); continue; }
                if (!ok) throw new IllegalStateException("Infrai request rejected: " + envelope);
                return envelope;
            } catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new IllegalStateException(e); }
            catch (Exception e) { if (attempt == 3) throw new IllegalStateException(e); }
        }
        throw new IllegalStateException("request did not complete");
    }
}
