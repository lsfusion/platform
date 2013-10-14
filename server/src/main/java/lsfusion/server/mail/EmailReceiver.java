package lsfusion.server.mail;


import com.sun.mail.pop3.POP3Store;
import lsfusion.base.BaseUtils;
import lsfusion.base.IOUtils;
import lsfusion.server.ServerLoggers;
import lsfusion.server.classes.ConcreteCustomClass;
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
    boolean deleteMessagesAccount;

    public EmailReceiver(EmailLogicsModule emailLM, DataObject accountObject, String pop3HostAccount,
                         String nameAccount, String passwordAccount, boolean deleteMessagesAccount) {
        mailProps.setProperty("mail.pop3.host", pop3HostAccount);
        this.LM = emailLM;
        this.accountObject = accountObject;
        this.nameAccount = nameAccount;
        this.passwordAccount = passwordAccount;
        this.deleteMessagesAccount = deleteMessagesAccount;
    }

    public void receiveEmail(ExecutionContext context) throws MessagingException, IOException, SQLException, ScriptingErrorLog.SemanticErrorException {

        List<List<List<Object>>> data = downloadEmailList();

        importEmails(context, data.get(0));
        importAttachments(context, data.get(1));

        LM.findLAPByCompoundName("formRefresh").execute(context);
    }

    public void importEmails(ExecutionContext context, List<List<Object>> data) throws ScriptingErrorLog.SemanticErrorException, SQLException {

        List<ImportProperty<?>> props = new ArrayList<ImportProperty<?>>();
        List<ImportField> fields = new ArrayList<ImportField>();
        List<ImportKey<?>> keys = new ArrayList<ImportKey<?>>();

        ImportField idEmailField = new ImportField(LM.findLCPByCompoundName("idEmail"));
        ImportKey<?> emailKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("Email"),
                LM.findLCPByCompoundName("emailId").getMapping(idEmailField));
        keys.add(emailKey);
        props.add(new ImportProperty(idEmailField, LM.findLCPByCompoundName("idEmail").getMapping(emailKey)));
        props.add(new ImportProperty(accountObject, LM.findLCPByCompoundName("accountEmail").getMapping(emailKey)));
        fields.add(idEmailField);

        ImportField dateTimeSentEmailField = new ImportField(LM.findLCPByCompoundName("dateTimeSentEmail"));
        props.add(new ImportProperty(dateTimeSentEmailField, LM.findLCPByCompoundName("dateTimeSentEmail").getMapping(emailKey)));
        fields.add(dateTimeSentEmailField);

        ImportField dateTimeReceivedEmailField = new ImportField(LM.findLCPByCompoundName("dateTimeReceivedEmail"));
        props.add(new ImportProperty(dateTimeReceivedEmailField, LM.findLCPByCompoundName("dateTimeReceivedEmail").getMapping(emailKey)));
        fields.add(dateTimeReceivedEmailField);

        ImportField fromAddressEmailField = new ImportField(LM.findLCPByCompoundName("fromAddressEmail"));
        props.add(new ImportProperty(fromAddressEmailField, LM.findLCPByCompoundName("fromAddressEmail").getMapping(emailKey)));
        fields.add(fromAddressEmailField);

        ImportField toAddressEmailField = new ImportField(LM.findLCPByCompoundName("toAddressEmail"));
        props.add(new ImportProperty(toAddressEmailField, LM.findLCPByCompoundName("toAddressEmail").getMapping(emailKey)));
        fields.add(toAddressEmailField);

        ImportField subjectEmailField = new ImportField(LM.findLCPByCompoundName("subjectEmail"));
        props.add(new ImportProperty(subjectEmailField, LM.findLCPByCompoundName("subjectEmail").getMapping(emailKey)));
        fields.add(subjectEmailField);

        ImportField messageEmailField = new ImportField(LM.findLCPByCompoundName("messageEmail"));
        props.add(new ImportProperty(messageEmailField, LM.findLCPByCompoundName("messageEmail").getMapping(emailKey)));
        fields.add(messageEmailField);
        
        ImportField emlFileEmailField = new ImportField(LM.findLCPByCompoundName("emlFileEmail"));
        props.add(new ImportProperty(emlFileEmailField, LM.findLCPByCompoundName("emlFileEmail").getMapping(emailKey)));
        fields.add(emlFileEmailField);
        
        ImportTable table = new ImportTable(fields, data);

        DataSession session = context.createSession();
        session.sql.pushVolatileStats(null);
        IntegrationService service = new IntegrationService(session, table, keys, props);
        service.synchronize(true, false);
        session.apply(context.getBL());
        session.sql.popVolatileStats(null);
        session.close();
    }

    public void importAttachments(ExecutionContext context, List<List<Object>> data) throws ScriptingErrorLog.SemanticErrorException, SQLException {

        List<ImportProperty<?>> props = new ArrayList<ImportProperty<?>>();
        List<ImportField> fields = new ArrayList<ImportField>();
        List<ImportKey<?>> keys = new ArrayList<ImportKey<?>>();

        ImportField idEmailField = new ImportField(LM.findLCPByCompoundName("idEmail"));
        ImportKey<?> emailKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("Email"),
                LM.findLCPByCompoundName("emailId").getMapping(idEmailField));
        emailKey.skipKey = true;
        keys.add(emailKey);
        fields.add(idEmailField);

        ImportField idAttachmentEmailField = new ImportField(LM.findLCPByCompoundName("idAttachmentEmail"));
        ImportKey<?> attachmentEmailKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("AttachmentEmail"),
                LM.findLCPByCompoundName("attachmentEmailIdEmail").getMapping(idAttachmentEmailField, idEmailField));
        keys.add(attachmentEmailKey);
        props.add(new ImportProperty(idAttachmentEmailField, LM.findLCPByCompoundName("idAttachmentEmail").getMapping(attachmentEmailKey)));
        props.add(new ImportProperty(idEmailField, LM.findLCPByCompoundName("emailAttachmentEmail").getMapping(attachmentEmailKey),
                LM.object(LM.findClassByCompoundName("Email")).getMapping(emailKey)));
        fields.add(idAttachmentEmailField);

        ImportField nameAttachmentEmailField = new ImportField(LM.findLCPByCompoundName("nameAttachmentEmail"));
        props.add(new ImportProperty(nameAttachmentEmailField, LM.findLCPByCompoundName("nameAttachmentEmail").getMapping(attachmentEmailKey)));
        fields.add(nameAttachmentEmailField);

        ImportField fileAttachmentEmailField = new ImportField(LM.findLCPByCompoundName("fileAttachmentEmail"));
        props.add(new ImportProperty(fileAttachmentEmailField, LM.findLCPByCompoundName("fileAttachmentEmail").getMapping(attachmentEmailKey)));
        fields.add(fileAttachmentEmailField);

        ImportTable table = new ImportTable(fields, data);

        DataSession session = context.createSession();
        session.sql.pushVolatileStats(null);
        IntegrationService service = new IntegrationService(session, table, keys, props);
        service.synchronize(true, false);
        session.apply(context.getBL());
        session.sql.popVolatileStats(null);
        session.close();
    }

    public List<List<List<Object>>> downloadEmailList() throws MessagingException, SQLException, IOException {

        List<List<Object>> dataEmails = new ArrayList<List<Object>>();
        List<List<Object>> dataAttachments = new ArrayList<List<Object>>();
        Session emailSession = Session.getDefaultInstance(mailProps);

        POP3Store emailStore = (POP3Store) emailSession.getStore("pop3");
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
            MultipartBody messageEmail = getMultipartBody((Multipart) message.getContent());
            byte[] emlFileEmail = BaseUtils.mergeFileAndExtension(getEMLByteArray(message), "eml".getBytes());
            dataEmails.add(Arrays.asList((Object) idEmail, dateTimeSentEmail, dateTimeReceivedEmail,
                    fromAddressEmail, nameAccount, subjectEmail, messageEmail.message, emlFileEmail));
            int counter = 1;
            for (Map.Entry<String, byte[]> entry : messageEmail.attachments.entrySet()) {
                dataAttachments.add(Arrays.asList((Object) idEmail, String.valueOf(counter), entry.getKey(), entry.getValue()));
                counter++;
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

    private MultipartBody getMultipartBody(Multipart mp) throws IOException, MessagingException {
        String body = "";
        Map<String, byte[]> attachments = new HashMap<String, byte[]>();
        for (int i = 0; i < mp.getCount(); i++) {
            BodyPart bp = mp.getBodyPart(i);
            String disp = bp.getDisposition();
            if (disp != null && (disp.equals(BodyPart.ATTACHMENT))) {
                InputStream is = bp.getInputStream();
                File f = File.createTempFile(bp.getFileName(), "");
                FileOutputStream fos = new FileOutputStream(f);
                byte[] buf = new byte[4096];
                int bytesRead;
                while ((bytesRead = is.read(buf)) != -1) {
                    fos.write(buf, 0, bytesRead);
                }
                fos.close();
                String[] fileName = bp.getFileName().split("\\.");
                String fileExtension = fileName.length > 1 ? fileName[fileName.length - 1] : "";
                attachments.put(bp.getFileName(), BaseUtils.mergeFileAndExtension(IOUtils.getFileBytes(f), fileExtension.getBytes()));
            } else {
                Object content = bp.getContent();
                body = content instanceof MimeMultipart ? getMultipartBody((Multipart) content).message : String.valueOf(content);
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


