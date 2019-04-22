package lsfusion.client.controller.remote;

import lsfusion.base.Pair;
import lsfusion.client.base.log.ClientLoggers;
import lsfusion.client.controller.MainController;
import lsfusion.interop.action.ServerResponse;
import org.apache.log4j.Logger;

import java.rmi.RemoteException;

public abstract class RmiRequest<T> {
    private static final Logger logger = ClientLoggers.invocationLogger;

    private long requestIndex;
    private long lastReceivedRequestIndex;
    private String name;
    
    /**
     * first - power base;
     * second - offset coefficient
     */
    private Pair<Integer, Integer> timeoutParams;

    protected RmiRequest(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * first - power base;
     * second - offset coefficient
     */
    public void setTimeoutParams(Pair<Integer, Integer> timeoutParams) {
        this.timeoutParams = timeoutParams;
    }

    public Pair<Integer, Integer> getTimeoutParams() {
        return timeoutParams;
    }

    void setRequestIndex(long rmiRequestIndex) {
        this.requestIndex = rmiRequestIndex;
    }

    public long getRequestIndex() {
        return requestIndex;
    }

    public void setLastReceivedRequestIndex(long lastReceivedRequestIndex) {
        this.lastReceivedRequestIndex = lastReceivedRequestIndex;
    }

    public final T doRequest() throws RemoteException {
        boolean logDebugEnabled = logger.isDebugEnabled();
        long started = 0;
        if (logDebugEnabled) {
            logger.debug("DoRequest: " + this);
            started = System.currentTimeMillis();
        }
        T result = doRequest(requestIndex, lastReceivedRequestIndex);
        if(logDebugEnabled && result instanceof ServerResponse) {
            long serverSpent = ((ServerResponse) result).timeSpent;
            if(serverSpent >= 0) {
                long totalSpent = System.currentTimeMillis() - started;
                long commSpent = totalSpent - serverSpent;
                if (commSpent > MainController.timeDiffServerClientLog)
                    logger.debug("Request communication time threshold exceeded (" + commSpent + " ms) : " + this);
            }
        }
        return result;
    }

    public final void onAsyncRequest() {
        if (logger.isDebugEnabled()) {
            logger.debug("OnAsyncRequest: " + this);
        }
        onAsyncRequest(requestIndex);
    }

    public final void onResponse(T result) throws Exception {
        if (logger.isDebugEnabled()) {
            logger.debug("OnResponse: " + this);
        }
        onResponse(requestIndex, result);
    }

    public final void onResponseGetFailed(Exception e) throws Exception {
        if (logger.isDebugEnabled()) {
            logger.debug("OnResponseGetFailed: " + this);
        }
        onResponseGetFailed(requestIndex, e);
    }

    protected void onAsyncRequest(long requestIndex) {
    }

    protected abstract T doRequest(long requestIndex, long lastReceivedRequestIndex) throws RemoteException;

    protected void onResponseGetFailed(long requestIndex, Exception e) throws Exception {
    }

    protected void onResponse(long requestIndex, T result) throws Exception {
    }

    @Override
    public String toString() {
        return "RR[" + name + " : " + requestIndex + ", last received : " + lastReceivedRequestIndex + "]";
    }
}
