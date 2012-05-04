package platform.gwt.sgwtbase.client.ui.login;

import com.google.gwt.core.client.GWT;
import com.google.gwt.http.client.*;
import com.google.gwt.user.client.Window;
import com.smartgwt.client.util.SC;
import platform.gwt.base.client.BaseMessages;
import platform.gwt.base.client.GwtClientUtils;

public class SpringLoginBoxUiHandlers implements LoginBoxUiHandlers {
    private static final BaseMessages messages = BaseMessages.Instance.get();
    private static final String LOGIN_FAILED_STRING = "7698a602e3376d89c2329cf84f9dc779"; //=md5("Login failed!!!");

    private final LoginBox loginBox;

    public SpringLoginBoxUiHandlers(LoginBox loginBox) {
        this.loginBox = loginBox;
    }

    @Override
    public void login() {
        String loginUrl = GWT.getHostPageBaseURL() + "j_spring_security_check";

        String postData = URL.encode("j_username") + "=" + URL.encode(loginBox.getUserName()) +
                          "&" +
                          URL.encode("j_password") + "=" + URL.encode(loginBox.getPassword());

        RequestBuilder builder = new RequestBuilder(RequestBuilder.POST, loginUrl);
        builder.setHeader("Content-type", "application/x-www-form-urlencoded");
        try {
            builder.sendRequest(postData, new RequestCallback() {
                @Override
                public void onResponseReceived(Request request, Response response) {
                    //эта строка должна совпадать с содержимым loginFailed.txt
                    if (LOGIN_FAILED_STRING.equals(response.getText())) {
                        onLoginFailed();
                    } else {
                        onLoginSucceded();
                    }
                }

                @Override
                public void onError(Request request, Throwable exception) {
                    onLoginFailed();
                }
            });
        } catch (RequestException e) {
            onLoginFailed();
        }
    }

    @Override
    public void remindPassword() {
        loginBox.getRemindButton().disable();

        onRemind();
    }

    protected void onRemind() {
        //for overriding
    }

    protected void onRemindSucceded() {
        showInfo(messages.remindSuccess());
        loginBox.toggleRemindForm();
        loginBox.getRemindButton().enable();
    }

    protected void remindError() {
        showError(messages.remindError());
    }

    protected void onRemindFailed() {
        loginBox.getRemindButton().enable();
    }

    protected void onLoginSucceded() {
        String targetUrl = Window.Location.getParameter(GwtClientUtils.TARGET_PARAM);
        if (targetUrl != null) {
            Window.open(URL.decodePathSegment(targetUrl), "_self", null);
        } else {
            Window.Location.reload();
        }
    }

    protected void onLoginFailed() {
        showError(messages.loginFailed());
    }

    protected void showError(String errorMessage) {
        SC.warn(errorMessage);
    }

    protected void showInfo(String infoMessage) {
        SC.say(infoMessage);
    }
}
