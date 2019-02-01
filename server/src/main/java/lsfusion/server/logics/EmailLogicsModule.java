package lsfusion.server.logics;

import lsfusion.server.classes.ConcreteCustomClass;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.logics.i18n.LocalizedString;
import lsfusion.server.logics.linear.LAP;
import lsfusion.server.logics.linear.LCP;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.group.AbstractGroup;
import lsfusion.server.logics.scripted.ScriptingLogicsModule;
import lsfusion.server.mail.SendEmailActionProperty;
import org.antlr.runtime.RecognitionException;

import java.io.IOException;

import static lsfusion.server.logics.PropertyUtils.readCalcImplements;

public class EmailLogicsModule extends ScriptingLogicsModule{

    public ConcreteCustomClass notification;
    
    public LCP inboxAccount;
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
    public LCP maxMessagesAccount;
    public LCP blindCarbonCopyAccount;
    public LCP fromAddressAccount;
    public LCP disableAccount;
    public LCP enableAccount;
    public LCP unpackAccount;

    public LAP emailUserPassUser;

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

        // ------- Управление почтой ------ //

        // Настройки почтового сервера
        inboxAccount = findProperty("inboxAccount[VARSTRING[100]]");

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
        maxMessagesAccount = findProperty("maxMessages[Account]");
        blindCarbonCopyAccount = findProperty("blindCarbonCopy[Account]");

        disableAccount = findProperty("disable[Account]");
        enableAccount = findProperty("enable[Account]");
        unpackAccount = findProperty("unpack[Account]");

        emailUserPassUser = findAction("emailUserPass[Contact]");
        
        fromAddressAccount = findProperty("fromAddress[Account]");
    }

    public LAP<ClassPropertyInterface> addEAProp(AbstractGroup group, LocalizedString caption, ValueClass[] params) {
        return addProperty(group, new LAP<>(new SendEmailActionProperty(caption, params)));
    }

}
