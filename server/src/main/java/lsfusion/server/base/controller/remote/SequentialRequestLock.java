package lsfusion.server.base.controller.remote;

import com.google.common.base.Throwables;
import lsfusion.server.base.controller.stack.ExecutionStackAspect;
import lsfusion.server.physics.admin.log.ServerLoggers;
import org.thavam.util.concurrent.BlockingHashMap;
import org.thavam.util.concurrent.BlockingMap;

import java.util.concurrent.ArrayBlockingQueue;

public class SequentialRequestLock {

    private static final Object LOCK_OBJECT = new Object();
    private long waitingForRequestIndex = 0;

    public void acquireRequestLock(String ownerSID, long requestIndex) {
        ServerLoggers.pausableLog("Acquiring request lock for " + ownerSID + " for request #" + requestIndex);
        try {
            ExecutionStackAspect.take(() -> {
                synchronized (LOCK_OBJECT) {
                    while (requestIndex != waitingForRequestIndex)
                        LOCK_OBJECT.wait();
                }
            });
            ServerLoggers.pausableLog("Acquired request lock for " + ownerSID + " for request #" + requestIndex);
        } catch (InterruptedException e) {
            ServerLoggers.pausableLog("Interrupted request lock for " + ownerSID + " for request #" + requestIndex);
            throw Throwables.propagate(e);
        }
    }

    public void releaseRequestLock(String ownerSID, long requestIndex) {
        ServerLoggers.pausableLog("Releasing request lock for " + ownerSID + " for request #" + requestIndex);
        try {
            ExecutionStackAspect.take(() -> {
                synchronized (LOCK_OBJECT) {
                    waitingForRequestIndex++;
                    LOCK_OBJECT.notifyAll();
                }
            });
        } catch (InterruptedException e) {
            ServerLoggers.pausableLog("Interrupted request lock for " + ownerSID + " for request #" + requestIndex);
            throw Throwables.propagate(e);
        }
    }
}
