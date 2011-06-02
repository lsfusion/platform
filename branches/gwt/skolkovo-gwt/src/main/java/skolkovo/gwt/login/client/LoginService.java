package skolkovo.gwt.login.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;
import platform.gwt.base.shared.MessageException;

@RemoteServiceRelativePath("LoginService")
public interface LoginService extends RemoteService {
    void remindPassword(String email) throws MessageException;

    /**
     * Utility/Convenience class.
     * Use LoginService.App.getInstance() to access static instance of LoginServiceAsync
     */
    public static class App {
        private static final LoginServiceAsync ourInstance = (LoginServiceAsync) GWT.create(LoginService.class);

        public static LoginServiceAsync getInstance() {
            return ourInstance;
        }
    }
}
