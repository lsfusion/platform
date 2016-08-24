package lsfusion.client.form;

import lsfusion.client.ClientLoggers;
import org.apache.log4j.Logger;

import java.rmi.RemoteException;

public abstract class RmiRequest<T> {
    private static final Logger logger = ClientLoggers.invocationLogger;

    private long requestIndex = -1;
    private long lastReceivedRequestIndex = -1;
    private String name;

    protected RmiRequest(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    void setRequestIndex(long rmiRequestIndex) {
        this.requestIndex = rmiRequestIndex;
    }

    public long getRequestIndex() {
        return requestIndex;
    }

    void setLastReceivedRequestIndex(long lastReceivedRequestIndex) {
        this.lastReceivedRequestIndex = lastReceivedRequestIndex;
    }

    final T doRequest() throws RemoteException {
        if (logger.isDebugEnabled()) {
            logger.debug("DoRequest: " + this);
        }
        return doRequest(requestIndex, lastReceivedRequestIndex);
    }

    final void onAsyncRequest() {
        if (logger.isDebugEnabled()) {
            logger.debug("OnAsyncRequest: " + this);
        }
        onAsyncRequest(requestIndex);
    }

    final void onResponse(T result) throws Exception {
        if (logger.isDebugEnabled()) {
            logger.debug("OnResponse: " + this);
        }
        onResponse(requestIndex, result);
    }

    protected void onAsyncRequest(long requestIndex) {
    }

    protected abstract T doRequest(long requestIndex, long lastReceivedRequestIndex) throws RemoteException;

    protected void onResponse(long requestIndex, T result) throws Exception {
    }

    @Override
    public String toString() {
        return "RR[" + name + " : " + requestIndex + "]";
    }
}
