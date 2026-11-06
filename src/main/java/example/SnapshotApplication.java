package example;

import example.config.SnapshotConfig;
import example.domain.NightlySnapshotService;
import example.domain.NightlySnapshotService.Tenant;
import example.infra.InfraiClient;
import java.util.List;

public final class SnapshotApplication {
    public static void main(String[] args) {
        SnapshotConfig config = SnapshotConfig.fromEnvironment();
        var service = new NightlySnapshotService(new InfraiClient(config));
        String key = service.run(List.of(new Tenant("school-a", "ACTIVE", "pro"), new Tenant("school-b", "ONBOARDING", "starter")));
        System.out.println("snapshot stored: " + key);
    }
}
