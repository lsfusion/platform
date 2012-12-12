package platform.server.logics;

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

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;

import static platform.server.logics.ServerResourceBundle.getString;

public class EmailLogicsModule<T extends BusinessLogics<T>> extends LogicsModule {
    Logger logger;
    T BL;

    public T getBL(){
        return BL;
    }

    public StaticCustomClass encryptedConnectionTypeStatus;
    public ConcreteCustomClass notification;

    public AbstractGroup emailGroup;

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

    public EmailLogicsModule(T BL, BaseLogicsModule baseLM, Logger logger) {
        super("Email", "Email");
        setBaseLogicsModule(baseLM);
        this.BL = BL;
        this.logger = logger;
    }
    @Override
    public void initModuleDependencies() {
    }

    @Override
    public void initModule() {
    }

    @Override
    public void initClasses() {
        encryptedConnectionTypeStatus = addStaticClass("encryptedConnectionTypeStatus", getString("logics.connection.type.status"),
                new String[]{"SSL", "TLS"},
                new String[]{"SSL", "TLS"});
        notification = addConcreteClass("notification", getString("logics.notification"), baseLM.baseClass);
    }

    @Override
    public void initGroups() {
        emailGroup = addAbstractGroup("emailGroup", getString("logics.groups.emailgroup"), rootGroup, true);
    }

    @Override
    public void initTables() {
        addTable("notification", notification);
        addTable("notificationProperty", notification, BL.reflectionLM.property);
    }

    @Override
    public void initProperties() {
        // EmailLogicsModule
        // ------- Управление почтой ------ //
        emailContact = addDProp(baseGroup, "emailContact", getString("logics.email"), StringClass.get(50), baseLM.contact);
        emailContact.setRegexp("^[-a-zA-Z0-9!#$%&'*+/=?^_`{|}~]+(?:\\.[-a-zA-Z0-9!#$%&'*+/=?^_`{|}~]+)*@(?:[a-zA-Z0-9]([-a-zA-Z0-9]{0,61}[a-zA-Z0-9])?\\.)*(?:aero|arpa|asia|biz|cat|com|coop|edu|gov|info|int|jobs|mil|mobi|museum|name|net|org|pro|tel|travel|[a-zA-Z][a-zA-Z])$");
        emailContact.setRegexpMessage("<html>Неверный формат e-mail</html>");

        contactEmail = addAGProp("contactEmail", getString("logics.email.to.object"), emailContact);

        // Настройки почтового сервера
        encryptedConnectionType = addDProp(emailGroup, "encryptedConnectionType", getString("logics.connection.type.status"), encryptedConnectionTypeStatus);
        nameEncryptedConnectionType = addJProp(emailGroup, "nameEncryptedConnectionType", getString("logics.connection.type.status"), baseLM.name, encryptedConnectionType);
        nameEncryptedConnectionType.setPreferredCharWidth(3);

        smtpHost = addDProp(emailGroup, "smtpHost", getString("logics.host.smtphost"), StringClass.get(50));
        smtpPort = addDProp(emailGroup, "smtpPort", getString("logics.host.smtpport"), StringClass.get(10));

        emailAccount = addDProp(emailGroup, "emailAccount", getString("logics.email.accountname"), StringClass.get(50));
        emailPassword = addDProp(emailGroup, "emailPassword", getString("logics.email.password"), StringClass.get(50));

        emailBlindCarbonCopy = addDProp(emailGroup, "emailBlindCarbonCopy", getString("logics.email.copy.bcc"), StringClass.get(50));
        fromAddress = addDProp(emailGroup, "fromAddress", getString("logics.email.sender"), StringClass.get(50));

        disableEmail = addDProp(emailGroup, "disableEmail", getString("logics.email.disable.email.sending"), LogicalClass.instance);

        // Пользователи
        generateLoginPassword = addAProp(actionGroup, new GenerateLoginPasswordActionProperty(emailContact, baseLM.userLogin, baseLM.userPassword, baseLM.customUser));

        emailUserPassUser = addEAProp(getString("logics.user.password.reminder"), baseLM.customUser);
        addEARecipients(emailUserPassUser, emailContact, 1);

        // Уведомления
        isEventNotification = addDProp(baseGroup, "isDerivedChangeNotification", getString("logics.notification.for.any.change"), LogicalClass.instance, notification);
        emailFromNotification = addDProp(baseGroup, "emailFromNotification", getString("logics.notification.sender.address"), StringClass.get(50), notification);
        emailToNotification = addDProp(baseGroup, "emailToNotification", getString("logics.notification.recipient.address"), StringClass.get(50), notification);
        emailToCCNotification = addDProp(baseGroup, "emailToCCNotification", getString("logics.notification.copy"), StringClass.get(50), notification);
        emailToBCNotification = addDProp(baseGroup, "emailToBCNotification", getString("logics.notification.blind.copy"), StringClass.get(50), notification);
        textNotification = addDProp(baseGroup, "textNotification", getString("logics.notification.text"), TextClass.instance, notification);
        subjectNotification = addDProp(baseGroup, "subjectNotification", getString("logics.notification.topic"), StringClass.get(100), notification);
        inNotificationProperty = addDProp(baseGroup, "inNotificationProperty", getString("logics.notification.enable"), LogicalClass.instance, notification, BL.reflectionLM.property);


        initNavigators();
    }

    private void initNavigators() {
        addFormEntity(new RemindUserPassFormEntity(null, "remindPasswordLetter"));
        addFormEntity(new NotificationFormEntity(BL.LM.configuration, "notification"));
    }
    
    @Override
    public void initIndexes() {
    }

    @Override
    public String getNamePrefix() {
        return null;
    }


    private class RemindUserPassFormEntity extends FormEntity { // письмо пользователю о логине
        private ObjectEntity objUser;

        private RemindUserPassFormEntity(NavigatorElement parent, String sID) {
            super(parent, sID, getString("logics.user.password.remind"), true);

            objUser = addSingleGroupObject(1, "customUser", baseLM.customUser, baseLM.userLogin, baseLM.userPassword, baseLM.name);
            objUser.groupTo.initClassView = ClassViewType.PANEL;

            addInlineEAForm(emailUserPassUser, this, objUser, 1);

            setEditType(PropertyEditType.READONLY);
        }
    }

    public class NotificationFormEntity extends FormEntity {

        private ObjectEntity objNotification;
        private ObjectEntity objProperty;

        public NotificationFormEntity(NavigatorElement parent, String sID) {
            super(parent, sID, getString("logics.notification.notifications"));

            addPropertyDraw(new LP[]{smtpHost, smtpPort, nameEncryptedConnectionType, fromAddress, emailAccount, emailPassword,
                    emailBlindCarbonCopy, disableEmail});

            objNotification = addSingleGroupObject(notification, getString("logics.notification"));
            objProperty = addSingleGroupObject(BL.reflectionLM.property, getString("logics.property.properties"));

            addPropertyDraw(inNotificationProperty, objNotification, objProperty);
            addPropertyDraw(objNotification, subjectNotification, textNotification, emailFromNotification, emailToNotification, emailToCCNotification, emailToBCNotification, isEventNotification);
            addObjectActions(this, objNotification);
            addPropertyDraw(objProperty, BL.reflectionLM.captionProperty, BL.reflectionLM.SIDProperty);
            setForceViewType(textNotification, ClassViewType.PANEL);
            setEditType(BL.reflectionLM.captionProperty, PropertyEditType.READONLY);
            setEditType(BL.reflectionLM.SIDProperty, PropertyEditType.READONLY);

            RegularFilterGroupEntity filterGroup = new RegularFilterGroupEntity(genID());
            filterGroup.addFilter(
                    new RegularFilterEntity(genID(),
                            new NotNullFilterEntity(addPropertyObject(inNotificationProperty, objNotification, objProperty)),
                            getString("logics.only.checked"),
                            KeyStroke.getKeyStroke(KeyEvent.VK_F9, 0)
                    ), true);
            addRegularFilterGroup(filterGroup);
        }

        @Override
        public FormView createDefaultRichDesign() {
            DefaultFormView design = (DefaultFormView) super.createDefaultRichDesign();

            ContainerView textContainer = design.createContainer(getString("logics.notification.text"));
            textContainer.constraints.childConstraints = DoNotIntersectSimplexConstraint.TOTHE_BOTTOM;
            textContainer.add(design.get(getPropertyDraw(textNotification, objNotification)));
            textContainer.constraints.fillHorizontal = 1.0;
            textContainer.constraints.fillVertical = 1.0;

            PropertyDrawView textView = design.get(getPropertyDraw(textNotification, objNotification));
            textView.constraints.fillHorizontal = 1.0;
            textView.preferredSize = new Dimension(-1, 300);
            textView.panelLabelAbove = true;

            ContainerView specContainer = design.createContainer();
            design.getMainContainer().addAfter(specContainer, design.getGroupObjectContainer(objNotification.groupTo));
            specContainer.add(design.getGroupObjectContainer(objProperty.groupTo));
            specContainer.add(textContainer);
            specContainer.type = ContainerType.TABBED_PANE;

            addDefaultOrder(getPropertyDraw(BL.reflectionLM.SIDProperty, objProperty), true);
            return design;
        }
    }
}
