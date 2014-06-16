package lsfusion.server.mail;


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
import org.apache.log4j.Logger;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMultipart;
import javax.mail.internet.MimeUtility;
import java.io.*;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.*;

public class EmailReceiver {
    private final static Logger logger = ServerLoggers.mailLogger;
    EmailLogicsModule LM;
    Properties mailProps = new Properties();
    DataObject accountObject;
    String nameAccount;
    String passwordAccount;
    boolean isPOP3;
    boolean deleteMessagesAccount;

    public EmailReceiver(EmailLogicsModule emailLM, DataObject accountObject, String hostAccount, 
                         String nameAccount, String passwordAccount, boolean isPOP3, boolean deleteMessagesAccount) {
            mailProps.setProperty(isPOP3 ? "mail.pop3.host" : "mail.imap.host", hostAccount);
        this.LM = emailLM;
        this.accountObject = accountObject;
        this.nameAccount = nameAccount;
        this.passwordAccount = passwordAccount;
        this.isPOP3 = isPOP3;
        this.deleteMessagesAccount = deleteMessagesAccount;
    }

    public void receiveEmail(ExecutionContext context) throws MessagingException, IOException, SQLException, ScriptingErrorLog.SemanticErrorException, SQLHandledException {

        List<List<List<Object>>> data = downloadEmailList();

        importEmails(context, data.get(0));
        importAttachments(context, data.get(1));

        LM.findLAPByCompoundOldName("formRefresh").execute(context);
    }

    public void importEmails(ExecutionContext context, List<List<Object>> data) throws ScriptingErrorLog.SemanticErrorException, SQLException, SQLHandledException {

        List<ImportProperty<?>> props = new ArrayList<ImportProperty<?>>();
        List<ImportField> fields = new ArrayList<ImportField>();
        List<ImportKey<?>> keys = new ArrayList<ImportKey<?>>();

        ImportField idEmailField = new ImportField(LM.findLCPByCompoundOldName("idEmail"));
        ImportKey<?> emailKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("Email"),
                LM.findLCPByCompoundOldName("emailId").getMapping(idEmailField));
        keys.add(emailKey);
        props.add(new ImportProperty(idEmailField, LM.findLCPByCompoundOldName("idEmail").getMapping(emailKey)));
        props.add(new ImportProperty(accountObject, LM.findLCPByCompoundOldName("accountEmail").getMapping(emailKey)));
        fields.add(idEmailField);

        ImportField dateTimeSentEmailField = new ImportField(LM.findLCPByCompoundOldName("dateTimeSentEmail"));
        props.add(new ImportProperty(dateTimeSentEmailField, LM.findLCPByCompoundOldName("dateTimeSentEmail").getMapping(emailKey), true));
        fields.add(dateTimeSentEmailField);

        ImportField dateTimeReceivedEmailField = new ImportField(LM.findLCPByCompoundOldName("dateTimeReceivedEmail"));
        props.add(new ImportProperty(dateTimeReceivedEmailField, LM.findLCPByCompoundOldName("dateTimeReceivedEmail").getMapping(emailKey), true));
        fields.add(dateTimeReceivedEmailField);

        ImportField fromAddressEmailField = new ImportField(LM.findLCPByCompoundOldName("fromAddressEmail"));
        props.add(new ImportProperty(fromAddressEmailField, LM.findLCPByCompoundOldName("fromAddressEmail").getMapping(emailKey), true));
        fields.add(fromAddressEmailField);

        ImportField toAddressEmailField = new ImportField(LM.findLCPByCompoundOldName("toAddressEmail"));
        props.add(new ImportProperty(toAddressEmailField, LM.findLCPByCompoundOldName("toAddressEmail").getMapping(emailKey), true));
        fields.add(toAddressEmailField);

        ImportField subjectEmailField = new ImportField(LM.findLCPByCompoundOldName("subjectEmail"));
        props.add(new ImportProperty(subjectEmailField, LM.findLCPByCompoundOldName("subjectEmail").getMapping(emailKey), true));
        fields.add(subjectEmailField);

        ImportField messageEmailField = new ImportField(LM.findLCPByCompoundOldName("messageEmail"));
        props.add(new ImportProperty(messageEmailField, LM.findLCPByCompoundOldName("messageEmail").getMapping(emailKey), true));
        fields.add(messageEmailField);
        
        ImportField emlFileEmailField = new ImportField(LM.findLCPByCompoundOldName("emlFileEmail"));
        props.add(new ImportProperty(emlFileEmailField, LM.findLCPByCompoundOldName("emlFileEmail").getMapping(emailKey), true));
        fields.add(emlFileEmailField);
        
        ImportTable table = new ImportTable(fields, data);

        DataSession session = context.createSession();
        session.pushVolatileStats("ER_AT");
        IntegrationService service = new IntegrationService(session, table, keys, props);
        service.synchronize(true, false);
        session.apply(context);
        session.popVolatileStats();
        session.close();
    }

    public void importAttachments(ExecutionContext context, List<List<Object>> data) throws ScriptingErrorLog.SemanticErrorException, SQLException, SQLHandledException {

        List<ImportProperty<?>> props = new ArrayList<ImportProperty<?>>();
        List<ImportField> fields = new ArrayList<ImportField>();
        List<ImportKey<?>> keys = new ArrayList<ImportKey<?>>();

        ImportField idEmailField = new ImportField(LM.findLCPByCompoundOldName("idEmail"));
        ImportKey<?> emailKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("Email"),
                LM.findLCPByCompoundOldName("emailId").getMapping(idEmailField));
        emailKey.skipKey = true;
        keys.add(emailKey);
        fields.add(idEmailField);

        ImportField idAttachmentEmailField = new ImportField(LM.findLCPByCompoundOldName("idAttachmentEmail"));
        ImportKey<?> attachmentEmailKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("AttachmentEmail"),
                LM.findLCPByCompoundOldName("attachmentEmailIdEmail").getMapping(idAttachmentEmailField, idEmailField));
        keys.add(attachmentEmailKey);
        props.add(new ImportProperty(idAttachmentEmailField, LM.findLCPByCompoundOldName("idAttachmentEmail").getMapping(attachmentEmailKey)));
        props.add(new ImportProperty(idEmailField, LM.findLCPByCompoundOldName("emailAttachmentEmail").getMapping(attachmentEmailKey),
                LM.object(LM.findClassByCompoundName("Email")).getMapping(emailKey)));
        fields.add(idAttachmentEmailField);

        ImportField nameAttachmentEmailField = new ImportField(LM.findLCPByCompoundOldName("nameAttachmentEmail"));
        props.add(new ImportProperty(nameAttachmentEmailField, LM.findLCPByCompoundOldName("nameAttachmentEmail").getMapping(attachmentEmailKey)));
        fields.add(nameAttachmentEmailField);

        ImportField fileAttachmentEmailField = new ImportField(LM.findLCPByCompoundOldName("fileAttachmentEmail"));
        props.add(new ImportProperty(fileAttachmentEmailField, LM.findLCPByCompoundOldName("fileAttachmentEmail").getMapping(attachmentEmailKey)));
        fields.add(fileAttachmentEmailField);

        ImportTable table = new ImportTable(fields, data);

        DataSession session = context.createSession();
        session.pushVolatileStats("ER_EL");
        IntegrationService service = new IntegrationService(session, table, keys, props);
        service.synchronize(true, false);
        session.apply(context);
        session.popVolatileStats();
        session.close();
    }

    public List<List<List<Object>>> downloadEmailList() throws MessagingException, SQLException, IOException {

        List<List<Object>> dataEmails = new ArrayList<List<Object>>();
        List<List<Object>> dataAttachments = new ArrayList<List<Object>>();
        Session emailSession = Session.getDefaultInstance(mailProps);

        Store emailStore = emailSession.getStore(isPOP3 ? "pop3" : "imap");
        emailStore.connect(nameAccount, passwordAccount);

        Folder emailFolder = emailStore.getFolder("INBOX");
        emailFolder.open(Folder.READ_WRITE);

        Timestamp dateTimeReceivedEmail = new Timestamp(Calendar.getInstance().getTime().getTime());

        Message[] messages = emailFolder.getMessages();
        for (int i = 0; i < messages.length; i++) {
            Message message = messages[i];
            if (deleteMessagesAccount)
                message.setFlag(Flags.Flag.DELETED, true);
            Timestamp dateTimeSentEmail = new Timestamp(message.getSentDate().getTime());
            String fromAddressEmail = ((InternetAddress) message.getFrom()[0]).getAddress();
            String idEmail = String.valueOf(dateTimeSentEmail.getTime()) + fromAddressEmail;
            String subjectEmail = message.getSubject();
            MultipartBody messageEmail = message.getContent() instanceof Multipart ? getMultipartBody(subjectEmail, (Multipart) message.getContent()) : new MultipartBody((String) message.getContent(), null);
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

        emailFolder.close(true);
        emailStore.close();

        return Arrays.asList(dataEmails, dataAttachments);
    }

    private byte[] getEMLByteArray (Message msg) throws IOException, MessagingException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        msg.writeTo(out); //вообще, out сначала необходимо MimeUtility.encode, а при открытии - decode, чтобы всё сохранялось корректно
        return out.toByteArray();
    }

    private MultipartBody getMultipartBody(String subjectEmail, Multipart mp) throws IOException, MessagingException {
        String body = "";
        Map<String, byte[]> attachments = new HashMap<String, byte[]>();
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
                    ServerLoggers.systemLogger.error("Error reading attachment '" + fileName + "' from email '" + subjectEmail + "'");
                    throw ioe;
                }
                
                attachments.put(fileName, BaseUtils.mergeFileAndExtension(IOUtils.getFileBytes(f), fileExtension.getBytes()));
                f.delete();
            } else {
                Object content = bp.getContent();
                body = content instanceof MimeMultipart ? getMultipartBody(subjectEmail, (Multipart) content).message : String.valueOf(content);
            }
        }
        return new MultipartBody(body, attachments);
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


