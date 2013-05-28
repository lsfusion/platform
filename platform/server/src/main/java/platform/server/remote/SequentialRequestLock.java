package platform.server.remote;

import com.google.common.base.Throwables;
import org.apache.log4j.Logger;
import org.thavam.util.concurrent.BlockingHashMap;
import org.thavam.util.concurrent.BlockingMap;
import platform.server.ServerLoggers;

import java.util.concurrent.ArrayBlockingQueue;

public class SequentialRequestLock {
    private final static Logger logger = ServerLoggers.pausablesInvocationLogger;

    private static final Object LOCK_OBJECT = new Object();

    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
    private ArrayBlockingQueue requestLock = new ArrayBlockingQueue(1, true);

    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
    private BlockingMap<Long, Object> sequentialRequestLock = new BlockingHashMap<Long, Object>();

    public SequentialRequestLock() {
        initRequestLock();
    }

    private void initRequestLock() {
        try {
            sequentialRequestLock.offer(0L, LOCK_OBJECT);
            requestLock.put(LOCK_OBJECT);
        } catch (InterruptedException e) {
            Throwables.propagate(e);
        }
    }

    public void acquireRequestLock(String ownerSID, long requestIndex) {
        logger.debug("Acquiring request lock for " + ownerSID + " for request #" + requestIndex);
        try {
            if (requestIndex >= 0) {
                sequentialRequestLock.take(requestIndex);
            }
            requestLock.take();
            logger.debug("Acquired request lock for " + ownerSID + " for request #" + requestIndex);
        } catch (InterruptedException e) {
            Throwables.propagate(e);
        }
    }

    public void releaseRequestLock(String ownerSID, long requestIndex) {
        logger.debug("Releasing request lock for " + ownerSID + " for request #" + requestIndex);
        try {
            requestLock.put(LOCK_OBJECT);
            if (requestIndex >= 0) {
                sequentialRequestLock.offer(requestIndex + 1, LOCK_OBJECT);
            }
            logger.debug("Released request lock for " + ownerSID + " for request #" + requestIndex);
        } catch (InterruptedException e) {
            Throwables.propagate(e);
        }
    }
}
