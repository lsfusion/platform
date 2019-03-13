package lsfusion.server.physics.dev.integration.external.to.mail;

import lsfusion.base.file.FileData;
import lsfusion.base.file.RawFileData;
import lsfusion.interop.action.MessageClientAction;
import lsfusion.server.ServerLoggers;
import lsfusion.server.logics.classes.DynamicFormatFileClass;
import lsfusion.server.logics.classes.FileClass;
import lsfusion.server.logics.classes.StaticFormatFileClass;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.type.Type;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.EmailLogicsModule;
import lsfusion.server.logics.NullValue;
import lsfusion.server.logics.ObjectValue;
import lsfusion.server.physics.dev.i18n.LocalizedString;
import lsfusion.server.logics.property.CalcPropertyInterfaceImplement;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.property.PropertyInterface;
import lsfusion.server.logics.property.actions.SystemExplicitActionProperty;
import org.apache.log4j.Logger;

import javax.mail.Message;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static javax.mail.Message.RecipientType.TO;
import static lsfusion.base.BaseUtils.nullTrim;
import static lsfusion.server.base.context.ThreadLocalContext.localize;
import static org.apache.commons.lang3.StringUtils.trimToNull;

public class SendEmailActionProperty extends SystemExplicitActionProperty {
    private final static Logger logger = ServerLoggers.mailLogger;

    private CalcPropertyInterfaceImplement<ClassPropertyInterface> fromAddressAccount;
    private CalcPropertyInterfaceImplement<ClassPropertyInterface> subject;

    private List<CalcPropertyInterfaceImplement<ClassPropertyInterface>> recipients = new ArrayList<>();
    private List<Message.RecipientType> recipientTypes = new ArrayList<>();

    private final List<CalcPropertyInterfaceImplement> attachFileNames = new ArrayList<>();
    private final List<CalcPropertyInterfaceImplement> attachFiles = new ArrayList<>();
    private final List<CalcPropertyInterfaceImplement> inlineFiles = new ArrayList<>();

    public SendEmailActionProperty(LocalizedString caption, ValueClass[] classes) {
        super(caption, classes);

        drawOptions.setAskConfirm(true);
        drawOptions.setImage("email.png");
    }

    @Override
    protected boolean isSync() { // формы используются, не определишь getUsedProps
        return true;
    }

    public void setFromAddressAccount(CalcPropertyInterfaceImplement<ClassPropertyInterface> fromAddressAccount) {
        this.fromAddressAccount = fromAddressAccount;
    }

    public void setSubject(CalcPropertyInterfaceImplement<ClassPropertyInterface> subject) {
        this.subject = subject;
    }

    public <R extends PropertyInterface> void addRecipient(CalcPropertyInterfaceImplement<ClassPropertyInterface> recipient, Message.RecipientType type) {
        recipients.add(recipient);
        recipientTypes.add(type);
    }

    public void addAttachmentFile(CalcPropertyInterfaceImplement fileName, CalcPropertyInterfaceImplement file) {
        attachFileNames.add(fileName);
        attachFiles.add(file);
    }

    public void addInlineFile(CalcPropertyInterfaceImplement file) {
        inlineFiles.add(file);
    }

    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        EmailLogicsModule emailLM = context.getBL().emailLM;
        try {
            Map<String, Message.RecipientType> recipients = getRecipientEmails(context);
            String fromAddress = fromAddressAccount != null ? (String) fromAddressAccount.read(context, context.getKeys()) : null;
            ObjectValue account = emailLM.inboxAccount.readClasses(context, fromAddress != null ? new DataObject(fromAddress) : NullValue.instance);

            if (account instanceof DataObject) {
                String encryptedConnectionType = (String) emailLM.nameEncryptedConnectionTypeAccount.read(context, account);
                String smtpHostAccount = (String) emailLM.smtpHostAccount.read(context, account);
                String smtpPortAccount = (String) emailLM.smtpPortAccount.read(context, account);

                String fromAddressAccount = (String) (fromAddress != null ? fromAddress : emailLM.fromAddressAccount.read(context, account));

                String subject = this.subject != null ? (String) this.subject.read(context, context.getKeys()) : localize("{mail.nosubject}");
                String nameAccount = (String) emailLM.nameAccount.read(context, account);
                String passwordAccount = (String) emailLM.passwordAccount.read(context, account);
                
                if (emailLM.disableAccount.read(context, account) != null) {
                    logger.error(localize("{mail.disabled}"));
                    return;
                }

                if (smtpHostAccount == null || fromAddressAccount == null) {
                    logError(context, localize("{mail.smtp.host.or.sender.not.specified.letters.will.not.be.sent}"));
                    return;
                }

                if (recipients.isEmpty()) {
                    logError(context, localize("{mail.recipient.not.specified}"));
                    return;
                }

                EmailSender sender = new EmailSender(nullTrim(smtpHostAccount), nullTrim(smtpPortAccount), nullTrim(encryptedConnectionType), nullTrim(fromAddressAccount), nullTrim(nameAccount),nullTrim(passwordAccount), recipients);

                List<EmailSender.AttachmentFile> attachFiles = new ArrayList<>();
                List<String> inlineFiles = new ArrayList<>();
                proceedFiles(context, attachFiles, inlineFiles);
                
                sender.sendMail(context, subject, inlineFiles, attachFiles);
            }
        } catch (Throwable e) {
            String errorMessage = localize("{mail.failed.to.send.mail}") + " : " + e.toString();
            logger.error(errorMessage);
            context.delayUserInterfaction(new MessageClientAction(errorMessage, localize("{mail.sending}")));

            logError(context, localize("{mail.failed.to.send.mail}") + " : " + e.toString());
            e.printStackTrace();
        }
    }

    private Map<String, Message.RecipientType> getRecipientEmails(ExecutionContext context) throws SQLException, SQLHandledException {
        assert recipients.size() == recipientTypes.size();

        Pattern p = Pattern.compile("^([A-Za-z0-9_-]+\\.)*[A-Za-z0-9_-]+@[A-Za-z0-9_-]+(\\.[A-Za-z0-9_-]+)*\\.[A-Za-z]{2,10}$");

        Map<String, Message.RecipientType> recipientEmails = new HashMap<>();
        for (int i = 0; i < recipients.size(); ++i) {
            CalcPropertyInterfaceImplement<ClassPropertyInterface> recipient = recipients.get(i);
            Message.RecipientType recipientType = recipientTypes.get(i);

            String recipientEmailList = (String) recipient.read(context, context.getKeys());
            if (recipientEmailList != null) {
                String[] emails = recipientEmailList.replace(',',';').replace(":", ";").split(";");
                for (String email : emails) {
                    email = trimToNull(email);
                    if (email == null || !p.matcher(email).matches()) {
                        if(email != null)
                            context.requestUserInteraction(new MessageClientAction("Invalid email: " + email, "Invalid email"));
                        continue;
                    }

                    // приоритет отдается TO, так как без него письмо не улетит
                    if (TO.equals(recipientType) || !recipientEmails.containsKey(email)) {
                        recipientEmails.put(email, recipientType);
                    }
                }
            }
        }
        return recipientEmails;
    }
    
    private void proceedFiles(ExecutionContext<ClassPropertyInterface> context, List<EmailSender.AttachmentFile> attachments, List<String> customInlines) throws SQLException, SQLHandledException {
        for (int i = 0; i < attachFileNames.size(); i++) {
            String name;
            CalcPropertyInterfaceImplement attachFileNameProp = attachFileNames.get(i);
            if (attachFileNameProp != null) {
                 name = (String) attachFileNameProp.read(context, context.getKeys());
            } else {
                 name = "attachment" + (i + 1);
            }

            ObjectValue fileObject = attachFiles.get(i).readClasses(context, context.getKeys());
            if (fileObject instanceof DataObject) {
                Type objectType = ((DataObject)fileObject).getType();
                String extension;
                RawFileData rawFile;
                if (objectType instanceof StaticFormatFileClass) {
                    rawFile = (RawFileData) fileObject.getValue();
                    extension = ((StaticFormatFileClass) objectType).getOpenExtension(rawFile);
                } else {
                    FileData file = (FileData) fileObject.getValue();
                    extension = file.getExtension();
                    rawFile = file.getRawFile();
                }
                attachments.add(new EmailSender.AttachmentFile(rawFile, name + "." + extension, extension));
            }
        }

        for (CalcPropertyInterfaceImplement inlineFile : this.inlineFiles) {
            ObjectValue inlineObject = inlineFile.readClasses(context, context.getKeys());
            if (inlineObject instanceof DataObject) {
                Object inlineValue = inlineObject.getValue();
                Type type = ((DataObject) inlineObject).getType();
                String inlineText;
                if(type instanceof FileClass) {
                    RawFileData rawFile;
                    if (type instanceof DynamicFormatFileClass) {
                        rawFile = ((FileData) inlineValue).getRawFile();
                    } else {
                        rawFile = (RawFileData) inlineValue;
                    }
                    inlineText = new String(rawFile.getBytes());
                } else {
                    inlineText = type.formatString(inlineValue);
                }
                customInlines.add(inlineText);
            }
        }
    }

    private void logError(ExecutionContext context, String errorMessage) {
        logger.error(errorMessage);
        context.delayUserInterfaction(new MessageClientAction(errorMessage, localize("{mail.sending}")));
    }
}
