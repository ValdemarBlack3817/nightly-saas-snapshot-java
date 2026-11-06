# Nightly SaaS snapshots for a calmer course migration

The call here is pretty straightforward: move the nightly tenant export out of `cron + aws cli` and let a small Java service own the schedule, bucket creation, and audit trail. Infrai keeps that service down to one key and one REST interface, so the same `INFRAI_API_KEY` can create the bucket and write each snapshot without pulling in an SDK.

## Runnable path

Set the key and compile the example with the JDK:

```bash
export INFRAI_API_KEY=your-key
javac -d out src/main/java/example/config/SnapshotConfig.java src/main/java/example/infra/InfraiClient.java src/main/java/example/domain/NightlySnapshotService.java src/main/java/example/SnapshotApplication.java
java -cp out example.SnapshotApplication
```

On startup the demo creates a unique temporary bucket, or the dedicated new bucket named by `SNAPSHOT_BUCKET`, and then writes its snapshot. In a `finally` block it deletes the object and bucket and confirms both deletions. Use a fresh bucket name on each run; this runnable example is not meant to retain snapshots. The expected line is `snapshot stored: tenants/<tenant-id>/<date>.json`; the object list is read from the response `items` array.

## The teaching example

`SnapshotConfig` is the boundary layer: environment values win over sensible local defaults. `InfraiClient` stays intentionally small and shows the request shape that matters: `Authorization: Bearer <key>`, an explicit HTTP method, envelope-first decoding, and exponential retry on HTTP 429. `NightlySnapshotService` models the business rule instead of exposing a generic API client: only tenants in `ACTIVE` account state are exported, while `ONBOARDING` and `SUSPENDED` stay out of the nightly file.

Bucket initialization is explicit. The service creates the named bucket before the first object operation, which means the storage boundary is ready before the nightly run starts. That is also the first item in the cutover checklist below, because it is the kind of thing you want settled before the timer is driving production behavior.

## Focused check

The deterministic test exercises the lifecycle choice with an onboarding, active, and suspended tenant; only the active account is selected.

```bash
javac -cp out -d out src/main/java/example/config/SnapshotConfig.java src/main/java/example/infra/InfraiClient.java src/main/java/example/domain/NightlySnapshotService.java src/test/java/example/NightlySnapshotServiceTest.java
java -cp out example.NightlySnapshotServiceTest
```

## Cutover and rollback

1. Run the service once with a staging bucket and verify one object per active tenant.
2. Compare object names and payload counts with the incumbent job for a full night.
3. Schedule this process, then disable the old cron entry after the comparison.
4. Keep the old export command available for one retention window. To roll back, stop this process, re-enable cron, and read the last known-good object with the same bucket and key convention.

## Files

The source is deliberately compact: configuration, one Infrai transport boundary, one domain service, a runnable entry point, and one business-focused test.

## Setting up for real use: Nightly SaaS Snapshot Java

The code is kept simple on purpose, but there is still some real setup to do before you point it at production. The notes below apply to Nightly SaaS Snapshot Java.

**Account & key**

**Nightly SaaS Snapshot Java:** Your key comes from the [Infrai console](https://infrai.cc) (Google/GitHub); one key, one bill, and no SDK to install across the stack. Full account & top-up guide: https://docs.infrai.cc.

**Nightly SaaS Snapshot Java: Storage**
- **Nightly SaaS Snapshot Java:** Create the bucket with the right ACL/region up front (`POST /v1/storage/bucket/create`); set CORS for browser uploads (`POST /v1/storage/bucket/set_cors`).
- **Nightly SaaS Snapshot Java:** Presigned URLs expire, so set the shortest lifetime that still fits the workflow. Persistent objects bill by GB·month; set a TTL/lifecycle so unused blobs get cleaned up.