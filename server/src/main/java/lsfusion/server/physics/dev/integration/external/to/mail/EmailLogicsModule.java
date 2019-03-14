package lsfusion.server.physics.dev.integration.external.to.mail;

import lsfusion.server.logics.BaseLogicsModule;
import lsfusion.server.logics.BusinessLogics;
import lsfusion.server.logics.classes.ConcreteCustomClass;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.physics.dev.i18n.LocalizedString;
import lsfusion.server.language.linear.LAP;
import lsfusion.server.language.linear.LCP;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.logics.form.struct.group.AbstractGroup;
import lsfusion.server.language.ScriptingLogicsModule;
import org.antlr.runtime.RecognitionException;

import java.io.IOException;

public class EmailLogicsModule extends ScriptingLogicsModule{

    public ConcreteCustomClass notification;

    public LCP nameAccount;
    public LCP passwordAccount;
    public LCP disableAccount;

    public LCP smtpHostAccount;
    public LCP smtpPortAccount;

    public LCP nameEncryptedConnectionTypeAccount;

    public LCP fromAddressAccount;
    public LCP inboxAccount;

    public LCP emailSent;

    public LCP receiveHostAccount;
    public LCP receivePortAccount;

    public LCP nameReceiveAccountTypeAccount;

    public LCP deleteMessagesAccount;
    public LCP lastDaysAccount;
    public LCP maxMessagesAccount;
    public LCP unpackAccount;

    public EmailLogicsModule(BusinessLogics BL, BaseLogicsModule baseLM) throws IOException {
        super(EmailLogicsModule.class.getResourceAsStream("/system/Email.lsf"), "/system/Email.lsf", baseLM, BL);
    }

    @Override
    public void initMetaAndClasses() throws RecognitionException {
        super.initMetaAndClasses();
    }

    @Override
    public void initMainLogic() throws RecognitionException {
        super.initMainLogic();

        nameAccount = findProperty("name[Account]");
        passwordAccount = findProperty("password[Account]");
        disableAccount = findProperty("disable[Account]");

        // Sending
        smtpHostAccount = findProperty("smtpHost[Account]");
        smtpPortAccount = findProperty("smtpPort[Account]");

        nameEncryptedConnectionTypeAccount = findProperty("nameEncryptedConnectionType[Account]");

        fromAddressAccount = findProperty("fromAddress[Account]");
        inboxAccount = findProperty("inboxAccount[VARSTRING[100]]");

        emailSent = findProperty("emailSent[]");

        // Receiving
        receiveHostAccount = findProperty("receiveHost[Account]");
        receivePortAccount = findProperty("receivePort[Account]");

        nameReceiveAccountTypeAccount = findProperty("nameReceiveAccountType[Account]");
        
        deleteMessagesAccount = findProperty("deleteMessages[Account]");
        lastDaysAccount = findProperty("lastDays[Account]");
        maxMessagesAccount = findProperty("maxMessages[Account]");
        unpackAccount = findProperty("unpack[Account]");
    }

    public LAP<ClassPropertyInterface> addEAProp(AbstractGroup group, LocalizedString caption, ValueClass[] params) {
        return addProperty(group, new LAP<>(new SendEmailActionProperty(caption, params)));
    }

}
