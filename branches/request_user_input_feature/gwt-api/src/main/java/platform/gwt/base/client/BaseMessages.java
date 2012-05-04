package platform.gwt.base.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.i18n.client.Messages;

public interface BaseMessages extends Messages {
    String internalServerErrorMessage();

    String actionTimeoutErrorMessage();

    String sessionTimeoutErrorMessage();

    String yes();

    String no();

    String here();

    String showProfile();

    String logoutNotice();

    @Key("locale.ru")
    String localeRu();

    @Key("locale.en")
    String localeEn();

    String loading();

    String logout();

    String username();

    String firstName();

    String lastName();

    String password();

    String repeatPassword();

    String clickToReload();

    String pictureText();

    String login();

    String forgot();

    String register();

    String registration();

    String loginFailed();

    String cancel();

    String emailPrompt();

    String remind();

    String incorrectEmail();

    String remindError();

    String remindSuccess();

    String loginError();

    String loggedInMessage(String userName);

    String passwordsDontMatch();

    String wrongCaptcha();

    public static class Instance {
        private static final BaseMessages instance = (BaseMessages) GWT.create(BaseMessages.class);

        public static BaseMessages get() {
            return instance;
        }
    }
}
