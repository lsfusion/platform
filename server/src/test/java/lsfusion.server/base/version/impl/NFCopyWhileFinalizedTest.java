package lsfusion.server.base.version.impl;

import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.server.base.version.Version;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

// a form built from another one (FORMS) keeps copies of the other's collections, which read it as they are finalized - and
// the start finalizes forms in parallel chunks, so the source can be finalized by its own chunk right then. A copy that
// had checked the source was not final yet read its changes again after the source's swap, and the start failed with
// "NF COLLECTION RESTARTED". The source here holds the copy's read at one of its checks for being final, finalizes
// meanwhile, and lets the read go on : it has to read what the source holds
public class NFCopyWhileFinalizedTest {

    // the first check, before the read looks at the changes again
    @Test
    public void aCopyReadsItsSourceFinalizedAfterItsCheck() throws Exception {
        readWhileFinalized(1);
    }

    // the third, inside the look at the changes the read goes through - between its check and what it returns
    @Test
    public void aCopyReadsItsSourceFinalizedInsideItsLook() throws Exception {
        readWhileFinalized(3);
    }

    // a write the read's mapping makes into its source, finished meanwhile, is no restart of the read's own look : it
    // fails as before, rather than the read being made again past it
    @Test
    public void aWriteInsideTheReadFailsAsBefore() throws Exception {
        failsInsideTheRead(source -> source.add("late", "value", Version.global()));
    }

    // and so does a read the mapping makes of its source : made again, the copy's read would find the source final and
    // skip the rest of the mapping - a write elsewhere here
    @Test
    public void aReadInsideTheReadFailsAsBefore() throws Exception {
        NFMapImpl<String, String> elsewhere = new NFMapImpl<>();
        failsInsideTheRead(source -> elsewhere.add("late", source.getNFValue("key", Version.last()), Version.global()));
    }

    // the copy's read maps its source's own source, and the mapping, the source finished meanwhile, does `inside` with
    // it : the read has to fail with what it met there
    private static void failsInsideTheRead(Consumer<NFMapImpl<String, String>> inside) throws Exception {
        NFMapImpl<String, String> other = new NFMapImpl<>();
        other.add("key", "value", Version.global());
        AtomicReference<Thread> reader = new AtomicReference<>();
        CountDownLatch mapped = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        NFMapImpl<String, String> source = new NFMapImpl<>();
        source.add(other, value -> {
            if (Thread.currentThread() == reader.get() && mapped.getCount() > 0) {
                mapped.countDown();
                try {
                    release.await(1, TimeUnit.MINUTES);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                inside.accept(source); // the source is finished by now
            }
            return value;
        }, Version.global());
        NFMapImpl<String, String> copy = new NFMapImpl<>();
        copy.add(source, Function.identity(), Version.global());

        ExecutorService threads = Executors.newFixedThreadPool(2);
        try {
            Future<ImMap<String, String>> reading = threads.submit(() -> {
                reader.set(Thread.currentThread());
                return copy.getMap();
            });
            assertTrue("the copy's read did not map its source in 30 seconds", mapped.await(30, TimeUnit.SECONDS));
            assertEquals(MapFact.singleton("key", "value"), result("the source's finalization", threads.submit(source::getMap)));
            release.countDown();

            try {
                fail("the copy's read went through its mapping's look at a finished source: " + reading.get(1, TimeUnit.MINUTES));
            } catch (ExecutionException e) {
                // the failure met inside the mapping, not a restart the read is made again on
                assertTrue("the copy's read failed otherwise: " + e.getCause(), e.getCause().getClass() == RuntimeException.class
                        && e.getCause().getMessage().startsWith("NF COLLECTION RESTARTED"));
            }
        } finally {
            threads.shutdownNow();
            threads.awaitTermination(1, TimeUnit.MINUTES);
        }
    }

    private static void readWhileFinalized(int check) throws Exception {
        HoldingMap source = new HoldingMap(check);
        source.add("key", "value", Version.global());
        NFMapImpl<String, String> copy = new NFMapImpl<>();
        copy.add(source, Function.identity(), Version.global());

        ExecutorService threads = Executors.newFixedThreadPool(2);
        try {
            Future<ImMap<String, String>> reading = threads.submit(() -> {
                source.reader = Thread.currentThread();
                return copy.getMap(); // the copy's finalization reads the source
            });
            assertTrue("the copy did not read its source in 30 seconds", source.checked.await(30, TimeUnit.SECONDS));

            ImMap<String, String> finalized = result("the source's finalization", threads.submit(source::getMap));
            source.release.countDown(); // the read goes on, past the source's swap

            ImMap<String, String> copied = result("the copy's read", reading);
            assertFalse("the copy's read was let go after a minute, not by the test", source.expired);
            assertEquals(MapFact.singleton("key", "value"), finalized);
            assertEquals(MapFact.singleton("key", "value"), copied);
        } finally {
            threads.shutdownNow();
            threads.awaitTermination(1, TimeUnit.MINUTES);
        }
    }

    // a source whose check for being final, the given one the reading thread makes, waits until the test lets it go
    private static class HoldingMap extends NFMapImpl<String, String> {
        private final int check;
        private int checks;
        private volatile Thread reader;
        private final CountDownLatch checked = new CountDownLatch(1);
        private final CountDownLatch release = new CountDownLatch(1);
        private volatile boolean expired;

        private HoldingMap(int check) {
            this.check = check;
        }

        @Override
        protected boolean checkFinal(Object object) {
            boolean isFinal = super.checkFinal(object);
            if (Thread.currentThread() == reader && ++checks == check) {
                checked.countDown();
                try {
                    if (!release.await(1, TimeUnit.MINUTES))
                        expired = true;
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
            return isFinal;
        }
    }

    private static ImMap<String, String> result(String operation, Future<ImMap<String, String>> future) throws InterruptedException {
        try {
            return future.get(1, TimeUnit.MINUTES);
        } catch (ExecutionException e) {
            fail(operation + " failed: " + e.getCause());
        } catch (TimeoutException e) {
            fail(operation + " was not done in a minute");
        }
        return null;
    }
}
