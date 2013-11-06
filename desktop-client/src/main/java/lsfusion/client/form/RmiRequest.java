package lsfusion.client.form;

import lsfusion.base.Callback;
import lsfusion.client.ClientLoggers;
import org.apache.log4j.Logger;

import java.util.concurrent.Callable;

public abstract class RmiRequest<T> implements Callable<T>, Callback<T> {
    private static final Logger logger = ClientLoggers.invocationLogger;

    private long requestIndex = -1;
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

    @Override
    public final T call() throws Exception {
        if (logger.isDebugEnabled()) {
            logger.debug("DoRequest: " + this);
        }
        return doRequest(requestIndex);
    }

    @Override
    public final void done(T result) throws Exception {
        if (logger.isDebugEnabled()) {
            logger.debug("OnResponse: " + this);
        }
        onResponse(requestIndex, result);
    }

    final void onAsyncRequest() {
        if (logger.isDebugEnabled()) {
            logger.debug("OnAsyncRequest: " + this);
        }
        onAsyncRequest(requestIndex);
    }

    protected void onAsyncRequest(long requestIndex) {
    }

    protected abstract T doRequest(long requestIndex) throws Exception;

    protected void onResponse(long requestIndex, T result) throws Exception {
    }

    @Override
    public String toString() {
        return "RR[" + name + " : " + requestIndex + "]";
    }
}
