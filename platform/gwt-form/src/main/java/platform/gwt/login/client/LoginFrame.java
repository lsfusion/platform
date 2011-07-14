package platform.gwt.login.client;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.http.client.*;
import com.google.gwt.i18n.client.Dictionary;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.types.TitleOrientation;
import com.smartgwt.client.types.Visibility;
import com.smartgwt.client.util.SC;
import com.smartgwt.client.widgets.Label;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.fields.ButtonItem;
import com.smartgwt.client.widgets.form.fields.PasswordItem;
import com.smartgwt.client.widgets.form.fields.TextItem;
import com.smartgwt.client.widgets.form.fields.events.ClickEvent;
import com.smartgwt.client.widgets.form.fields.events.ClickHandler;
import com.smartgwt.client.widgets.form.fields.events.KeyPressEvent;
import com.smartgwt.client.widgets.form.fields.events.KeyPressHandler;
import com.smartgwt.client.widgets.form.validator.RegExpValidator;
import com.smartgwt.client.widgets.layout.VLayout;
import net.customware.gwt.dispatch.client.DefaultExceptionHandler;
import net.customware.gwt.dispatch.client.standard.StandardDispatchAsync;
import platform.gwt.base.client.ui.CenterLayout;
import platform.gwt.base.client.ui.ToolStripPanel;
import platform.gwt.base.shared.MessageException;
import platform.gwt.base.shared.actions.VoidResult;
import platform.gwt.login.shared.actions.RemindPassword;

public class LoginFrame extends VLayout implements EntryPoint {
    /**
     * RFC 2822 compliant
     * http://www.regular-expressions.info/email.html
     */
    private final static String EMAIL_VALIDATION_REGEX = "^[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*@(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?$";

    private static final LoginFrameMessages messages = LoginFrameMessages.Instance.get();
    private static final String LOGIN_FAILED_STRING = "7698a602e3376d89c2329cf84f9dc779"; //=md5("Login failed!!!");

    private final static StandardDispatchAsync loginService = new StandardDispatchAsync(new DefaultExceptionHandler());

    private DynamicForm loginForm;
    private TextItem usernameBox;
    private PasswordItem passwordBox;
    private ButtonItem btnLogin;
    private ButtonItem btnForget;

    private DynamicForm remindForm;
    private TextItem emailBox;
    private ButtonItem btnRemind;

    public void onModuleLoad() {
        Window.setTitle(messages.title());

        setWidth100();
        setHeight100();

        VLayout centerComponent = new VLayout();
        centerComponent.setAutoHeight();
        centerComponent.setAutoWidth();

        String userName = getUserName();
        if (userName == null) {
            usernameBox = new TextItem("j_username", messages.username());
            usernameBox.setWidth(200);
            usernameBox.setRequired(true);
            usernameBox.setColSpan("*");
            passwordBox = new PasswordItem("j_password", messages.password());
            passwordBox.setWidth(200);
            passwordBox.setRequired(true);
            passwordBox.setColSpan("*");
            passwordBox.addKeyPressHandler(new KeyPressHandler() {
                @Override
                public void onKeyPress(KeyPressEvent event) {
                    if ("Enter".equals(event.getKeyName())) {
                        login();
                    }
                }
            });

            btnLogin = new ButtonItem("submit", messages.login());
            btnLogin.setEndRow(false);
            btnLogin.setAlign(Alignment.LEFT);
            btnLogin.addClickHandler(new ClickHandler() {
                @Override
                public void onClick(ClickEvent event) {
                    login();
                }
            });

            btnForget = new ButtonItem("forget", messages.forgot());
            btnForget.setStartRow(false);
            btnForget.setAlign(Alignment.RIGHT);
            btnForget.addClickHandler(new ClickHandler() {
                @Override
                public void onClick(ClickEvent event) {
                    toggleRemindForm();
                }
            });

            loginForm = new DynamicForm();
            loginForm.setTitleOrientation(TitleOrientation.TOP);
            loginForm.setAutoWidth();
            loginForm.setAutoHeight();
            loginForm.setColWidths("100", "*");
            loginForm.setFields(usernameBox, passwordBox, btnLogin, btnForget);

            emailBox = new TextItem("email", messages.emailPrompt());
            emailBox.setWidth(200);
            emailBox.setRequired(true);
            emailBox.setValidators(new RegExpValidator(EMAIL_VALIDATION_REGEX));
            emailBox.addKeyPressHandler(new KeyPressHandler() {
                @Override
                public void onKeyPress(KeyPressEvent event) {
                    if ("Enter".equals(event.getKeyName())) {
                        remindPassword();
                    }
                }
            });

            btnRemind = new ButtonItem("remind", messages.remind());
            btnRemind.addClickHandler(new ClickHandler() {
                @Override
                public void onClick(ClickEvent event) {
                    remindPassword();
                }
            });

            remindForm = new DynamicForm();
            remindForm.setTitleOrientation(TitleOrientation.TOP);
            remindForm.setAutoWidth();
            remindForm.setAutoHeight();
            remindForm.setFields(emailBox, btnRemind);
            remindForm.setVisibility(Visibility.HIDDEN);
            remindForm.setVisible(false);

            centerComponent.addMember(loginForm);
            centerComponent.addMember(remindForm);
        } else {
            Label lbInfo = new Label(messages.loggedInMessage(userName));
            lbInfo.setAlign(Alignment.CENTER);

            centerComponent.setWidth(300);
            centerComponent.addMember(lbInfo);
        }

        CenterLayout main = new CenterLayout(centerComponent);

        addMember(new ToolStripPanel("logo_toolbar.png", messages.title(), userName != null));
        addMember(main);

        draw();
    }

    public String getUserName() {
        try {
            Dictionary dict = Dictionary.getDictionary("parameters");
            if (dict != null) {
                return dict.get("userName");
            }
        } catch (Exception ignored) {
        }

        return null;
    }

    private void toggleRemindForm() {
        boolean reminderShown = remindForm.isVisible();
        btnForget.setTitle(reminderShown ? messages.forgot() : messages.cancel());
        remindForm.setVisible(!reminderShown);
    }

    private void remindPassword() {
        if (!remindForm.validate(false)) {
            return;
        }

        btnRemind.disable();

        loginService.execute(new RemindPassword(emailBox.getValueAsString()), new AsyncCallback<VoidResult>() {
            @Override
            public void onFailure(Throwable t) {
                if (t instanceof MessageException) {
                    showError(t.getMessage());
                } else {
                    showError(messages.remindError());
                }

                btnRemind.enable();
            }

            @Override
            public void onSuccess(VoidResult result) {
                showInfo(messages.remindSuccess());
                toggleRemindForm();
                btnRemind.enable();
            }
        });
    }

    private void login() {
        if (!loginForm.validate(false)) {
            return;
        }

        String loginUrl = GWT.getHostPageBaseURL() + "j_spring_security_check";

        String postData = URL.encode("j_username") + "=" + URL.encode(usernameBox.getValueAsString()) +
                          "&" +
                          URL.encode("j_password") + "=" + URL.encode(passwordBox.getValueAsString());

        RequestBuilder builder = new RequestBuilder(RequestBuilder.POST, loginUrl);
        builder.setHeader("Content-type", "application/x-www-form-urlencoded");
        try {
            builder.sendRequest(postData, new RequestCallback() {
                @Override
                public void onResponseReceived(Request request, Response response) {
                    //эта строка должна совпадать с содержимым loginFailed.txt
                    if (LOGIN_FAILED_STRING.equals(response.getText())) {
                        showError(messages.loginFailed());
                    } else {
                        Window.Location.reload();
                    }
                }

                @Override
                public void onError(Request request, Throwable exception) {
                    showError(messages.loginError());
                }
            });
        } catch (RequestException e) {
            showError(messages.loginError());
        }
    }

    private void showError(String errorMessage) {
        SC.warn(errorMessage);
    }

    private void showInfo(String infoMessage) {
        SC.say(infoMessage);
    }
}
