package lsfusion.server.mail;


import com.sun.mail.util.BASE64DecoderStream;
import com.sun.mail.util.MailSSLSocketFactory;
import lsfusion.base.BaseUtils;
import lsfusion.base.IOUtils;
import lsfusion.server.ServerLoggers;
import lsfusion.server.classes.ConcreteCustomClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.integration.*;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.EmailLogicsModule;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.scripted.ScriptingErrorLog;
import lsfusion.server.session.DataSession;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.internet.MimeUtility;
import java.io.*;
import java.security.GeneralSecurityException;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.*;

public class EmailReceiver {
    EmailLogicsModule LM;
    Properties mailProps = new Properties();
    DataObject accountObject;
    String receiveHostAccount;
    Integer receivePortAccount;
    String nameAccount;
    String passwordAccount;
    boolean isPOP3;
    boolean deleteMessagesAccount;
    Integer lastDaysAccount;

    public EmailReceiver(EmailLogicsModule emailLM, DataObject accountObject, String receiveHostAccount, Integer receivePortAccount,
                         String nameAccount, String passwordAccount, boolean isPOP3, boolean deleteMessagesAccount, Integer lastDaysAccount) {
            mailProps.setProperty(isPOP3 ? "mail.pop3.host" : "mail.imap.host", receiveHostAccount);
        this.LM = emailLM;
        this.accountObject = accountObject;
        this.receiveHostAccount = receiveHostAccount;
        this.receivePortAccount = receivePortAccount;
        this.nameAccount = nameAccount;
        this.passwordAccount = passwordAccount;
        this.isPOP3 = isPOP3;
        this.deleteMessagesAccount = deleteMessagesAccount;
        this.lastDaysAccount = lastDaysAccount;
    }

    public void receiveEmail(ExecutionContext context) throws MessagingException, IOException, SQLException, ScriptingErrorLog.SemanticErrorException, SQLHandledException, GeneralSecurityException {

        List<List<List<Object>>> data = downloadEmailList();

        importEmails(context, data.get(0));
        importAttachments(context, data.get(1));

        LM.findAction("formRefresh[]").execute(context);
    }

    public void importEmails(ExecutionContext context, List<List<Object>> data) throws ScriptingErrorLog.SemanticErrorException, SQLException, SQLHandledException {

        List<ImportProperty<?>> props = new ArrayList<>();
        List<ImportField> fields = new ArrayList<>();
        List<ImportKey<?>> keys = new ArrayList<>();

        ImportField idEmailField = new ImportField(LM.findProperty("id[Email]"));
        ImportKey<?> emailKey = new ImportKey((ConcreteCustomClass) LM.findClass("Email"),
                LM.findProperty("emailId[VARSTRING[100]]").getMapping(idEmailField));
        keys.add(emailKey);
        props.add(new ImportProperty(idEmailField, LM.findProperty("id[Email]").getMapping(emailKey)));
        props.add(new ImportProperty(accountObject, LM.findProperty("account[Email]").getMapping(emailKey)));
        fields.add(idEmailField);

        ImportField dateTimeSentEmailField = new ImportField(LM.findProperty("dateTimeSent[Email]"));
        props.add(new ImportProperty(dateTimeSentEmailField, LM.findProperty("dateTimeSent[Email]").getMapping(emailKey), true));
        fields.add(dateTimeSentEmailField);

        ImportField dateTimeReceivedEmailField = new ImportField(LM.findProperty("dateTimeReceived[Email]"));
        props.add(new ImportProperty(dateTimeReceivedEmailField, LM.findProperty("dateTimeReceived[Email]").getMapping(emailKey), true));
        fields.add(dateTimeReceivedEmailField);

        ImportField fromAddressEmailField = new ImportField(LM.findProperty("fromAddress[Email]"));
        props.add(new ImportProperty(fromAddressEmailField, LM.findProperty("fromAddress[Email]").getMapping(emailKey), true));
        fields.add(fromAddressEmailField);

        ImportField toAddressEmailField = new ImportField(LM.findProperty("toAddress[Email]"));
        props.add(new ImportProperty(toAddressEmailField, LM.findProperty("toAddress[Email]").getMapping(emailKey), true));
        fields.add(toAddressEmailField);

        ImportField subjectEmailField = new ImportField(LM.findProperty("subject[Email]"));
        props.add(new ImportProperty(subjectEmailField, LM.findProperty("subject[Email]").getMapping(emailKey), true));
        fields.add(subjectEmailField);

        ImportField messageEmailField = new ImportField(LM.findProperty("message[Email]"));
        props.add(new ImportProperty(messageEmailField, LM.findProperty("message[Email]").getMapping(emailKey), true));
        fields.add(messageEmailField);
        
        ImportField emlFileEmailField = new ImportField(LM.findProperty("emlFile[Email]"));
        props.add(new ImportProperty(emlFileEmailField, LM.findProperty("emlFile[Email]").getMapping(emailKey), true));
        fields.add(emlFileEmailField);
        
        ImportTable table = new ImportTable(fields, data);

        try (DataSession session = context.createSession()) {
            session.pushVolatileStats("ER_AT");
            IntegrationService service = new IntegrationService(session, table, keys, props);
            service.synchronize(true, false);
            session.apply(context);
            session.popVolatileStats();
        }
    }

    public void importAttachments(ExecutionContext context, List<List<Object>> data) throws ScriptingErrorLog.SemanticErrorException, SQLException, SQLHandledException {

        List<ImportProperty<?>> props = new ArrayList<>();
        List<ImportField> fields = new ArrayList<>();
        List<ImportKey<?>> keys = new ArrayList<>();

        ImportField idEmailField = new ImportField(LM.findProperty("id[Email]"));
        ImportKey<?> emailKey = new ImportKey((ConcreteCustomClass) LM.findClass("Email"),
                LM.findProperty("emailId[VARSTRING[100]]").getMapping(idEmailField));
        emailKey.skipKey = true;
        keys.add(emailKey);
        fields.add(idEmailField);

        ImportField idAttachmentEmailField = new ImportField(LM.findProperty("id[AttachmentEmail]"));
        ImportKey<?> attachmentEmailKey = new ImportKey((ConcreteCustomClass) LM.findClass("AttachmentEmail"),
                LM.findProperty("attachmentEmail[VARSTRING[100],VARSTRING[100]]").getMapping(idAttachmentEmailField, idEmailField));
        keys.add(attachmentEmailKey);
        props.add(new ImportProperty(idAttachmentEmailField, LM.findProperty("id[AttachmentEmail]").getMapping(attachmentEmailKey)));
        props.add(new ImportProperty(idEmailField, LM.findProperty("email[AttachmentEmail]").getMapping(attachmentEmailKey),
                LM.object(LM.findClass("Email")).getMapping(emailKey)));
        fields.add(idAttachmentEmailField);

        ImportField nameAttachmentEmailField = new ImportField(LM.findProperty("name[AttachmentEmail]"));
        props.add(new ImportProperty(nameAttachmentEmailField, LM.findProperty("name[AttachmentEmail]").getMapping(attachmentEmailKey)));
        fields.add(nameAttachmentEmailField);

        ImportField fileAttachmentEmailField = new ImportField(LM.findProperty("file[AttachmentEmail]"));
        props.add(new ImportProperty(fileAttachmentEmailField, LM.findProperty("file[AttachmentEmail]").getMapping(attachmentEmailKey)));
        fields.add(fileAttachmentEmailField);

        ImportTable table = new ImportTable(fields, data);

        try (DataSession session = context.createSession()) {
            session.pushVolatileStats("ER_EL");
            IntegrationService service = new IntegrationService(session, table, keys, props);
            service.synchronize(true, false);
            session.apply(context);
            session.popVolatileStats();
        }
    }

    public List<List<List<Object>>> downloadEmailList() throws MessagingException, SQLException, IOException, GeneralSecurityException {

        List<List<Object>> dataEmails = new ArrayList<>();
        List<List<Object>> dataAttachments = new ArrayList<>();
        //options to increase downloading big attachments
        mailProps.put("mail.imaps.partialfetch", "true");
        mailProps.put("mail.imaps.fetchsize", "819200");
        if(!isPOP3) { //imaps
            MailSSLSocketFactory socketFactory = new MailSSLSocketFactory();
            socketFactory.setTrustAllHosts(true);
            mailProps.put("mail.imaps.ssl.socketFactory", socketFactory);
            mailProps.setProperty("mail.store.protocol", "imaps");
        }
        Session emailSession = Session.getInstance(mailProps);
        Store emailStore = emailSession.getStore(isPOP3 ? "pop3" : "imaps");
        if(receivePortAccount != null)
            emailStore.connect(receiveHostAccount, receivePortAccount, nameAccount, passwordAccount);
        else
            emailStore.connect(receiveHostAccount, nameAccount, passwordAccount);

        Folder emailFolder = emailStore.getFolder("INBOX");
        emailFolder.open(Folder.READ_WRITE);

        Timestamp dateTimeReceivedEmail = new Timestamp(Calendar.getInstance().getTime().getTime());
        Timestamp minDateTime = null;
        if(lastDaysAccount != null) {
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.DATE, -lastDaysAccount);
            minDateTime = new Timestamp(calendar.getTime().getTime());
        }

        Message[] messages = emailFolder.getMessages();
        ServerLoggers.mailLogger.info(String.format("Account %s: found %s emails", nameAccount, messages.length));
        for (Message message : messages) {
            Timestamp dateTimeSentEmail = getSentDate(message);
            if(minDateTime == null || dateTimeSentEmail == null || minDateTime.compareTo(dateTimeSentEmail) <= 0) {
                message.setFlag(deleteMessagesAccount ? Flags.Flag.DELETED : Flags.Flag.SEEN, true);
                String fromAddressEmail = ((InternetAddress) message.getFrom()[0]).getAddress();
                String idEmail = (dateTimeSentEmail == null ? "" : dateTimeSentEmail.getTime()) + fromAddressEmail;
                String subjectEmail = message.getSubject();
                Object messageContent = getEmailContent(message);
                MultipartBody messageEmail = messageContent instanceof Multipart ? getMultipartBody(subjectEmail, (Multipart) messageContent) :
                        messageContent instanceof BASE64DecoderStream ? getMultipartBody64(subjectEmail, (BASE64DecoderStream) messageContent, message.getFileName()) :
                                messageContent instanceof String ? new MultipartBody((String) messageContent, null) : null;
                if (messageEmail == null) {
                    messageEmail = new MultipartBody(messageContent == null ? null : String.valueOf(messageContent), null);
                    ServerLoggers.mailLogger.error("Warning: missing attachment '" + messageContent + "' from email '" + subjectEmail + "'");
                }
                byte[] emlFileEmail = BaseUtils.mergeFileAndExtension(getEMLByteArray(message), "eml".getBytes());
                dataEmails.add(Arrays.asList((Object) idEmail, dateTimeSentEmail, dateTimeReceivedEmail,
                        fromAddressEmail, nameAccount, subjectEmail, messageEmail.message, emlFileEmail));
                int counter = 1;
                if (messageEmail.attachments != null) {
                    for (Map.Entry<String, byte[]> entry : messageEmail.attachments.entrySet()) {
                        dataAttachments.add(Arrays.asList((Object) idEmail, String.valueOf(counter), entry.getKey(), entry.getValue()));
                        counter++;
                    }
                }
            }
        }

        emailFolder.close(true);
        emailStore.close();

        return Arrays.asList(dataEmails, dataAttachments);
    }

    private Object getEmailContent(Message email) throws IOException, MessagingException {
        Object content;
        try {
            content = email.getContent();
        } catch (MessagingException | IOException | NullPointerException e) {
            // did this due to a bug
            try {
                content = new MimeMessage((MimeMessage) email).getContent();
            } catch (IOException e1) {
                if("Unknown encoding: utf-8".equalsIgnoreCase(e1.getMessage()))
                    content = null;
                else
                    throw e;
            }
        }
        return content;
    }

    private Timestamp getSentDate(Message message) throws MessagingException {
        Date sentDate = message.getSentDate();
        return sentDate == null ? null : new Timestamp(sentDate.getTime());
    }

    private byte[] getEMLByteArray (Message msg) throws IOException, MessagingException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        msg.writeTo(out); //вообще, out сначала необходимо MimeUtility.encode, а при открытии - decode, чтобы всё сохранялось корректно
        return out.toByteArray();
    }

    private MultipartBody getMultipartBody(String subjectEmail, Multipart mp) throws IOException, MessagingException {
        String body = "";
        Map<String, byte[]> attachments = new HashMap<>();
        for (int i = 0; i < mp.getCount(); i++) {
            BodyPart bp = mp.getBodyPart(i);
            String disp = bp.getDisposition();
            if (disp != null && (disp.equalsIgnoreCase(BodyPart.ATTACHMENT))) {
                String fileName = MimeUtility.decodeText(bp.getFileName());
                String[] fileNameAndExt = fileName.split("\\.");
                String fileExtension = fileNameAndExt.length > 1 ? fileNameAndExt[fileNameAndExt.length - 1] : "";
                
                InputStream is = bp.getInputStream();
                File f = File.createTempFile("attachment", "");
                try {
                    FileOutputStream fos = new FileOutputStream(f);
                    byte[] buf = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = is.read(buf)) != -1) {
                        fos.write(buf, 0, bytesRead);
                    }
                    fos.close();
                } catch (IOException ioe) {
                    ServerLoggers.mailLogger.error("Error reading attachment '" + fileName + "' from email '" + subjectEmail + "'");
                    throw ioe;
                }
                
                attachments.put(fileName, BaseUtils.mergeFileAndExtension(IOUtils.getFileBytes(f), fileExtension.getBytes()));
                if(!f.delete())
                    f.deleteOnExit();
            } else {
                Object content = bp.getContent();
                body = content instanceof MimeMultipart ? getMultipartBody(subjectEmail, (Multipart) content).message : String.valueOf(content);
            }
        }
        return new MultipartBody(body, attachments);
    }

    private MultipartBody getMultipartBody64(String subjectEmail, BASE64DecoderStream base64InputStream, String filename) throws IOException, MessagingException {
        byte[] byteArray = IOUtils.readBytesFromStream(base64InputStream);
        Map<String, byte[]> attachments = new HashMap<>();
        String[] fileNameAndExt = filename.split("\\.");
        String fileExtension = fileNameAndExt.length > 1 ? fileNameAndExt[fileNameAndExt.length - 1] : "";
        attachments.put(filename, BaseUtils.mergeFileAndExtension(byteArray, fileExtension.getBytes()));
        return new MultipartBody(subjectEmail, attachments);
    }

    private class MultipartBody {
        String message;
        Map<String, byte[]> attachments;

        private MultipartBody(String message, Map<String, byte[]> attachments) {
            this.message = message;
            this.attachments = attachments;
        }
    }
}


