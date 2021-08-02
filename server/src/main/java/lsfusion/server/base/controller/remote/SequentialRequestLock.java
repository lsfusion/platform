package lsfusion.server.base.controller.remote;

import com.google.common.base.Throwables;
import lsfusion.server.base.controller.stack.ExecutionStackAspect;
import lsfusion.server.physics.admin.log.ServerLoggers;
import org.thavam.util.concurrent.BlockingHashMap;
import org.thavam.util.concurrent.BlockingMap;

import java.util.concurrent.ArrayBlockingQueue;

public class SequentialRequestLock {

    private static final Object LOCK_OBJECT = new Object();

    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
    private ArrayBlockingQueue requestLock = new ArrayBlockingQueue(1, true);

    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
    private BlockingMap<Long, Object> sequentialRequestLock = new BlockingHashMap<>();

    public SequentialRequestLock() {
        initRequestLock();
    }

    private void initRequestLock() {
        try {
            sequentialRequestLock.offer(0L, LOCK_OBJECT);
            requestLock.put(LOCK_OBJECT);
        } catch (InterruptedException e) {
            throw Throwables.propagate(e);
        }
    }

    public void acquireRequestLock(String ownerSID, long requestIndex) {
        ServerLoggers.pausableLog("Acquiring request lock for " + ownerSID + " for request #" + requestIndex);
        try {
            if (requestIndex >= 0)
                ExecutionStackAspect.take(() -> sequentialRequestLock.take(requestIndex));
            ExecutionStackAspect.take(requestLock);

            ServerLoggers.pausableLog("Acquired request lock for " + ownerSID + " for request #" + requestIndex);
        } catch (InterruptedException e) {
            ServerLoggers.pausableLog("Interrupted request lock for " + ownerSID + " for request #" + requestIndex);
            throw Throwables.propagate(e);
        }
    }

    public void releaseRequestLock(String ownerSID, long requestIndex) {
        ServerLoggers.pausableLog("Releasing request lock for " + ownerSID + " for request #" + requestIndex);
        try {
            requestLock.put(LOCK_OBJECT);
            if (requestIndex >= 0) {
                sequentialRequestLock.offer(requestIndex + 1, LOCK_OBJECT);
            }
            ServerLoggers.pausableLog("Released request lock for " + ownerSID + " for request #" + requestIndex);
        } catch (InterruptedException e) {
            throw Throwables.propagate(e);
        }
    }
}
