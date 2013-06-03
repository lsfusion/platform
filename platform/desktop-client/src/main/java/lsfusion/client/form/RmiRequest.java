package lsfusion.client.form;

import lsfusion.base.Callback;

import java.util.concurrent.Callable;

public abstract class RmiRequest<T> implements Callable<T>, Callback<T> {
    private long requestIndex = -1;

    void setRequestIndex(long rmiRequestIndex) {
        this.requestIndex = rmiRequestIndex;
    }

    public long getRequestIndex() {
        return requestIndex;
    }

    @Override
    public final T call() throws Exception {
        return doRequest(requestIndex);
    }

    @Override
    public final void done(T result) throws Exception {
        onResponse(requestIndex, result);
    }

    final void onAsyncRequest() {
        onAsyncRequest(requestIndex);
    }

    protected void onAsyncRequest(long requestIndex) {
    }

    protected abstract T doRequest(long requestIndex) throws Exception;

    protected void onResponse(long requestIndex, T result) throws Exception {
    }
}
