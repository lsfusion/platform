package lsfusion.server.physics.dev.integration.external.to.mail;

import lsfusion.server.language.ScriptingLogicsModule;
import lsfusion.server.language.action.LA;
import lsfusion.server.language.property.LP;
import lsfusion.server.logics.BaseLogicsModule;
import lsfusion.server.logics.BusinessLogics;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.classes.user.ConcreteCustomClass;
import lsfusion.server.logics.form.struct.group.Group;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.physics.dev.i18n.LocalizedString;
import org.antlr.runtime.RecognitionException;

import java.io.IOException;

public class EmailLogicsModule extends ScriptingLogicsModule{

    public ConcreteCustomClass notification;

    public LP nameAccount;
    public LP passwordAccount;
    public LP disableAccount;
    public LP insecureSSLAccount;

    public LP smtpHostAccount;
    public LP smtpPortAccount;

    public LP nameEncryptedConnectionTypeAccount;

    public LP fromAddressAccount;
    public LP inboxAccount;

    public LP receiveHostAccount;
    public LP receivePortAccount;

    public LP nameReceiveAccountTypeAccount;
    public LP startTLS;

    public LP deleteMessagesAccount;
    public LP lastDaysAccount;
    public LP maxMessagesAccount;
    public LP unpackAccount;

    public EmailLogicsModule(BusinessLogics BL, BaseLogicsModule baseLM) throws IOException {
        super(baseLM, BL, "/system/Email.lsf");
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
        insecureSSLAccount = findProperty("insecureSSL[Account]");

        // Sending
        smtpHostAccount = findProperty("smtpHost[Account]");
        smtpPortAccount = findProperty("smtpPort[Account]");

        nameEncryptedConnectionTypeAccount = findProperty("nameEncryptedConnectionType[Account]");

        fromAddressAccount = findProperty("fromAddress[Account]");
        inboxAccount = findProperty("inboxAccount[STRING[100]]");

        // Receiving
        receiveHostAccount = findProperty("receiveHost[Account]");
        receivePortAccount = findProperty("receivePort[Account]");

        nameReceiveAccountTypeAccount = findProperty("nameReceiveAccountType[Account]");
        startTLS = findProperty("startTLS[Account]");

        deleteMessagesAccount = findProperty("deleteMessages[Account]");
        lastDaysAccount = findProperty("lastDays[Account]");
        maxMessagesAccount = findProperty("maxMessages[Account]");
        unpackAccount = findProperty("unpack[Account]");
    }

    public LA<PropertyInterface> addEAProp(Group group, LocalizedString caption, int paramsCount, boolean syncType) {
        return addAction(group, new LA<>(new SendEmailAction(caption, paramsCount, syncType)));
    }

}
