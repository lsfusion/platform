package lsfusion.server.base.controller.remote;

import com.google.common.base.Throwables;
import lsfusion.server.base.controller.stack.ExecutionStackAspect;
import lsfusion.server.physics.admin.log.ServerLoggers;

public class SequentialRequestLock {

    private final Object LOCK_OBJECT = new Object();
    private long waitingForRequestIndex = 0;

    public void blockRequestLock(String ownerSID, long requestIndex, RemoteRequestObject remoteObject) {
        ServerLoggers.pausableLog("Acquiring request lock for " + ownerSID + " for request #" + requestIndex);
        try {
            ExecutionStackAspect.take(remoteObject, () -> {
                synchronized (LOCK_OBJECT) {
                    while (requestIndex != waitingForRequestIndex)
                        LOCK_OBJECT.wait();
                }
            });
            ServerLoggers.pausableLog("Acquired request lock for " + ownerSID + " for request #" + requestIndex);
        } catch (Throwable e) {
            ServerLoggers.pausableLog("Interrupted request lock for " + ownerSID + " for request #" + requestIndex);
            throw Throwables.propagate(e);
        }
    }

    public void releaseRequestLock(String ownerSID, long requestIndex, RemoteRequestObject remoteObject) {
        ServerLoggers.pausableLog("Releasing request lock for " + ownerSID + " for request #" + requestIndex);
        try {
            ExecutionStackAspect.take(remoteObject, () -> {
                synchronized (LOCK_OBJECT) {
                    waitingForRequestIndex++;
                    LOCK_OBJECT.notifyAll();
                }
            });
        } catch (Throwable e) {
            ServerLoggers.pausableLog("Interrupted request lock for " + ownerSID + " for request #" + requestIndex);
            throw Throwables.propagate(e);
        }
    }
}
