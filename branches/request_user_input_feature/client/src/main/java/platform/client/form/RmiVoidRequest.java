package platform.client.form;

public abstract class RmiVoidRequest extends RmiRequest<Void> {
    protected final Void doRequest(long requestIndex) throws Exception {
        doExecute(requestIndex);
        return null;
    }

    protected abstract void doExecute(long requestIndex) throws Exception;
}
