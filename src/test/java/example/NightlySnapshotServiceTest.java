package example;

import example.domain.NightlySnapshotService;
import java.util.List;

public final class NightlySnapshotServiceTest {
    public static void main(String[] args) {
        var service = new NightlySnapshotService(null);
        var tenants = List.of(new NightlySnapshotService.Tenant("a", "ACTIVE", "pro"), new NightlySnapshotService.Tenant("b", "ONBOARDING", "starter"), new NightlySnapshotService.Tenant("c", "SUSPENDED", "pro"));
        if (service.activeTenants(tenants).size() != 1 || !"a".equals(service.activeTenants(tenants).get(0).id())) throw new AssertionError("lifecycle filter changed");
        System.out.println("active tenant decision: PASS");
    }
}
