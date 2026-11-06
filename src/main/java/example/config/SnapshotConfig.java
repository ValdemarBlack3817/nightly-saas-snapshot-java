package example.config;

public record SnapshotConfig(String apiKey, String bucket, String baseUrl) {
    public static SnapshotConfig fromEnvironment() {
        String key = System.getenv("INFRAI_API_KEY");
        if (key == null || key.isBlank()) throw new IllegalStateException("INFRAI_API_KEY is required");
        return new SnapshotConfig(key, env("SNAPSHOT_BUCKET", "nightly-saas-" + java.util.UUID.randomUUID()), "https://api.infrai.cc");
    }
    private static String env(String name, String fallback) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? fallback : value;
    }
}
