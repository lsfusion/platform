package lsfusion.server.mail;

import jasperapi.ReportGenerator;
import lsfusion.base.FileData;
import lsfusion.base.RawFileData;
import lsfusion.base.col.MapFact;
import lsfusion.interop.FormPrintType;
import lsfusion.interop.action.MessageClientAction;
import lsfusion.interop.form.ReportGenerationData;
import lsfusion.server.ServerLoggers;
import lsfusion.server.classes.DynamicFormatFileClass;
import lsfusion.server.classes.FileClass;
import lsfusion.server.classes.StaticFormatFileClass;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.type.Type;
import lsfusion.server.form.entity.FormEntity;
import lsfusion.server.form.entity.ObjectEntity;
import lsfusion.server.form.instance.FormInstance;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.EmailLogicsModule;
import lsfusion.server.logics.NullValue;
import lsfusion.server.logics.ObjectValue;
import lsfusion.server.logics.i18n.LocalizedString;
import lsfusion.server.logics.property.CalcPropertyInterfaceImplement;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.property.PropertyInterface;
import lsfusion.server.logics.property.actions.SystemExplicitActionProperty;
import lsfusion.server.form.stat.InteractiveFormReportManager;
import net.sf.jasperreports.engine.JRException;
import org.apache.log4j.Logger;

import javax.mail.Message;
import java.io.IOException;
import java.sql.SQLException;
import java.util.*;
import java.util.regex.Pattern;

import static javax.mail.Message.RecipientType.TO;
import static lsfusion.base.BaseUtils.nullTrim;
import static lsfusion.base.BaseUtils.rtrim;
import static lsfusion.server.context.ThreadLocalContext.localize;
import static org.apache.commons.lang3.StringUtils.trimToNull;

public class SendEmailActionProperty extends SystemExplicitActionProperty {
    private final static Logger logger = ServerLoggers.mailLogger;

    public enum FormStorageType {INLINE, ATTACH}

    private CalcPropertyInterfaceImplement<ClassPropertyInterface> fromAddressAccount;
    private CalcPropertyInterfaceImplement<ClassPropertyInterface> subject;

    private List<CalcPropertyInterfaceImplement<ClassPropertyInterface>> recipients = new ArrayList<>();
    private List<Message.RecipientType> recipientTypes = new ArrayList<>();

    private final List<FormEntity> forms = new ArrayList<>();
    private final List<AttachmentFormat> formats = new ArrayList<>();
    private final List<FormStorageType> storageTypes = new ArrayList<>();
    private final List<Map<ObjectEntity, CalcPropertyInterfaceImplement<ClassPropertyInterface>>> mapObjects = new ArrayList<>();
    private final List<CalcPropertyInterfaceImplement> attachmentProps = new ArrayList<>();
    private final List<CalcPropertyInterfaceImplement> attachFileNames = new ArrayList<>();
    private final List<CalcPropertyInterfaceImplement> attachFiles = new ArrayList<>();
    private final List<CalcPropertyInterfaceImplement> inlineTexts = new ArrayList<>(); // deprecated
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

    public void addInlineForm(FormEntity form, Map<ObjectEntity, CalcPropertyInterfaceImplement<ClassPropertyInterface>> objects) {
        forms.add(form);
        formats.add(AttachmentFormat.HTML);
        storageTypes.add(FormStorageType.INLINE);
        mapObjects.add(objects);
        attachmentProps.add(null);
    }

    public void addAttachmentForm(FormEntity form, AttachmentFormat format, Map<ObjectEntity, CalcPropertyInterfaceImplement<ClassPropertyInterface>> objects, CalcPropertyInterfaceImplement attachmentNameProp) {
        forms.add(form);
        formats.add(format);
        storageTypes.add(FormStorageType.ATTACH);
        mapObjects.add(objects);
        attachmentProps.add(attachmentNameProp);
    }
    
    public void addAttachmentFile(CalcPropertyInterfaceImplement fileName, CalcPropertyInterfaceImplement file) {
        attachFileNames.add(fileName);
        attachFiles.add(file);
    }

    public void addInlineText(CalcPropertyInterfaceImplement inlineText) {
        inlineTexts.add(inlineText);
    }

    public void addInlineFile(CalcPropertyInterfaceImplement file) {
        inlineFiles.add(file);
    }

    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        EmailLogicsModule emailLM = context.getBL().emailLM;
        try {
            Map<String, Message.RecipientType> recipients = getRecipientEmails(context);
            String fromAddress = (String) fromAddressAccount.read(context, context.getKeys());
            ObjectValue account = emailLM.inboxAccount.readClasses(context, fromAddress != null ? new DataObject(fromAddress) : NullValue.instance);

            if (account instanceof DataObject) {
                String encryptedConnectionType = (String) emailLM.nameEncryptedConnectionTypeAccount.read(context, account);
                String smtpHostAccount = (String) emailLM.smtpHostAccount.read(context, account);
                String smtpPortAccount = (String) emailLM.smtpPortAccount.read(context, account);

                String fromAddressAccount = (String) (fromAddress != null ? fromAddress : emailLM.fromAddressAccount.read(context, account));

                String subject = (String) this.subject.read(context, context.getKeys());
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
                proceedForms(context, attachFiles, inlineFiles);
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

    @Deprecated
    public void proceedForms(ExecutionContext<ClassPropertyInterface> context, List<EmailSender.AttachmentFile> attachments, List<String> inlineForms) throws SQLException, SQLHandledException, ClassNotFoundException, IOException, JRException {
        assert forms.size() == storageTypes.size() && forms.size() == formats.size() && forms.size() == attachmentProps.size() && forms.size() == mapObjects.size();

        for (int i = 0; i < forms.size(); i++) {
            FormEntity form = forms.get(i);
            FormStorageType storageType = storageTypes.get(i);
            AttachmentFormat attachmentFormat = formats.get(i);
            CalcPropertyInterfaceImplement attachmentProp = attachmentProps.get(i);
            Map<ObjectEntity, CalcPropertyInterfaceImplement<ClassPropertyInterface>> objectsImplements = mapObjects.get(i);

            FormInstance remoteForm = createReportForm(context, form, objectsImplements);

            // если объекты подошли
            if (remoteForm != null) {
                FormPrintType printType = attachmentFormat == null ? FormPrintType.HTML : attachmentFormat.getFormPrintType();

                ReportGenerationData generationData = new InteractiveFormReportManager(remoteForm).getReportData(printType);

                RawFileData file = ReportGenerator.exportToFileByteArray(generationData, printType);
                if (storageType == FormStorageType.INLINE)
                    inlineForms.add(new String(file.getBytes()));
                else {
                    attachments.add(createAttachment(form, printType, attachmentProp, context, file));
                }
            }
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
                 name = "attachment" + i;
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
        for(CalcPropertyInterfaceImplement inlineText : this.inlineTexts) {
            String text = (String) inlineText.read(context, context.getKeys());
            if(text != null)
                customInlines.add(text);
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

    private EmailSender.AttachmentFile createAttachment(FormEntity form, FormPrintType printType, CalcPropertyInterfaceImplement attachmentNameProp, ExecutionContext context, RawFileData file) throws SQLException, SQLHandledException {
        String attachmentName = null;
        if (attachmentNameProp != null) {
            attachmentName = (String) attachmentNameProp.read(context, context.getKeys());
        }
        if (attachmentName == null) {
            attachmentName = localize(form.getCaption());
        }
        attachmentName = rtrim(attachmentName.replace('"', '\''));

        String extension = printType.getExtension();
        
        // adding extension, because apparently not all mail clients determine it correctly from mimeType
        attachmentName += "." + extension;

        return new EmailSender.AttachmentFile(file, attachmentName, extension);
    }

    private FormInstance createReportForm(ExecutionContext context, FormEntity form, Map<ObjectEntity, CalcPropertyInterfaceImplement<ClassPropertyInterface>> objectsImplements) throws SQLException, SQLHandledException {
        Map<ObjectEntity, ObjectValue> objectValues = new HashMap<>();
        for (Map.Entry<ObjectEntity, CalcPropertyInterfaceImplement<ClassPropertyInterface>> objectImpl : objectsImplements.entrySet())
            objectValues.put(objectImpl.getKey(), objectImpl.getValue().readClasses(context, context.getKeys()));

        return context.createFormInstance(form, MapFact.fromJavaMap(objectValues));
    }

    private void logError(ExecutionContext context, String errorMessage) {
        logger.error(errorMessage);
        context.delayUserInterfaction(new MessageClientAction(errorMessage, localize("{mail.sending}")));
    }
}
