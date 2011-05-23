package skolkovo.gwt.login.client.ui;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.SpanElement;
import com.google.gwt.event.dom.client.*;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.*;
import skolkovo.gwt.base.shared.MessageException;
import skolkovo.gwt.login.client.LoginFrameMessages;
import skolkovo.gwt.login.client.LoginService;

public class LoginMainWidget extends Composite {
    interface LoginMainWidgetUiBinder extends UiBinder<Widget, LoginMainWidget> {}
    private static LoginMainWidgetUiBinder uiBinder = GWT.create(LoginMainWidgetUiBinder.class);

    /**
     * RFC 2822 compliant
     * http://www.regular-expressions.info/email.html
     */
    private final static String EMAIL_VALIDATION_REGEX = "[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*@(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?";

    private static LoginFrameMessages messages = LoginFrameMessages.Instance.get();
    private static final String LOGIN_FAILED_STRING = "7698a602e3376d89c2329cf84f9dc779"; //=md5("Login failed!!!");

    interface Styles extends CssResource {
        String error();
        String info();
    }

    @UiField
    Styles style;

    @UiField
    SpanElement titleSpan;
    @UiField
    Label lbUsername;
    @UiField
    TextBox tbUsername;
    @UiField
    Label lbPassword;
    @UiField
    PasswordTextBox tbPassword;
    @UiField
    Button btnForget;
    @UiField
    Button btnLogin;
    @UiField
    FormPanel loginForm;
    @UiField
    Label errorLabel;
    @UiField
    Label lbEmailPrompt;
    @UiField
    TextBox tbEmail;
    @UiField
    Button btnRemind;
    @UiField
    VerticalPanel remindPanel;

    public LoginMainWidget() {
        initWidget(uiBinder.createAndBindUi(this));

        titleSpan.setInnerText(messages.title());
        lbUsername.setText(messages.username());
        lbPassword.setText(messages.password());
        btnLogin.setText(messages.login());
        btnForget.setText(messages.forgot());
        btnRemind.setText(messages.remind());
        lbEmailPrompt.setText(messages.emailPrompt());

        tbPassword.addKeyPressHandler(new KeyPressHandler() {
            @Override
            public void onKeyPress(KeyPressEvent event) {
                if (event.getNativeEvent().getKeyCode() == KeyCodes.KEY_ENTER) {
                    loginForm.submit();
                }
            }
        });

        btnLogin.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                clearErrorMessage();
                loginForm.submit();
            }
        });

        loginForm.addSubmitCompleteHandler(new FormPanel.SubmitCompleteHandler() {
            @Override
            public void onSubmitComplete(FormPanel.SubmitCompleteEvent event) {
                //эта строка должна совпадать с содержимым loginFailed.txt
                if (event.getResults().contains(LOGIN_FAILED_STRING)) {
                    showError(messages.loginFailed());
                } else {
                    Window.Location.reload();
                }
            }
        });

        btnForget.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                clearErrorMessage();

                boolean reminderShown = remindPanel.isVisible();

                btnForget.setText(reminderShown ? messages.forgot() : messages.cancel());
                remindPanel.setVisible(!reminderShown);
            }
        });

        tbEmail.addKeyPressHandler(new KeyPressHandler() {
            @Override
            public void onKeyPress(KeyPressEvent event) {
                if (event.getNativeEvent().getKeyCode() == KeyCodes.KEY_ENTER) {
                    btnRemind.click();
                }
            }
        });

        btnRemind.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                clearErrorMessage();

                if (!tbEmail.getText().matches(EMAIL_VALIDATION_REGEX)) {
                    showError(messages.incorrectEmail());
                    return;
                }

                LoginService.App.getInstance().remindPassword(tbEmail.getText(), new AsyncCallback<Void>() {
                    @Override
                    public void onFailure(Throwable t) {
                        if (t instanceof MessageException) {
                            showError(t.getMessage());
                        } else {
                            showError(messages.remindError());
                        }
                    }

                    @Override
                    public void onSuccess(Void result) {
                        btnForget.click();
                        showInfo(messages.remindSuccess());
                    }
                });
            }
        });
    }

    private void showError(String errorMessage) {
        errorLabel.setStyleName(style.error());
        errorLabel.setText(errorMessage);
    }

    private void showInfo(String infoMessage) {
        errorLabel.setStyleName(style.info());
        errorLabel.setText(infoMessage);
    }

    private void clearErrorMessage() {
        errorLabel.setText("");
    }
}