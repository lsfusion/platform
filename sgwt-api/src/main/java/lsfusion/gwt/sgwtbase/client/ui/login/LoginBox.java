package lsfusion.gwt.sgwtbase.client.ui.login;

import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.types.TitleOrientation;
import com.smartgwt.client.types.Visibility;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.fields.*;
import com.smartgwt.client.widgets.form.fields.events.ClickEvent;
import com.smartgwt.client.widgets.form.fields.events.ClickHandler;
import com.smartgwt.client.widgets.form.fields.events.KeyPressEvent;
import com.smartgwt.client.widgets.form.fields.events.KeyPressHandler;
import com.smartgwt.client.widgets.form.validator.RegExpValidator;
import com.smartgwt.client.widgets.layout.VLayout;
import lsfusion.gwt.base.client.BaseMessages;

public class LoginBox extends VLayout {
    /**
     * RFC 2822 compliant
     * http://www.regular-expressions.info/email.html
     */
    private final static String EMAIL_VALIDATION_REGEX = "^[a-zA-Z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[a-zA-Z0-9!#$%&'*+/=?^_`{|}~-]+)*@(?:[a-zA-Z0-9](?:[a-zA-Z0-9-]*[a-zA-Z0-9])?\\.)+[a-zA-Z0-9](?:[a-zA-Z0-9-]*[a-zA-Z0-9])?$";

    private static final BaseMessages messages = BaseMessages.Instance.get();

    private DynamicForm loginForm;
    private TextItem usernameBox;
    private PasswordItem passwordBox;
    private ButtonItem btnLogin;
    private LinkItem lnkForget;
    private LinkItem lnkRegister;

    private DynamicForm remindForm;
    private TextItem emailBox;
    private ButtonItem btnRemind;

    private LoginBoxUiHandlers uiHandlers;

    public LoginBox() {
        this(true, true);
    }

    public LoginBox(boolean showRemindForm, boolean showRegisterLink) {
        setAutoHeight();
        setAutoWidth();

        String username = messages.username();
        usernameBox = new TextItem("j_username", username);
        usernameBox.setWidth("*");
        usernameBox.setRequired(true);
        passwordBox = new PasswordItem("j_password", messages.password());
        passwordBox.setWidth("*");
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
        btnLogin.setWidth("*");
        btnLogin.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                login();
            }
        });

        lnkForget = new LinkItem("forget");
        lnkForget.setLinkTitle(messages.forgot());
        lnkForget.setAlign(Alignment.RIGHT);
        lnkForget.setShowTitle(false);
        lnkForget.setWidth("*");
        lnkForget.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                toggleRemindForm();
            }
        });

        lnkRegister = new LinkItem("register");
        lnkRegister.setLinkTitle(messages.register());
        lnkRegister.setAlign(Alignment.RIGHT);
        lnkRegister.setShowTitle(false);
        lnkRegister.setWidth("*");

        loginForm = new DynamicForm();
        loginForm.setTitleOrientation(TitleOrientation.TOP);
        loginForm.setAutoWidth();
        loginForm.setAutoHeight();
        loginForm.setColWidths("240");
        loginForm.setNumCols(1);

        if (showRegisterLink) {
            if (showRemindForm)
                loginForm.setFields(usernameBox, passwordBox, btnLogin, lnkRegister, lnkForget);
            else
                loginForm.setFields(usernameBox, passwordBox, btnLogin, lnkRegister);
        } else {
            if (showRemindForm)
                loginForm.setFields(usernameBox, passwordBox, btnLogin, lnkForget);
            else
                loginForm.setFields(usernameBox, passwordBox, btnLogin);
        }

        emailBox = new TextItem("email", messages.emailPrompt());
        emailBox.setWidth("*");
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
        btnRemind.setWidth("*");
        btnRemind.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                remindPassword();
            }
        });

        ButtonItem btnCancelRemind = new ButtonItem("cancelRemind", messages.cancel());
        btnCancelRemind.setWidth("*");
        btnCancelRemind.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                toggleRemindForm();
            }
        });

        remindForm = new DynamicForm();
        remindForm.setTitleOrientation(TitleOrientation.TOP);
        remindForm.setAutoHeight();
        remindForm.setNumCols(1);
        remindForm.setColWidths("240");
        remindForm.setFields(emailBox, btnRemind, btnCancelRemind);
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
        if (!reminderShown)
            loginForm.hideItem(lnkForget.getName());
        else
            loginForm.showItem(lnkForget.getName());
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

    public void login() {
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

    public LinkItem getForgetButton() {
        return lnkForget;
    }

    public LinkItem getRegisterButton() {
        return lnkRegister;
    }
}
