package lsfusion.server.physics.admin.oauth;

import lsfusion.server.data.value.DataObject;
import lsfusion.server.physics.admin.log.ServerLoggers;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * In-memory store for OAuth authorization codes — short-lived, single-use tokens minted at
 * {@code /oauth/authorize} (after the user clicks Approve) and consumed at
 * {@code /oauth/token} a few seconds later.
 *
 * <p>Why in-memory and not lsFusion class storage like the rest of the OAuth schema:
 *
 * <ul>
 *   <li><strong>TTL is 10 minutes by design</strong> — a brief handoff between two HTTP
 *     calls. There's no audit value in keeping the row past consumption, and there's no
 *     real cost to losing in-flight codes on app-server restart (the user just clicks
 *     Approve again, worst case a few minutes of redo).</li>
 *   <li><strong>Single-use is atomic</strong> — {@code ConcurrentHashMap.remove} returns
 *     the value or null without race conditions, so two simultaneous {@code /token}
 *     exchanges of the same code can't both succeed.</li>
 *   <li><strong>Schema stays clean</strong> — admin forms see only persistent OAuth state
 *     (clients, refresh tokens), not an ephemeral row that flickers in and out every few
 *     seconds.</li>
 *   <li><strong>Standard practice</strong> — Auth0, Keycloak, Cognito all keep auth codes
 *     in cache (Redis or in-process), not in their relational schema.</li>
 * </ul>
 *
 * <p>Implementation: a {@link ConcurrentHashMap} keyed by the opaque code, with a single
 * background scheduler that sweeps expired entries once a minute. Eviction is also lazy on
 * read — {@link #consume(String)} discards expired codes inline so there's no "consumed
 * just before sweep" race window.
 *
 * <p><strong>Deployment constraint:</strong> {@code /oauth/authorize} (which calls
 * {@link #put}) and {@code /oauth/token} (which calls {@link #consume}) must hit the same
 * app-server JVM. lsFusion's canonical deployment is single-app-server-instance with many
 * web-clients sharing it via RMI, so this is the natural fit. Multi-app-server HA setups
 * would need either sticky routing on {@code code} or a shared cache (Redis) — neither is
 * MVP scope.
 *
 * <p>Lifecycle: instance-scoped, owned by {@link OAuthDispatcher}. The sweeper thread is
 * a daemon, so it dies cleanly on JVM exit; no explicit shutdown is required for the
 * canonical lsFusion deployment (one process per app-server, restart = full JVM
 * restart). {@link #shutdown()} is exposed for tests that spin up many instances.
 */
public class OAuthAuthCodeManager {

    /**
     * Background eviction frequency. Auth codes have a 10-min TTL, so a one-minute sweep
     * is fine-grained enough to keep memory usage bounded (worst case 1 min of expired-but-
     * not-yet-evicted entries) while staying lightweight.
     */
    private static final long SWEEP_INTERVAL_SECONDS = 60;

    private final ConcurrentHashMap<String, Entry> store = new ConcurrentHashMap<>();
    private final ScheduledExecutorService sweeper;

    public OAuthAuthCodeManager() {
        sweeper = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "oauth-authcode-sweeper");
            t.setDaemon(true);
            return t;
        });
        sweeper.scheduleAtFixedRate(this::sweepExpired,
                SWEEP_INTERVAL_SECONDS, SWEEP_INTERVAL_SECONDS, TimeUnit.SECONDS);
    }

    /**
     * Stop the background sweeper. Not called from production code (the daemon thread dies
     * with the JVM), but useful for tests that spin up many manager instances.
     */
    public void shutdown() {
        sweeper.shutdownNow();
    }

    /**
     * Store a freshly minted code, bound to (client, user, redirect_uri, code_challenge)
     * with the given TTL. Caller has already generated the code string and is responsible
     * for not generating duplicates (we're using cryptographic random so collisions are
     * astronomically improbable, but {@code put} would silently overwrite if it happens).
     */
    public void put(String code, DataObject client, DataObject user,
                    String codeChallenge, String redirectURI, long expiresAtMillis) {
        store.put(code, new Entry(client, user, codeChallenge, redirectURI, expiresAtMillis));
    }

    /**
     * Atomic single-use consume: returns the entry and removes it from the store, or
     * returns null if the code is unknown or expired. Removal is unconditional (even on
     * expired hits) so two concurrent /token requests can't both succeed even on a
     * just-expired code.
     */
    public Entry consume(String code) {
        Entry e = store.remove(code);
        if (e == null) return null;
        if (e.expiresAtMillis < System.currentTimeMillis()) return null;
        return e;
    }

    private void sweepExpired() {
        try {
            long now = System.currentTimeMillis();
            store.entrySet().removeIf(e -> e.getValue().expiresAtMillis < now);
        } catch (Throwable t) {
            // Sweep is best-effort — never let a transient failure kill the scheduler thread.
            ServerLoggers.systemLogger.warn("OAuth auth-code sweep failed", t);
        }
    }

    /**
     * Bound state for one auth code. Carries everything {@code /oauth/token} needs to
     * verify and mint the access/refresh pair: the FK objects (client, user) for direct
     * use in DataSession writes, the PKCE challenge for verifier comparison, and the
     * redirect_uri for the §4.1.3 binding check.
     */
    public static final class Entry {
        public final DataObject client;
        public final DataObject user;
        public final String codeChallenge;
        public final String redirectURI;
        public final long expiresAtMillis;

        Entry(DataObject client, DataObject user, String codeChallenge, String redirectURI, long expiresAtMillis) {
            this.client = client;
            this.user = user;
            this.codeChallenge = codeChallenge;
            this.redirectURI = redirectURI;
            this.expiresAtMillis = expiresAtMillis;
        }
    }
}
