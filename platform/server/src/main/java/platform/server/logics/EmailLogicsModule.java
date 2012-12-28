package platform.server.logics;

import org.antlr.runtime.RecognitionException;
import org.apache.log4j.Logger;
import platform.interop.ClassViewType;
import platform.interop.PropertyEditType;
import platform.interop.form.layout.ContainerType;
import platform.interop.form.layout.DoNotIntersectSimplexConstraint;
import platform.server.classes.*;
import platform.server.form.entity.FormEntity;
import platform.server.form.entity.ObjectEntity;
import platform.server.form.entity.filter.NotNullFilterEntity;
import platform.server.form.entity.filter.RegularFilterEntity;
import platform.server.form.entity.filter.RegularFilterGroupEntity;
import platform.server.form.navigator.NavigatorElement;
import platform.server.form.view.ContainerView;
import platform.server.form.view.DefaultFormView;
import platform.server.form.view.FormView;
import platform.server.form.view.PropertyDrawView;
import platform.server.logics.linear.LAP;
import platform.server.logics.linear.LCP;
import platform.server.logics.linear.LP;
import platform.server.logics.property.actions.GenerateLoginPasswordActionProperty;
import platform.server.logics.property.group.AbstractGroup;
import platform.server.logics.scripted.ScriptingLogicsModule;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.util.Arrays;

import static platform.server.logics.ServerResourceBundle.getString;

public class EmailLogicsModule extends ScriptingLogicsModule{

    public ConcreteCustomClass notification;

    public LCP emailContact;
    public LCP contactEmail;

    public LCP encryptedConnectionType;
    public LCP nameEncryptedConnectionType;
    public LCP smtpHost;
    public LCP smtpPort;
    public LCP emailAccount;
    public LCP emailPassword;
    public LCP emailBlindCarbonCopy;
    public LCP fromAddress;
    public LCP disableEmail;

    public LAP generateLoginPassword;
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
        super(EmailLogicsModule.class.getResourceAsStream("/scripts/Email.lsf"), baseLM, BL);
        setBaseLogicsModule(baseLM);
    }

    @Override
    public void initClasses() throws RecognitionException {
        super.initClasses();
        notification = (ConcreteCustomClass) getClassByName("notification");
    }

    @Override
    public void initProperties() throws RecognitionException {
        fromAddress = addDProp(getGroupByName("emailGroup"), "fromAddress", getString("logics.email.sender"), StringClass.get(50));
        super.initProperties();

        // ------- Управление почтой ------ //
        emailContact = getLCPByName("emailContact");
        emailContact.setRegexp("^[-a-zA-Z0-9!#$%&'*+/=?^_`{|}~]+(?:\\.[-a-zA-Z0-9!#$%&'*+/=?^_`{|}~]+)*@(?:[a-zA-Z0-9]([-a-zA-Z0-9]{0,61}[a-zA-Z0-9])?\\.)*(?:aero|arpa|asia|biz|cat|com|coop|edu|gov|info|int|jobs|mil|mobi|museum|name|net|org|pro|tel|travel|[a-zA-Z][a-zA-Z])$");
        emailContact.setRegexpMessage("<html>Неверный формат e-mail</html>");

        contactEmail = getLCPByName("contactEmail");

        // Настройки почтового сервера
        nameEncryptedConnectionType = getLCPByName("nameEncryptedConnectionType");

        smtpHost = getLCPByName("smtpHost");
        smtpPort = getLCPByName("smtpPort");

        emailAccount = getLCPByName("emailAccount");
        emailPassword = getLCPByName("emailPassword");
        emailBlindCarbonCopy = getLCPByName("emailBlindCarbonCopy");

        disableEmail = getLCPByName("disableEMail");

        // Пользователи
        generateLoginPassword = addAProp(new GenerateLoginPasswordActionProperty(emailContact, baseLM.userLogin, baseLM.userPassword, baseLM.customUser));

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
