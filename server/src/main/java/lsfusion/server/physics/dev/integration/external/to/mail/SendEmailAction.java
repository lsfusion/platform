package lsfusion.server.physics.dev.integration.external.to.mail;

import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.base.file.FileData;
import lsfusion.base.file.RawFileData;
import lsfusion.interop.action.MessageClientAction;
import lsfusion.server.data.expr.key.KeyExpr;
import lsfusion.server.data.query.build.QueryBuilder;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.type.Type;
import lsfusion.server.data.value.DataObject;
import lsfusion.server.data.value.NullValue;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.language.property.LP;
import lsfusion.server.logics.action.SystemExplicitAction;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.action.session.change.modifier.Modifier;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.classes.data.file.DynamicFormatFileClass;
import lsfusion.server.logics.classes.data.file.FileClass;
import lsfusion.server.logics.classes.data.file.StaticFormatFileClass;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.logics.property.implement.PropertyInterfaceImplement;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.physics.admin.log.ServerLoggers;
import lsfusion.server.physics.dev.i18n.LocalizedString;
import org.apache.log4j.Logger;

import javax.mail.Message;
import javax.mail.MessagingException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static javax.mail.Message.RecipientType.TO;
import static lsfusion.base.BaseUtils.nullTrim;
import static lsfusion.base.BaseUtils.nvl;
import static lsfusion.server.base.controller.thread.ThreadLocalContext.localize;
import static org.apache.commons.lang3.StringUtils.trimToNull;

public class SendEmailAction extends SystemExplicitAction {
    private final static Logger logger = ServerLoggers.mailLogger;

    private PropertyInterfaceImplement<ClassPropertyInterface> fromAddressAccount;
    private PropertyInterfaceImplement<ClassPropertyInterface> subject;

    private List<PropertyInterfaceImplement<ClassPropertyInterface>> recipients = new ArrayList<>();
    private List<Message.RecipientType> recipientTypes = new ArrayList<>();

    private final List<PropertyInterfaceImplement> attachFileNames = new ArrayList<>();
    private final List<PropertyInterfaceImplement> attachFiles = new ArrayList<>();

    private final List<LP> attachFileNameProps = new ArrayList<>();
    private final List<LP> attachFileProps = new ArrayList<>();

    private final List<PropertyInterfaceImplement> inlineFiles = new ArrayList<>();
    private final Boolean syncType;

    public SendEmailAction(LocalizedString caption, ValueClass[] classes, boolean syncType) {
        super(caption, classes);

        drawOptions.setAskConfirm(true);
        drawOptions.setImage("email.png");
        this.syncType = syncType;
    }

    @Override
    protected boolean isSync() { // формы используются, не определишь getUsedProps
        return true;
    }

    public void setFromAddressAccount(PropertyInterfaceImplement<ClassPropertyInterface> fromAddressAccount) {
        this.fromAddressAccount = fromAddressAccount;
    }

    public void setSubject(PropertyInterfaceImplement<ClassPropertyInterface> subject) {
        this.subject = subject;
    }

    public <R extends PropertyInterface> void addRecipient(PropertyInterfaceImplement<ClassPropertyInterface> recipient, Message.RecipientType type) {
        recipients.add(recipient);
        recipientTypes.add(type);
    }

    public void addAttachmentFile(PropertyInterfaceImplement fileName, PropertyInterfaceImplement file) {
        attachFileNames.add(fileName);
        attachFiles.add(file);
    }

    public void addAttachmentFileProp(LP fileNameProp, LP fileProp) {
        attachFileNameProps.add(fileNameProp);
        attachFileProps.add(fileProp);
    }

    public void addInlineFile(PropertyInterfaceImplement file) {
        inlineFiles.add(file);
    }

    public void executeInternal(ExecutionContext<ClassPropertyInterface> context) {
        EmailLogicsModule emailLM = context.getBL().emailLM;
        try {
            Map<String, Message.RecipientType> recipients = getRecipientEmails(context);
            String fromAddress = fromAddressAccount != null ? trimToNull((String) fromAddressAccount.read(context, context.getKeys())) : null;
            ObjectValue account = emailLM.inboxAccount.readClasses(context, fromAddress != null ? new DataObject(fromAddress) : NullValue.instance);

            if (account instanceof DataObject) {
                String encryptedConnectionType = (String) emailLM.nameEncryptedConnectionTypeAccount.read(context, account);
                String smtpHostAccount = (String) emailLM.smtpHostAccount.read(context, account);
                String smtpPortAccount = (String) emailLM.smtpPortAccount.read(context, account);

                String fromAddressAccount = fromAddress != null ? fromAddress : trimToNull((String) emailLM.fromAddressAccount.read(context, account));

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

                EmailSender sender = new EmailSender(nullTrim(smtpHostAccount), nullTrim(smtpPortAccount), nullTrim(encryptedConnectionType), fromAddressAccount, nullTrim(nameAccount),nullTrim(passwordAccount), recipients);

                List<EmailSender.AttachmentFile> attachFiles = new ArrayList<>();
                List<String> inlineFiles = new ArrayList<>();
                proceedFiles(context, attachFiles, inlineFiles);

                sender.sendMail(context, subject, inlineFiles, attachFiles, syncType);
            } else {
                throw new RuntimeException(localize("{mail.failed.email.not.configured}"));
            }
        } catch (SQLException | SQLHandledException | MessagingException | IOException e) {
            logError(context, localize("{mail.failed.to.send.mail}") + " : " + e.toString());
        }
    }

    private Map<String, Message.RecipientType> getRecipientEmails(ExecutionContext context) throws SQLException, SQLHandledException {
        assert recipients.size() == recipientTypes.size();

        Pattern p = Pattern.compile("^([A-Za-z0-9'_-]+\\.)*[A-Za-z0-9'_-]+@[A-Za-z0-9_-]+(\\.[A-Za-z0-9_-]+)*\\.[A-Za-z]{2,10}$");

        Map<String, Message.RecipientType> recipientEmails = new HashMap<>();
        for (int i = 0; i < recipients.size(); ++i) {
            PropertyInterfaceImplement<ClassPropertyInterface> recipient = recipients.get(i);
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
        int attachmentCount = 0;
        for (int i = 0; i < attachFileNames.size(); i++) {
            attachmentCount++;
            PropertyInterfaceImplement attachFileNameProp = attachFileNames.get(i);
            String name = nvl(attachFileNameProp != null ? (String) attachFileNameProp.read(context, context.getKeys()) : null, "attachment" + attachmentCount);

            ObjectValue fileObject = attachFiles.get(i).readClasses(context);
            if (fileObject instanceof DataObject) {
                Type objectType = fileObject.getType();
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

        for (int i = 0; i < attachFileProps.size(); i++) {
            attachments.addAll(getAttachFiles(context, attachFileProps.get(i), attachFileNameProps.get(i), attachmentCount));
        }

        for (PropertyInterfaceImplement inlineFile : this.inlineFiles) {
            ObjectValue inlineObject = inlineFile.readClasses(context);
            if (inlineObject instanceof DataObject) {
                Object inlineValue = inlineObject.getValue();
                Type type = inlineObject.getType();
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

    private List<EmailSender.AttachmentFile> getAttachFiles(ExecutionContext context, LP attachFileLP, LP attachFileNameLP, int attachmentCount) throws SQLException, SQLHandledException {
        List<EmailSender.AttachmentFile> attachments = new ArrayList<>();
        KeyExpr iExpr = new KeyExpr("i");
        QueryBuilder<Object, Object> query = new QueryBuilder<>(MapFact.singletonRev("i", iExpr));
        Modifier modifier = context.getModifier();
        query.addProperty("file", attachFileLP.getExpr(modifier, iExpr));
        if (attachFileNameLP != null) {
            query.addProperty("fileName", attachFileNameLP.getExpr(modifier, iExpr));
        }
        query.and(attachFileLP.getExpr(modifier, iExpr).getWhere());

        ImOrderMap<ImMap<Object, DataObject>, ImMap<Object, ObjectValue>> result = query.executeClasses(context);

        for (int i = 0; i < result.size(); i++) {
            attachmentCount++;
            ObjectValue fileObject = result.getValue(i).get("file");
            if (fileObject instanceof DataObject) {
                Type type = fileObject.getType();
                RawFileData rawFile;
                String extension;
                if (type instanceof StaticFormatFileClass) {
                    rawFile = (RawFileData) fileObject.getValue();
                    extension = ((StaticFormatFileClass) type).getOpenExtension(rawFile);
                } else {
                    FileData file = (FileData) fileObject.getValue();
                    rawFile = file.getRawFile();
                    extension = file.getExtension();
                }

                String fileName = nvl((String) result.getValue(i).get("fileName").getValue(), "attachment" + attachmentCount);
                attachments.add(new EmailSender.AttachmentFile(rawFile, fileName + "." + extension, extension));
            }
        }

        return attachments;
    }

    private void logError(ExecutionContext context, String errorMessage) {
        logger.error(errorMessage);
        context.delayUserInterfaction(new MessageClientAction(errorMessage, localize("{mail.sending}")));
    }
}
