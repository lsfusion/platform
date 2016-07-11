package lsfusion.gwt.login.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.i18n.client.Messages;

public interface LoginFrameMessages extends Messages {
    String username();

    String password();

    String login();

    String title();

    String registerTitle();

    String forgot();

    String loginFailed();

    String cancel();

    String emailPrompt();

    String remind();

    String incorrectEmail();

    String remindError();

    String remindSuccess();

    String loginError();

    String loggedInMessage(String userName);

    class Instance {
        private static final LoginFrameMessages instance = (LoginFrameMessages) GWT.create(LoginFrameMessages.class);

        public static LoginFrameMessages get() {
            return instance;
        }
    }
}
