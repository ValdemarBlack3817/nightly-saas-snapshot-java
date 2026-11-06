package example.domain;

import example.infra.InfraiClient;
import java.time.LocalDate;
import java.util.*;

public final class NightlySnapshotService {
    public record Tenant(String id, String accountState, String plan) {}
    private final InfraiClient client;
    public NightlySnapshotService(InfraiClient client) { this.client = client; }
    public List<Tenant> activeTenants(List<Tenant> tenants) {
        return tenants.stream().filter(t -> "ACTIVE".equals(t.accountState())).toList();
    }
    public String run(List<Tenant> tenants) {
        client.createBucket();
        List<Tenant> active = activeTenants(tenants);
        List<String> keys = new ArrayList<>();
        try {
            for (Tenant tenant : active) {
                String key = "tenants/" + tenant.id() + "/" + LocalDate.now() + ".json";
                keys.add(key);
                client.putObject(key, "{\"active_tenants\":1}");
            }
            client.listObjects();
            return String.join(", ", keys);
        } finally {
            try {
                for (String key : keys) {
                    client.deleteObject(key);
                    client.verifyObjectDeleted(key);
                }
            } finally {
                client.deleteBucket();
                client.verifyBucketDeleted();
            }
        }
    }
}
