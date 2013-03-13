package platform.server.logics;

import org.antlr.runtime.RecognitionException;
import platform.server.classes.ConcreteCustomClass;
import platform.server.classes.StringClass;
import platform.server.logics.linear.LAP;
import platform.server.logics.linear.LCP;
import platform.server.logics.property.actions.GenerateLoginPasswordActionProperty;
import platform.server.logics.scripted.ScriptingLogicsModule;

import java.io.IOException;

import static platform.server.logics.ServerResourceBundle.getString;

public class EmailLogicsModule extends ScriptingLogicsModule{

    public ConcreteCustomClass notification;

    public LCP encryptedConnectionType;
    public LCP nameEncryptedConnectionType;
    public LCP smtpHost;
    public LCP smtpPort;
    public LCP emailAccount;
    public LCP emailPassword;
    public LCP emailBlindCarbonCopy;
    public LCP fromAddress;
    public LCP disableEmail;

    public LAP emailUserPassUser;

    public LCP isEventNotification;
    public LCP emailFromNotification;
    public LCP emailToNotification;
    public LCP emailToCCNotification;
    public LCP emailToBCNotification;
    public LCP textNotification;
    public LCP subjectNotification;
    public LCP inNotificationProperty;

    public EmailLogicsModule(BusinessLogics BL, BaseLogicsModule baseLM) throws IOException {
        super(EmailLogicsModule.class.getResourceAsStream("/scripts/system/Email.lsf"), baseLM, BL);
        setBaseLogicsModule(baseLM);
    }

    @Override
    public void initClasses() throws RecognitionException {
        super.initClasses();
        notification = (ConcreteCustomClass) getClassByName("notification");
    }

    @Override
    public void initProperties() throws RecognitionException {
        fromAddress = addDProp(getGroupByName("email"), "fromAddress", getString("logics.email.sender"), StringClass.get(50));
        super.initProperties();

        // ------- Управление почтой ------ //

        // Настройки почтового сервера
        nameEncryptedConnectionType = getLCPByName("nameEncryptedConnectionType");

        smtpHost = getLCPByName("smtpHost");
        smtpPort = getLCPByName("smtpPort");

        emailAccount = getLCPByName("emailAccount");
        emailPassword = getLCPByName("emailPassword");
        emailBlindCarbonCopy = getLCPByName("emailBlindCarbonCopy");

        disableEmail = getLCPByName("disableEmail");

        emailUserPassUser = getLAPByName("emailUserPassUser");

        // Уведомления
        isEventNotification = getLCPByName("isEventNotification");
        emailFromNotification = getLCPByName("emailFromNotification");
        emailToNotification = getLCPByName("emailToNotification");
        emailToCCNotification = getLCPByName("emailToCCNotification");
        emailToBCNotification = getLCPByName("emailToBCNotification");
        textNotification = getLCPByName("textNotification");
        subjectNotification = getLCPByName("subjectNotification");
        inNotificationProperty = getLCPByName("inNotificationProperty");
    }
}
