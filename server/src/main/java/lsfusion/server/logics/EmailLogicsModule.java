package lsfusion.server.logics;

import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.server.classes.ConcreteCustomClass;
import lsfusion.server.classes.StringClass;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.form.entity.FormEntity;
import lsfusion.server.form.entity.ObjectEntity;
import lsfusion.server.logics.i18n.LocalizedString;
import lsfusion.server.logics.linear.LAP;
import lsfusion.server.logics.linear.LCP;
import lsfusion.server.logics.property.CalcPropertyInterfaceImplement;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.PropertyInterface;
import lsfusion.server.logics.property.group.AbstractGroup;
import lsfusion.server.logics.scripted.ScriptingErrorLog;
import lsfusion.server.logics.scripted.ScriptingLogicsModule;
import lsfusion.server.mail.AttachmentFormat;
import lsfusion.server.mail.SendEmailActionProperty;
import org.antlr.runtime.RecognitionException;

import javax.mail.Message;
import javax.mail.internet.MimeMessage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static java.util.Arrays.asList;
import static java.util.Arrays.copyOfRange;
import static lsfusion.base.BaseUtils.consecutiveList;
import static lsfusion.server.logics.PropertyUtils.readCalcImplements;

public class EmailLogicsModule extends ScriptingLogicsModule{

    public ConcreteCustomClass notification;
    
    public LCP defaultInboxAccount;
    public LCP nameEncryptedConnectionTypeAccount;
    public LCP smtpHostAccount;
    public LCP smtpPortAccount;
    public LCP receiveHostAccount;
    public LCP receivePortAccount;
    public LCP nameAccount;
    public LCP passwordAccount;
    public LCP nameReceiveAccountTypeAccount;
    public LCP deleteMessagesAccount;
    public LCP lastDaysAccount;
    public LCP blindCarbonCopyAccount;
    public LCP fromAddressAccount;
    public LCP disableAccount;
    public LCP enableAccount;

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
        super(EmailLogicsModule.class.getResourceAsStream("/lsfusion/system/Email.lsf"), "/lsfusion/system/Email.lsf", baseLM, BL);
        setBaseLogicsModule(baseLM);
    }

    @Override
    public void initClasses() throws RecognitionException {
        super.initClasses();
        notification = (ConcreteCustomClass) findClass("Notification");
    }

    @Override
    public void initProperties() throws RecognitionException {
        super.initProperties();

        // ------- Управление почтой ------ //

        // Настройки почтового сервера
        defaultInboxAccount = findProperty("defaultInboxAccount[]");
        
        nameEncryptedConnectionTypeAccount = findProperty("nameEncryptedConnectionType[Account]");

        smtpHostAccount = findProperty("smtpHost[Account]");
        smtpPortAccount = findProperty("smtpPort[Account]");
        receiveHostAccount = findProperty("receiveHost[Account]");
        receivePortAccount = findProperty("receivePort[Account]");

        nameAccount = findProperty("name[Account]");
        passwordAccount = findProperty("password[Account]");
        nameReceiveAccountTypeAccount = findProperty("nameReceiveAccountType[Account]");
        deleteMessagesAccount = findProperty("deleteMessages[Account]");
        lastDaysAccount = findProperty("lastDays[Account]");
        blindCarbonCopyAccount = findProperty("blindCarbonCopy[Account]");

        disableAccount = findProperty("disable[Account]");
        enableAccount = findProperty("enable[Account]");

        emailUserPassUser = findAction("emailUserPass[Contact]");
        
        // Уведомления
        isEventNotification = findProperty("isEvent[Notification]");
        emailFromNotification = findProperty("emailFrom[Notification]");
        emailToNotification = findProperty("emailTo[Notification]");
        emailToCCNotification = findProperty("emailToCC[Notification]");
        emailToBCNotification = findProperty("emailToBC[Notification]");
        textNotification = findProperty("text[Notification]");
        subjectNotification = findProperty("subject[Notification]");
        inNotificationProperty = findProperty("in[Notification,Property]");

        fromAddressAccount = findProperty("fromAddress[Account]");
    }

    public LAP addEAProp(ValueClass... params) throws ScriptingErrorLog.SemanticErrorException {
        return addEAProp((String) null, params);
    }

    public LAP addEAProp(LCP fromAddressAccount, ValueClass... params) {
        return addEAProp(null, fromAddressAccount, blindCarbonCopyAccount, params);
    }

    public LAP addEAProp(String subject, ValueClass... params) throws ScriptingErrorLog.SemanticErrorException {
        return addEAProp(subject, findProperty("fromAddress[Account]"), blindCarbonCopyAccount, params);
    }

    public LAP addEAProp(LCP fromAddressAccount, LCP blindCarbonCopyAccount, ValueClass... params) {
        return addEAProp(null, fromAddressAccount, blindCarbonCopyAccount, params);
    }

    public LAP addEAProp(String subject, LCP fromAddressAccount, LCP blindCarbonCopyAccount, ValueClass... params) {
        return addEAProp(null, LocalizedString.create("emailContact"), subject, fromAddressAccount, blindCarbonCopyAccount, params);
    }

    public LAP addEAProp(AbstractGroup group, LocalizedString caption, String subject, LCP fromAddressAccount, LCP blindCarbonCopyAccount, ValueClass... params) {
        Object[] fromImplement = new Object[] {fromAddressAccount};
        Object[] subjImplement;
        if (subject != null) {
            subjImplement = new Object[] {addCProp(StringClass.get(subject.length()), subject)};
        } else {
            ValueClass[] nParams = new ValueClass[params.length + 1];
            System.arraycopy(params, 0, nParams, 0, params.length);
            nParams[params.length] = StringClass.get(100);

            params = nParams;

            subjImplement = new Object[] {params.length};
        }

        LAP eaPropLP = addEAProp(group, caption, params, fromImplement, subjImplement);
        addEARecipientsType(eaPropLP, Message.RecipientType.BCC, blindCarbonCopyAccount);

        return eaPropLP;
    }

    public LAP<ClassPropertyInterface> addEAProp(AbstractGroup group, LocalizedString caption, ValueClass[] params, Object[] fromAddressAccount, Object[] subject) {
        SendEmailActionProperty eaProp = new SendEmailActionProperty(caption, params);
        LAP<ClassPropertyInterface> eaPropLP = addProperty(group, new LAP<>(eaProp));

        if (fromAddressAccount != null) {
            eaProp.setFromAddressAccount(readCalcImplements(eaPropLP.listInterfaces, fromAddressAccount).single());
        }

        if (subject != null) {
            eaProp.setSubject(readCalcImplements(eaPropLP.listInterfaces, subject).single());
        }

        return eaPropLP;
    }

    public void addEARecipientsType(LAP eaProp, Message.RecipientType type, Object... params) {
        ImList<CalcPropertyInterfaceImplement<ClassPropertyInterface>> recipImpls = readCalcImplements(eaProp.listInterfaces, params);

        for (CalcPropertyInterfaceImplement<ClassPropertyInterface> recipImpl : recipImpls) {
            ((SendEmailActionProperty) eaProp.property).addRecipient(recipImpl, type);
        }
    }

    public void addEARecipients(LAP eaProp, Object... params) {
        addEARecipientsType(eaProp, MimeMessage.RecipientType.TO, params);
    }

    public void addInlineEAForm(LAP eaProp, FormEntity form, Object... params) {
        ((SendEmailActionProperty) eaProp.property).addInlineForm(form, readObjectImplements(eaProp, params));
    }

    /**
     * @param params : сначала может идти свойство, из которго будет читаться имя attachment'a,
     * при этом все его входы мэпятся на входы eaProp по порядку, <br/>
     * затем список объектов ObjectEntity + мэппинг, из которого будет читаться значение этого объекта.
     * <br/>
     * Мэппинг - это мэппинг на интерфейсы результирующего свойства (prop, 1,3,4 или просто N)
     * @deprecated теперь лучше использовать {@link lsfusion.server.logics.EmailLogicsModule#addAttachEAForm(lsfusion.server.logics.linear.LAP, lsfusion.server.form.entity.FormEntity, lsfusion.server.mail.AttachmentFormat, java.lang.Object...)}
     * с явным мэппингом свойства для имени
     */
    public void addAttachEAFormNameFullyMapped(LAP eaProp, FormEntity form, AttachmentFormat format, Object... params) {
        if (params.length > 0 && params[0] instanceof LCP) {
            LCP attachmentNameProp = (LCP) params[0];

            ArrayList nParams = new ArrayList();
            nParams.add(attachmentNameProp);
            nParams.addAll(consecutiveList(attachmentNameProp.listInterfaces.size()));
            nParams.addAll(asList(copyOfRange(params, 1, params.length)));

            params = nParams.toArray();
        }

        addAttachEAForm(eaProp, form, format, params);
    }

    /**
     * @param params : сначала может идти мэппинг, из которго будет читаться имя attachment'a,
     * затем список объектов ObjectEntity + мэппинг, из которого будет читаться значение этого объекта.
     * <br/>
     * Мэппинг - это мэппинг на интерфейсы результирующего свойства (prop, 1,3,4 или просто N)
     */
    public void addAttachEAForm(LAP<ClassPropertyInterface> eaProp, FormEntity form, AttachmentFormat format, Object... params) {
        CalcPropertyInterfaceImplement<ClassPropertyInterface> attachNameImpl = null;
        if (params.length > 0 && !(params[0] instanceof ObjectEntity)) {
            int attachNameParamsCnt = 1;
            while (attachNameParamsCnt < params.length && !(params[attachNameParamsCnt] instanceof ObjectEntity)) {
                ++attachNameParamsCnt;
            }
            attachNameImpl = readCalcImplements(eaProp.listInterfaces, copyOfRange(params, 0, attachNameParamsCnt)).single();
            params = copyOfRange(params, attachNameParamsCnt, params.length);
        }
        ((SendEmailActionProperty) eaProp.property).addAttachmentForm(form, format, readObjectImplements(eaProp, params), attachNameImpl);
    }

    private <P extends PropertyInterface> Map<ObjectEntity, CalcPropertyInterfaceImplement<P>> readObjectImplements(LAP<P> eaProp, Object[] params) {
        Map<ObjectEntity, CalcPropertyInterfaceImplement<P>> mapObjects = new HashMap<>();

        int i = 0;
        while (i < params.length) {
            ObjectEntity object = (ObjectEntity)params[i];

            ArrayList<Object> objectImplement = new ArrayList<>();
            while (++i < params.length && !(params[i] instanceof ObjectEntity)) {
                objectImplement.add(params[i]);
            }

            // знаем, что только один будет
            mapObjects.put(object, readCalcImplements(eaProp.listInterfaces, objectImplement.toArray()).single());
        }
        return mapObjects;
    }

}
