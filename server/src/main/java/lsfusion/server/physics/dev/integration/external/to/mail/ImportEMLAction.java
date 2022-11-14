package lsfusion.server.physics.dev.integration.external.to.mail;

import lsfusion.base.BaseUtils;
import lsfusion.base.file.FileData;
import lsfusion.interop.action.MessageClientAction;
import lsfusion.server.data.value.DataObject;
import lsfusion.server.data.value.NullValue;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.classes.user.ConcreteCustomClass;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.admin.log.ServerLoggers;

import javax.mail.Address;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;

import static lsfusion.server.base.controller.thread.ThreadLocalContext.localize;
import static lsfusion.base.DateConverter.sqlTimestampToLocalDateTime;

public class ImportEMLAction extends EmailAction {
    private final ClassPropertyInterface accountInterface;
    private final ClassPropertyInterface uidInterface;
    private final ClassPropertyInterface emlInterface;

    public ImportEMLAction(EmailLogicsModule LM, ValueClass... classes) {
        super(LM, classes);

        Iterator<ClassPropertyInterface> i = interfaces.iterator();
        accountInterface = i.next();
        uidInterface = i.next();
        emlInterface = i.next();
    }

    public void executeInternal(ExecutionContext<ClassPropertyInterface> context) {

        try {

            DataObject accountObject = context.getDataKeyValue(accountInterface);
            DataObject uidObject = context.getDataKeyValue(uidInterface);
            FileData emlFile = (FileData) context.getDataKeyValue(emlInterface).object;

            boolean unpack = emailLM.unpackAccount.read(context, accountObject) != null;
            String nameAccount = (String) emailLM.nameAccount.read(context, accountObject);

            Email email = parseEML(unpack, emlFile);

            try (ExecutionContext.NewSession session = context.newSession()) {

                ObjectValue emailObject = findProperty("emailAccountUID[Account,LONG]").readClasses(session, accountObject, uidObject);
                if (emailObject instanceof NullValue) {
                    //обратная совместимость
                    emailObject = findProperty("emailId[STRING[100]]").readClasses(session, new DataObject(email.id));
                    if (emailObject instanceof NullValue) {
                        emailObject = session.addObject((ConcreteCustomClass) emailLM.findClass("Email"));
                    }
                }

                emailLM.findProperty("account[Email]").change(accountObject, session, (DataObject) emailObject);
                emailLM.findProperty("uid[Email]").change(uidObject.object, session, (DataObject) emailObject);
                emailLM.findProperty("id[Email]").change(email.id, session, (DataObject) emailObject);
                emailLM.findProperty("dateTimeSent[Email]").change(email.dateTimeSent, session, (DataObject) emailObject);
                emailLM.findProperty("dateTimeReceived[Email]").change(LocalDateTime.now(), session, (DataObject) emailObject);
                emailLM.findProperty("fromAddress[Email]").change(email.fromAddress, session, (DataObject) emailObject);
                emailLM.findProperty("toAddress[Email]").change(nameAccount, session, (DataObject) emailObject);
                emailLM.findProperty("subject[Email]").change(email.subject, session, (DataObject) emailObject);
                emailLM.findProperty("message[Email]").change(email.message, session, (DataObject) emailObject);
                emailLM.findProperty("emlFile[Email]").change(emlFile, session, (DataObject) emailObject);

                for (EmailAttachment attachment : email.attachments) {
                    DataObject attachmentObject = session.addObject((ConcreteCustomClass) emailLM.findClass("AttachmentEmail"));
                    emailLM.findProperty("email[AttachmentEmail]").change(emailObject, session, attachmentObject);
                    emailLM.findProperty("id[AttachmentEmail]").change(String.valueOf(attachment.id), session, attachmentObject);
                    emailLM.findProperty("name[AttachmentEmail]").change(BaseUtils.getFileName(attachment.name), session, attachmentObject);
                    emailLM.findProperty("file[AttachmentEmail]").change(attachment.file, session, attachmentObject);
                }
                String result = session.applyMessage();
                if (result != null) {
                    throw new RuntimeException(result);
                }
            }

        } catch (Exception e) {
            logger.error(localize("{mail.failed.to.receive.mail}"), e);
            context.delayUserInterfaction(new MessageClientAction(localize("{mail.failed.to.receive.mail}") + " : " + e, localize("{mail.receiving}")));
        }

    }

    private Email parseEML(boolean unpack, FileData eml) throws MessagingException, IOException {

        Session emailSession = Session.getInstance(new Properties());
        MimeMessage message = new MimeMessage(emailSession, new ByteArrayInputStream(eml.getRawFile().getBytes()));

        Timestamp dateTimeSent = getSentDate(message);
        Address[] fromAddresses = message.getFrom();
        String fromAddress = fromAddresses.length > 0 ? ((InternetAddress) fromAddresses[0]).getAddress() : null;
        String subjectEmail = message.getSubject();

        Object messageContent = EmailReceiver.getEmailContent(message);
        EmailReceiver.MultipartBody messageEmail = EmailReceiver.getEmailMessage(subjectEmail, message, messageContent, unpack);
        if (messageEmail == null) {
            messageEmail = new EmailReceiver.MultipartBody(messageContent == null ? null : String.valueOf(messageContent), null);
            ServerLoggers.mailLogger.error("Warning: missing attachment '" + messageContent + "' from email '" + subjectEmail + "'");
        }

        List<EmailAttachment> attachments = new ArrayList<>();
        int counter = 1;
        if (messageEmail.attachments != null) {
            for (Map.Entry<String, FileData> entry : messageEmail.attachments.entrySet()) {
                attachments.add(new EmailAttachment(counter, entry.getKey(), entry.getValue()));
                counter++;
            }
        }

        return new Email(getEmailId(dateTimeSent, fromAddress, subjectEmail), sqlTimestampToLocalDateTime(dateTimeSent), fromAddress, subjectEmail, messageEmail.message, attachments);
    }

    private String getEmailId(Timestamp dateTime, String fromAddress, String subject) {
        return String.format("%s/%s/%s", dateTime == null ? "" : dateTime.getTime(), fromAddress, subject == null ? "" : subject);
    }

    private class Email {
        String id;
        LocalDateTime dateTimeSent;
        String fromAddress;
        String subject;
        String message;
        List<EmailAttachment> attachments;

        private Email(String id, LocalDateTime dateTimeSent, String fromAddress, String subject, String message, List<EmailAttachment> attachments) {
            this.id = id;
            this.dateTimeSent = dateTimeSent;
            this.fromAddress = fromAddress;
            this.subject = subject;
            this.message = message;
            this.attachments = attachments;
        }
    }

    private class EmailAttachment {
        int id;
        String name;
        FileData file;

        private EmailAttachment(int id, String name, FileData file) {
            this.id = id;
            this.name = name;
            this.file = file;
        }
    }
}