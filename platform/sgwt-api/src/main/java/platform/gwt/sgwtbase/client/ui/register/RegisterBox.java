package platform.gwt.sgwtbase.client.ui.register;

import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.types.TitleOrientation;
import com.smartgwt.client.widgets.Img;
import com.smartgwt.client.widgets.form.fields.events.ClickEvent;
import com.smartgwt.client.widgets.form.fields.events.ClickHandler;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.fields.*;
import com.smartgwt.client.widgets.form.validator.MatchesFieldValidator;
import com.smartgwt.client.widgets.form.validator.RegExpValidator;
import com.smartgwt.client.widgets.layout.VLayout;
import platform.gwt.base.client.BaseMessages;
import platform.gwt.base.client.GwtClientUtils;

public class RegisterBox extends VLayout {
    private final static String EMAIL_VALIDATION_REGEX = "^[a-zA-Z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[a-zA-Z0-9!#$%&'*+/=?^_`{|}~-]+)*@(?:[a-zA-Z0-9](?:[a-zA-Z0-9-]*[a-zA-Z0-9])?\\.)+[a-zA-Z0-9](?:[a-zA-Z0-9-]*[a-zA-Z0-9])?$";
    private final static String WRONG_CAPTCHA = "wrongCaptcha";

    private RegisterBoxUiHandlers uiHandlers;
    private static final BaseMessages messages = BaseMessages.Instance.get();

    private DynamicForm mainForm;
    private StaticTextItem errorLabel;
    private TextItem usernameBox;
    private TextItem emailBox;
    private TextItem firstNameBox;
    private TextItem lastNameBox;
    private PasswordItem passwordBox;
    private PasswordItem repeatPasswordBox;
    private CanvasItem captchaImageItem;
    private String captchaSalt;
    private TextItem captchaBox;
    private ButtonItem cancelButton;

    public RegisterBox() {
        mainForm = new DynamicForm();
        mainForm.setAutoHeight();
        mainForm.setAutoWidth();
        mainForm.setTitleOrientation(TitleOrientation.TOP);
        mainForm.setColWidths("140", "140");
        mainForm.setValidateOnExit(true);

        errorLabel = new StaticTextItem("error");
        errorLabel.setWidth("*");
        errorLabel.setShowTitle(false);
        errorLabel.setColSpan(2);
        errorLabel.setTextBoxStyle("errorMessage");
        errorLabel.setVisible(false);

        usernameBox = new TextItem("username", messages.username() + " *");
        usernameBox.setColSpan(2);
        usernameBox.setWidth("*");
        usernameBox.setRequired(true);

        emailBox = new TextItem("email", "e-mail *");
        emailBox.setColSpan(2);
        emailBox.setWidth("*");
        emailBox.setValidators(new RegExpValidator(EMAIL_VALIDATION_REGEX));
        emailBox.setRequired(true);

        firstNameBox = new TextItem("firstName", messages.firstName() + " *");
        firstNameBox.setColSpan(2);
        firstNameBox.setWidth("*");
        firstNameBox.setRequired(true);

        lastNameBox = new TextItem("lastName", messages.lastName() + " *");
        lastNameBox.setColSpan(2);
        lastNameBox.setWidth("*");
        lastNameBox.setRequired(true);

        passwordBox = new PasswordItem("password", messages.password() + " *");
        passwordBox.setColSpan(2);
        passwordBox.setWidth("*");
        passwordBox.setRequired(true);

        repeatPasswordBox = new PasswordItem("confirmPassword", messages.repeatPassword() + " *");
        MatchesFieldValidator passwordsMatchValidator = new MatchesFieldValidator();
        passwordsMatchValidator.setErrorMessage(messages.passwordsDontMatch());
        passwordsMatchValidator.setOtherField("password");
        repeatPasswordBox.setColSpan(2);
        repeatPasswordBox.setWidth("*");
        repeatPasswordBox.setValidators(passwordsMatchValidator);
        repeatPasswordBox.setRequired(true);

        captchaSalt = ("" + Math.random() * 10).substring(3);
        Img captchaImage = new Img(GwtClientUtils.getWebAppBaseURL() + "/jcaptcha?salt=" + captchaSalt);
        captchaImageItem = new CanvasItem();
        captchaImageItem.setShowTitle(false);
        captchaImageItem.setCanvas(captchaImage);
        captchaImageItem.setWidth("*");
        captchaImageItem.setHeight(70);
        captchaImageItem.setTooltip(messages.clickToReload());
        captchaImageItem.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                refreshCaptchaImage();
            }
        });

        captchaBox = new TextItem("captcha", messages.pictureText() + " *");
        captchaBox.setWidth("*");
        captchaBox.setRequired(true);

        ButtonItem registerButton = new ButtonItem("register", messages.registration());
        registerButton.setStartRow(false);
        registerButton.setColSpan(2);
        registerButton.setAlign(Alignment.RIGHT);
        registerButton.setWidth("*");
        registerButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                if (validate()) {
                    uiHandlers.register();
                }
            }
        });

        cancelButton = new ButtonItem("cancel", messages.cancel());
        cancelButton.setStartRow(false);
        cancelButton.setColSpan(2);
        cancelButton.setAlign(Alignment.RIGHT);
        cancelButton.setWidth("*");

        mainForm.setFields(errorLabel, usernameBox, emailBox, firstNameBox, lastNameBox, passwordBox, repeatPasswordBox, captchaImageItem,
                captchaBox, new SpacerItem(), registerButton, cancelButton);

        addMember(mainForm);
    }

    public void refreshCaptchaImage() {
        captchaSalt = ("" + Math.random() * 10).substring(3);
        Img newCaptchaImg = new Img("/jcaptcha?salt=" + captchaSalt);
        captchaImageItem.getCanvas().hide();
        captchaImageItem.setCanvas(newCaptchaImg);
        captchaImageItem.redraw();
        captchaBox.clearValue();
    }

    public boolean validate() {
        if (!mainForm.validate(false)) {
            errorLabel.setVisible(false);
            return false;
        }
        return true;
    }
    
    public ButtonItem getCancelButton() {
        return cancelButton;
    }

    public void setUiHandlers(RegisterBoxUiHandlers handlers) {
        uiHandlers = handlers;
    }

    public String getUsername() {
        return usernameBox.getValueAsString();
    }

    public String getEmail() {
        return emailBox.getValueAsString();
    }

    public String getFirstName() {
        return firstNameBox.getValueAsString();
    }

    public String getLastName() {
        return lastNameBox.getValueAsString();
    }

    public String getPassword() {
        return passwordBox.getValueAsString();
    }

    public String getCaptchaText() {
        return captchaBox.getValueAsString();
    }

    public String getCaptchaSalt() {
        return captchaSalt;
    }
    
    public void reset() {
        usernameBox.clearValue();
        emailBox.clearValue();
        firstNameBox.clearValue();
        lastNameBox.clearValue();
        passwordBox.clearValue();
        repeatPasswordBox.clearValue();
        captchaBox.clearValue();
        errorLabel.setVisible(false);
    }

    public void showError(String errorText) {
        errorLabel.setValue(WRONG_CAPTCHA.equals(errorText) ? messages.wrongCaptcha() : errorText);
        errorLabel.setVisible(true);
        refreshCaptchaImage();
    }
}
