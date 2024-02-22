package lsfusion.server.physics.dev.integration.external.to.mail;

import lsfusion.base.EscapeUtils;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.base.file.FileData;
import lsfusion.base.file.RawFileData;
import lsfusion.interop.action.MessageClientAction;
import lsfusion.server.base.AppServerImage;
import lsfusion.server.data.expr.key.KeyExpr;
import lsfusion.server.data.query.build.QueryBuilder;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.type.Type;
import lsfusion.server.data.value.DataObject;
import lsfusion.server.data.value.NullValue;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.language.property.LP;
import lsfusion.server.logics.action.BaseAction;
import lsfusion.server.logics.action.SystemAction;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.action.flow.ChangeFlowType;
import lsfusion.server.logics.action.flow.FlowResult;
import lsfusion.server.logics.action.session.change.modifier.Modifier;
import lsfusion.server.logics.classes.data.file.DynamicFormatFileClass;
import lsfusion.server.logics.classes.data.file.FileClass;
import lsfusion.server.logics.classes.data.file.StaticFormatFileClass;
import lsfusion.server.logics.property.implement.PropertyInterfaceImplement;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.physics.admin.log.ServerLoggers;
import lsfusion.server.physics.dev.i18n.LocalizedString;

import javax.mail.Message;
import javax.mail.MessagingException;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static javax.mail.Message.RecipientType.TO;
import static lsfusion.base.BaseUtils.*;
import static lsfusion.server.base.controller.thread.ThreadLocalContext.localize;

public class SendEmailAction extends SystemAction {
    private PropertyInterfaceImplement<PropertyInterface> fromAddress;
    private PropertyInterfaceImplement<PropertyInterface> subject;

    private List<PropertyInterfaceImplement<PropertyInterface>> recipients = new ArrayList<>();
    private List<Message.RecipientType> recipientTypes = new ArrayList<>();

    private final List<PropertyInterfaceImplement> attachFileNames = new ArrayList<>();
    private final List<PropertyInterfaceImplement> attachFiles = new ArrayList<>();

    private final List<LP> attachFileNameProps = new ArrayList<>();
    private final List<LP> attachFileProps = new ArrayList<>();

    private final List<PropertyInterfaceImplement> inlineFiles = new ArrayList<>();
    private final Boolean syncType;

    public static void setDrawOptions(BaseAction action) {
        action.drawOptions.setAskConfirm(true);
        action.setImage(AppServerImage.EMAIL);
    }

    public SendEmailAction(LocalizedString caption, int paramsCount, boolean syncType) {
        super(caption, SetFact.toOrderExclSet(paramsCount, i -> new PropertyInterface()));

        setDrawOptions(this);
        this.syncType = syncType;
    }

    public void setFromAddress(PropertyInterfaceImplement<PropertyInterface> fromAddress) {
        this.fromAddress = fromAddress;
    }

    public void setSubject(PropertyInterfaceImplement<PropertyInterface> subject) {
        this.subject = subject;
    }

    public void addRecipient(PropertyInterfaceImplement<PropertyInterface> recipient, Message.RecipientType type) {
        this.recipients.add(recipient);
        this.recipientTypes.add(type);
    }

    public void addAttachmentFile(PropertyInterfaceImplement fileName, PropertyInterfaceImplement file) {
        this.attachFileNames.add(fileName);
        this.attachFiles.add(file);
    }

    public void addAttachmentFileProp(LP fileNameProp, LP fileProp) {
        this.attachFileNameProps.add(fileNameProp);
        this.attachFileProps.add(fileProp);
    }

    public void addInlineFile(PropertyInterfaceImplement file) {
        this.inlineFiles.add(file);
    }

    @Override
    protected FlowResult aspectExecute(ExecutionContext<PropertyInterface> context) {
        EmailLogicsModule emailLM = context.getBL().emailLM;
        try {
            Map<String, Message.RecipientType> recipients = getRecipientEmails(context);
            String fromAddress = this.fromAddress != null ? trimToNull((String) this.fromAddress.read(context, context.getKeys())) : null;
            ObjectValue account = emailLM.inboxAccount.readClasses(context, fromAddress != null ? new DataObject(fromAddress) : NullValue.instance);

            if (account instanceof DataObject) {
                String encryptedConnectionType = trimToEmpty((String) emailLM.nameEncryptedConnectionTypeAccount.read(context, account));
                String smtpHost = trim((String) emailLM.smtpHostAccount.read(context, account));
                String smtpPort = trimToEmpty((String) emailLM.smtpPortAccount.read(context, account));
                boolean insecureSSL = emailLM.insecureSSLAccount.read(context, account) != null;

                if(fromAddress == null) {
                    fromAddress = trimToNull((String) emailLM.fromAddressAccount.read(context, account));
                }

                String subject = this.subject != null ? (String) this.subject.read(context, context.getKeys()) : localize("{mail.nosubject}");
                String user = trimToEmpty((String) emailLM.nameAccount.read(context, account));
                String password = trimToEmpty((String) emailLM.passwordAccount.read(context, account));

                if (emailLM.disableAccount.read(context, account) != null) {
                    logErrorAndShowMessage(context, localize("{mail.disabled}"));
                    return FlowResult.FINISH;
                }

                if (smtpHost == null || fromAddress == null) {
                    logErrorAndShowMessage(context, localize("{mail.smtp.host.or.sender.not.specified.letters.will.not.be.sent}"));
                    return FlowResult.FINISH;
                }

                if (recipients.isEmpty()) {
                    logErrorAndShowMessage(context, localize("{mail.recipient.not.specified}"));
                    return FlowResult.FINISH;
                }

                List<EmailSender.AttachmentFile> attachFiles = new ArrayList<>();
                List<String> inlineFiles = new ArrayList<>();
                proceedFiles(context, attachFiles, inlineFiles);

                EmailSender.sendMail(context, fromAddress, recipients, subject, inlineFiles, attachFiles, smtpHost, smtpPort, encryptedConnectionType, user, password, syncType, insecureSSL);
            } else {
                throw new RuntimeException(localize("{mail.failed.email.not.configured}"));
            }
        } catch (SQLException | SQLHandledException | MessagingException | IOException | GeneralSecurityException e) {
            logErrorAndShowMessage(context, localize("{mail.failed.to.send.mail}") + " : " + e);
        }

        return FlowResult.FINISH;
    }

    private Map<String, Message.RecipientType> getRecipientEmails(ExecutionContext context) throws SQLException, SQLHandledException {
        assert recipients.size() == recipientTypes.size();

        Pattern p = Pattern.compile("^([A-Za-z0-9'_-]+\\.)*[A-Za-z0-9'_-]+@[A-Za-z0-9_-]+(\\.[A-Za-z0-9_-]+)*\\.[A-Za-z]{2,10}$");

        Map<String, Message.RecipientType> recipientEmails = new HashMap<>();
        for (int i = 0; i < recipients.size(); ++i) {
            PropertyInterfaceImplement<PropertyInterface> recipient = recipients.get(i);
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
    
    private void proceedFiles(ExecutionContext<PropertyInterface> context, List<EmailSender.AttachmentFile> attachments, List<String> customInlines) throws SQLException, SQLHandledException {
        int attachmentCount = 0;
        for (int i = 0; i < attachFileNames.size(); i++) {
            attachmentCount++;
            ObjectValue fileObject = attachFiles.get(i).readClasses(context);
            if (fileObject instanceof DataObject) {
                PropertyInterfaceImplement attachFileNameProp = attachFileNames.get(i);
                String name = nvl(attachFileNameProp != null ? (String) attachFileNameProp.read(context, context.getKeys()) : null, "attachment" + attachmentCount);
                attachments.add(getAttachFile(fileObject, name));
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
                    inlineText = type.formatString(inlineValue, true);
                }
                customInlines.add(EscapeUtils.toHtml(inlineText));
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
                String name = nvl((String) result.getValue(i).get("fileName").getValue(), "attachment" + attachmentCount);
                attachments.add(getAttachFile(fileObject, name));
            }
        }

        return attachments;
    }

    private EmailSender.AttachmentFile getAttachFile(ObjectValue fileObject, String name) {
        Type objectType = fileObject.getType();
        RawFileData rawFile;
        String extension;
        if (objectType instanceof StaticFormatFileClass) {
            rawFile = (RawFileData) fileObject.getValue();
            extension = ((StaticFormatFileClass) objectType).getOpenExtension(rawFile);
        } else {
            FileData file = (FileData) fileObject.getValue();
            rawFile = file.getRawFile();
            extension = file.getExtension();
        }
        return new EmailSender.AttachmentFile(rawFile, name + "." + extension, extension);
    }

    private void logErrorAndShowMessage(ExecutionContext context, String errorMessage) {
        ServerLoggers.mailLogger.error(errorMessage);
        context.delayUserInterfaction(new MessageClientAction(errorMessage, localize("{mail.sending}")));
    }

    @Override
    public boolean hasFlow(ChangeFlowType type) {
        if(type == ChangeFlowType.SYNC)
            return true;
        return super.hasFlow(type);
    }
}
