package skolkovo.gwt.login.client;

import com.google.gwt.user.client.rpc.AsyncCallback;

public interface LoginServiceAsync {
    void remindPassword(String email, AsyncCallback<Void> async);
}
