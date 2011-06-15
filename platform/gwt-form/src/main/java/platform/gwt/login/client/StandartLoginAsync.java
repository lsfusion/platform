package platform.gwt.login.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;

public class StandartLoginAsync {
    private static final LoginServiceAsync realService = GWT.create(LoginService.class);

    public static void remindPassword(String email, AsyncCallback<Void> async) {
        realService.remindPassword(email, async);
    }
}
