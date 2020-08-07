package lsfusion.server.physics.dev.integration.external.to.mail;

import lsfusion.server.language.ScriptingLogicsModule;
import lsfusion.server.language.action.LA;
import lsfusion.server.language.property.LP;
import lsfusion.server.logics.BaseLogicsModule;
import lsfusion.server.logics.BusinessLogics;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.classes.user.ConcreteCustomClass;
import lsfusion.server.logics.form.struct.group.Group;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.dev.i18n.LocalizedString;
import org.antlr.runtime.RecognitionException;

import java.io.IOException;

public class EmailLogicsModule extends ScriptingLogicsModule{

    public ConcreteCustomClass notification;

    public LP nameAccount;
    public LP passwordAccount;
    public LP disableAccount;

    public LP smtpHostAccount;
    public LP smtpPortAccount;

    public LP nameEncryptedConnectionTypeAccount;

    public LP fromAddressAccount;
    public LP inboxAccount;

    public LP emailSent;

    public LP receiveHostAccount;
    public LP receivePortAccount;

    public LP nameReceiveAccountTypeAccount;

    public LP deleteMessagesAccount;
    public LP lastDaysAccount;
    public LP maxMessagesAccount;
    public LP unpackAccount;

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
        inboxAccount = findProperty("inboxAccount[STRING[100]]");

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

    public LA<ClassPropertyInterface> addEAProp(Group group, LocalizedString caption, ValueClass[] params, Boolean syncType) {
        return addAction(group, new LA<>(new SendEmailAction(caption, params, syncType)));
    }

}
