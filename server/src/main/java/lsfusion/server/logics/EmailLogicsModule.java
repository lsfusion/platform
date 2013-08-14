package lsfusion.server.logics;

import org.antlr.runtime.RecognitionException;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.server.classes.ConcreteCustomClass;
import lsfusion.server.classes.StringClass;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.form.entity.FormEntity;
import lsfusion.server.form.entity.ObjectEntity;
import lsfusion.server.logics.linear.LAP;
import lsfusion.server.logics.linear.LCP;
import lsfusion.server.logics.property.CalcPropertyInterfaceImplement;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.PropertyInterface;
import lsfusion.server.logics.property.group.AbstractGroup;
import lsfusion.server.logics.scripted.ScriptingLogicsModule;
import lsfusion.server.mail.AttachmentFormat;
import lsfusion.server.mail.EmailActionProperty;

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
import static lsfusion.server.logics.ServerResourceBundle.getString;

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
        notification = (ConcreteCustomClass) getClassByName("Notification");
    }

    @Override
    public void initProperties() throws RecognitionException {
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

        fromAddress = getLCPByName("fromAddress");
    }

    public LAP addEAProp(ValueClass... params) {
        return addEAProp((String) null, params);
    }

    public LAP addEAProp(LCP fromAddress, ValueClass... params) {
        return addEAProp(null, fromAddress, emailBlindCarbonCopy, params);
    }

    public LAP addEAProp(String subject, ValueClass... params) {
        return addEAProp(subject, getLCPByName("fromAddress"), emailBlindCarbonCopy, params);
    }

    public LAP addEAProp(LCP fromAddress, LCP emailBlindCarbonCopy, ValueClass... params) {
        return addEAProp(null, fromAddress, emailBlindCarbonCopy, params);
    }

    public LAP addEAProp(String subject, LCP fromAddress, LCP emailBlindCarbonCopy, ValueClass... params) {
        return addEAProp(null, genSID(), "emailContact", subject, fromAddress, emailBlindCarbonCopy, params);
    }

    public LAP addEAProp(AbstractGroup group, String name, String caption, String subject, LCP fromAddress, LCP emailBlindCarbonCopy, ValueClass... params) {
        Object[] fromImplement = new Object[] {fromAddress};
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

        LAP eaPropLP = addEAProp(group, name, caption, params, fromImplement, subjImplement);
        addEARecipientsType(eaPropLP, Message.RecipientType.BCC, emailBlindCarbonCopy);

        return eaPropLP;
    }

    public LAP<ClassPropertyInterface> addEAProp(AbstractGroup group, String name, String caption, ValueClass[] params, Object[] fromAddress, Object[] subject) {
        EmailActionProperty eaProp = new EmailActionProperty(name, caption, params);
        LAP<ClassPropertyInterface> eaPropLP = addProperty(group, new LAP<ClassPropertyInterface>(eaProp));

        if (fromAddress != null) {
            eaProp.setFromAddress(readCalcImplements(eaPropLP.listInterfaces, fromAddress).single());
        }

        if (subject != null) {
            eaProp.setSubject(readCalcImplements(eaPropLP.listInterfaces, subject).single());
        }

        return eaPropLP;
    }

    public void addEARecipientsType(LAP eaProp, Message.RecipientType type, Object... params) {
        ImList<CalcPropertyInterfaceImplement<ClassPropertyInterface>> recipImpls = readCalcImplements(eaProp.listInterfaces, params);

        for (CalcPropertyInterfaceImplement<ClassPropertyInterface> recipImpl : recipImpls) {
            ((EmailActionProperty) eaProp.property).addRecipient(recipImpl, type);
        }
    }

    public void addEARecipients(LAP eaProp, Object... params) {
        addEARecipientsType(eaProp, MimeMessage.RecipientType.TO, params);
    }

    public void addInlineEAForm(LAP eaProp, FormEntity form, Object... params) {
        ((EmailActionProperty) eaProp.property).addInlineForm(form, readObjectImplements(eaProp, params));
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
        ((EmailActionProperty) eaProp.property).addAttachmentForm(form, format, readObjectImplements(eaProp, params), attachNameImpl);
    }

    private <P extends PropertyInterface> Map<ObjectEntity, CalcPropertyInterfaceImplement<P>> readObjectImplements(LAP<P> eaProp, Object[] params) {
        Map<ObjectEntity, CalcPropertyInterfaceImplement<P>> mapObjects = new HashMap<ObjectEntity, CalcPropertyInterfaceImplement<P>>();

        int i = 0;
        while (i < params.length) {
            ObjectEntity object = (ObjectEntity)params[i];

            ArrayList<Object> objectImplement = new ArrayList<Object>();
            while (++i < params.length && !(params[i] instanceof ObjectEntity)) {
                objectImplement.add(params[i]);
            }

            // знаем, что только один будет
            mapObjects.put(object, readCalcImplements(eaProp.listInterfaces, objectImplement.toArray()).single());
        }
        return mapObjects;
    }

}
