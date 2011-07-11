package platform.gwt.form.client;

import com.google.gwt.user.client.rpc.AsyncCallback;
import net.customware.gwt.dispatch.shared.Action;

public class QueuedAction {
    public Action action;
    public AsyncCallback callback;

    public QueuedAction(Action action, AsyncCallback callback) {
        this.action = action;
        this.callback = callback;
    }
}
