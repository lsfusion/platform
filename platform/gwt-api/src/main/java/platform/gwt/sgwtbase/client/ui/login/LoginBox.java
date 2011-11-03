package platform.gwt.sgwtbase.client.ui.login;

import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.types.TitleOrientation;
import com.smartgwt.client.types.Visibility;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.fields.ButtonItem;
import com.smartgwt.client.widgets.form.fields.PasswordItem;
import com.smartgwt.client.widgets.form.fields.SpacerItem;
import com.smartgwt.client.widgets.form.fields.TextItem;
import com.smartgwt.client.widgets.form.fields.events.ClickEvent;
import com.smartgwt.client.widgets.form.fields.events.ClickHandler;
import com.smartgwt.client.widgets.form.fields.events.KeyPressEvent;
import com.smartgwt.client.widgets.form.fields.events.KeyPressHandler;
import com.smartgwt.client.widgets.form.validator.RegExpValidator;
import com.smartgwt.client.widgets.layout.VLayout;
import platform.gwt.base.client.BaseMessages;

public class LoginBox extends VLayout {
    /**
     * RFC 2822 compliant
     * http://www.regular-expressions.info/email.html
     */
    private final static String EMAIL_VALIDATION_REGEX = "^[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*@(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?$";

    private static final BaseMessages messages = BaseMessages.Instance.get();

    private DynamicForm loginForm;
    private TextItem usernameBox;
    private PasswordItem passwordBox;
    private ButtonItem btnLogin;
    private ButtonItem btnForget;

    private DynamicForm remindForm;
    private TextItem emailBox;
    private ButtonItem btnRemind;

    private LoginBoxUiHandlers uiHandlers;

    public LoginBox() {
        this(true);
    }

    public LoginBox(boolean showRemindForm) {
        setAutoHeight();
        setAutoWidth();

        usernameBox = new TextItem("j_username", messages.username());
        usernameBox.setColSpan(2);
        usernameBox.setWidth("*");
        usernameBox.setRequired(true);
        passwordBox = new PasswordItem("j_password", messages.password());
        passwordBox.setWidth("*");
        passwordBox.setColSpan(2);
        passwordBox.setRequired(true);
        passwordBox.addKeyPressHandler(new KeyPressHandler() {
            @Override
            public void onKeyPress(KeyPressEvent event) {
                if ("Enter".equals(event.getKeyName())) {
                    login();
                }
            }
        });

        btnLogin = new ButtonItem("submit", messages.login());
        btnLogin.setStartRow(false);
        btnLogin.setAlign(Alignment.LEFT);
        btnLogin.setWidth("*");
        btnLogin.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                login();
            }
        });

        btnForget = new ButtonItem("forget", messages.forgot());
        btnForget.setStartRow(false);
        btnForget.setAlign(Alignment.RIGHT);
        btnForget.setWidth("*");
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
        loginForm.setColWidths("120", "120");
        if (showRemindForm) {
            loginForm.setFields(usernameBox, passwordBox, new SpacerItem(), btnLogin, new SpacerItem(), btnForget);
        } else {
            loginForm.setFields(usernameBox, passwordBox, new SpacerItem(), btnLogin);
        }

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

        addMember(loginForm);
//        loginForm.setBorder("1px solid green");
        if (showRemindForm) {
            addMember(remindForm);
        }
    }

    public String getUserName() {
        return usernameBox.getValueAsString();
    }

    public void setUserName(String userName) {
        usernameBox.setValue(userName);
    }

    public void setPassword(String password) {
        passwordBox.setValue(password);
    }

    public String getPassword() {
        return passwordBox.getValueAsString();
    }

    public String getEmail() {
        return emailBox.getValueAsString();
    }

    public void toggleRemindForm() {
        boolean reminderShown = remindForm.isVisible();
        btnForget.setTitle(reminderShown ? messages.forgot() : messages.cancel());
        remindForm.setVisible(!reminderShown);
    }

    private void remindPassword() {
        if (!remindForm.validate(false)) {
            return;
        }

        if (uiHandlers != null) {
            uiHandlers.remindPassword();
        }
    }

    private void login() {
        if (!loginForm.validate(false)) {
            return;
        }

        if (uiHandlers != null) {
            uiHandlers.login();
        }
    }

    public void setUIHandlers(LoginBoxUiHandlers uiHandlers) {
        this.uiHandlers = uiHandlers;
    }

    public void resetAndFocus() {
        usernameBox.selectValue();
    }

    public ButtonItem getRemindButton() {
        return btnRemind;
    }

    public ButtonItem getLoginButton() {
        return btnLogin;
    }

    public ButtonItem getForgetButton() {
        return btnForget;
    }
}
