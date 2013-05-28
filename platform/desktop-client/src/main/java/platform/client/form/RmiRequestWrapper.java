package platform.client.form;

public class RmiRequestWrapper<T> extends RmiRequest<T> {
    private final RmiRequest<T> target;

    public RmiRequestWrapper(RmiRequest<T> target) {
        this.target = target;
    }

    @Override
    void setRequestIndex(long rmiRequestIndex) {
        target.setRequestIndex(rmiRequestIndex);
    }

    @Override
    public long getRequestIndex() {
        return target.getRequestIndex();
    }

    @Override
    protected void onAsyncRequest(long requestIndex) {
        target.onAsyncRequest(requestIndex);
    }

    @Override
    protected T doRequest(long requestIndex) throws Exception {
        return target.doRequest(requestIndex);
    }

    @Override
    protected void onResponse(long requestIndex, T result) throws Exception {
        target.onResponse(requestIndex, result);
    }
}
