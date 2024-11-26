package lsfusion.server.physics.dev.integration.external.to.mail;

import lsfusion.server.language.ScriptingLogicsModule;
import lsfusion.server.language.action.LA;
import lsfusion.server.language.property.LP;
import lsfusion.server.logics.BaseLogicsModule;
import lsfusion.server.logics.BusinessLogics;
import lsfusion.server.logics.classes.user.ConcreteCustomClass;
import lsfusion.server.logics.form.struct.group.Group;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.physics.dev.i18n.LocalizedString;
import org.antlr.runtime.RecognitionException;

import java.io.IOException;

public class EmailLogicsModule extends ScriptingLogicsModule{

    public ConcreteCustomClass folder;
    public ConcreteCustomClass email;
    public ConcreteCustomClass attachmentEmail;

    public LP nameAccount;
    public LP passwordAccount;
    public LP disableAccount;
    public LP insecureSSLAccount;
    public LP readAllFoldersAccount;

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
    public LP ignoreExceptionsAccount;

    public LP idFolder;
    public LP accountFolder;
    public LP folderAccountId;
    public LP parentFolder;

    public LP folderEmail;
    public LP idEmail;
    public LP accountEmail;
    public LP emailId;
    public LP uidEmail;
    public LP emailAccountUID;
    public LP dateTimeSentEmail;
    public LP dateTimeReceivedEmail;
    public LP fromAddressEmail;
    public LP toAddressEmail;
    public LP ccAddressEmail;
    public LP bccAddressEmail;
    public LP subjectEmail;
    public LP messageEmail;
    public LP emlFileEmail;

    public LP skipFilter;

    public LP idAttachmentEmail;
    public LP emailAttachmentEmail;
    public LP attachmentEmailIdEmail;
    public LP nameAttachmentEmail;
    public LP fileAttachmentEmail;

    public LP emlFile;

    public LP saveSentEmailToDirectoryAccount;

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

        folder = (ConcreteCustomClass) findClass("Folder");
        email = (ConcreteCustomClass) findClass("Email");
        attachmentEmail = (ConcreteCustomClass) findClass("AttachmentEmail");

        nameAccount = findProperty("name[Account]");
        passwordAccount = findProperty("password[Account]");
        disableAccount = findProperty("disable[Account]");
        insecureSSLAccount = findProperty("insecureSSL[Account]");
        readAllFoldersAccount = findProperty("readAllFolders[Account]");

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
        ignoreExceptionsAccount = findProperty("ignoreExceptions[Account]");

        idFolder = findProperty("id[Folder]");
        accountFolder = findProperty("account[Folder]");
        folderAccountId = findProperty("folder[Account,STRING]");
        parentFolder = findProperty("parent[Folder]");

        folderEmail = findProperty("folder[Email]");
        idEmail = findProperty("id[Email]");
        accountEmail = findProperty("account[Email]");
        emailId = findProperty("emailId[Account,STRING]");
        uidEmail = findProperty("uid[Email]");
        emailAccountUID = findProperty("emailAccountUID[Account,LONG]");
        dateTimeSentEmail = findProperty("dateTimeSent[Email]");
        dateTimeReceivedEmail = findProperty("dateTimeReceived[Email]");
        fromAddressEmail = findProperty("fromAddress[Email]");
        toAddressEmail = findProperty("toAddress[Email]");
        ccAddressEmail = findProperty("ccAddress[Email]");
        bccAddressEmail = findProperty("bccAddress[Email]");
        subjectEmail = findProperty("subject[Email]");
        messageEmail = findProperty("message[Email]");
        emlFileEmail = findProperty("emlFile[Email]");

        skipFilter = findProperty("skipFilter[Email,Account,DATETIME]");

        idAttachmentEmail = findProperty("id[AttachmentEmail]");
        emailAttachmentEmail = findProperty("email[AttachmentEmail]");
        attachmentEmailIdEmail = findProperty("attachmentEmail[STRING,STRING]");
        nameAttachmentEmail = findProperty("name[AttachmentEmail]");
        fileAttachmentEmail = findProperty("file[AttachmentEmail]");

        emlFile = findProperty("emlFile[LONG]");

        saveSentEmailToDirectoryAccount = findProperty("saveSentEmailToDirectory[Account]");
    }

    public LA<PropertyInterface> addEAProp(Group group, LocalizedString caption, int paramsCount, boolean syncType) {
        return addAction(group, new LA<>(new SendEmailAction(caption, paramsCount, syncType)));
    }

}
