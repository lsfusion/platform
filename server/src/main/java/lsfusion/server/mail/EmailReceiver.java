package lsfusion.server.mail;


import com.github.junrar.Archive;
import com.github.junrar.exception.RarException;
import com.github.junrar.impl.FileVolumeManager;
import com.github.junrar.rarfile.FileHeader;
import com.google.common.base.Throwables;
import com.sun.mail.pop3.POP3Folder;
import com.sun.mail.util.BASE64DecoderStream;
import com.sun.mail.util.MailSSLSocketFactory;
import lsfusion.base.BaseUtils;
import lsfusion.base.IOUtils;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.server.ServerLoggers;
import lsfusion.server.classes.ConcreteCustomClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.expr.KeyExpr;
import lsfusion.server.data.query.QueryBuilder;
import lsfusion.server.integration.*;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.EmailLogicsModule;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.scripted.ScriptingModuleErrorLog;
import lsfusion.server.session.DataSession;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.internet.MimeUtility;
import java.io.*;
import java.nio.charset.Charset;
import java.security.GeneralSecurityException;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

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

    public void receiveEmail(ExecutionContext context) throws MessagingException, IOException, SQLException, ScriptingModuleErrorLog.SemanticError, SQLHandledException, GeneralSecurityException {

        boolean unpack = LM.findProperty("unpack[Account]").read(context, accountObject) != null;
        List<List<List<Object>>> data = downloadEmailList(getSkipEmails(context), unpack);

        importEmails(context, data.get(0));
        importAttachments(context, data.get(1));

        LM.findAction("formRefresh[]").execute(context);
    }

    private Set<String> getSkipEmails(ExecutionContext context) {
        Set<String> skipEmails = new HashSet<>();
        try {
            KeyExpr emailExpr = new KeyExpr("email");
            ImRevMap<Object, KeyExpr> emailKeys = MapFact.singletonRev((Object) "email", emailExpr);

            QueryBuilder<Object, Object> emailQuery = new QueryBuilder<>(emailKeys);
            emailQuery.addProperty("fromAddressEmail", LM.findProperty("fromAddress[Email]").getExpr(emailExpr));
            emailQuery.addProperty("dateTimeSentEmail", LM.findProperty("dateTimeSent[Email]").getExpr(emailExpr));
            emailQuery.addProperty("subjectEmail", LM.findProperty("subject[Email]").getExpr(emailExpr));
            emailQuery.and(LM.findProperty("fromAddress[Email]").getExpr(emailExpr).getWhere());

            ImOrderMap<ImMap<Object, Object>, ImMap<Object, Object>> emailResult = emailQuery.execute(context);
            for(ImMap<Object, Object> entry : emailResult.values()) {
                skipEmails.add(getEmailId((Timestamp) entry.get("dateTimeSentEmail"), (String) entry.get("fromAddressEmail"),
                        (String) entry.get("subjectEmail")));
            }

        } catch (Exception e) {
            ServerLoggers.mailLogger.error(String.format("Account %s: read emails from base failed", nameAccount), e);
        }
        return skipEmails;
    }

    public void importEmails(ExecutionContext context, List<List<Object>> data) throws ScriptingModuleErrorLog.SemanticError, SQLException, SQLHandledException {

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

    public void importAttachments(ExecutionContext context, List<List<Object>> data) throws ScriptingModuleErrorLog.SemanticError, SQLException, SQLHandledException {

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

    public List<List<List<Object>>> downloadEmailList(Set<String> skipEmails, boolean unpack) throws MessagingException, SQLException, IOException, GeneralSecurityException {

        List<List<Object>> dataEmails = new ArrayList<>();
        List<List<Object>> dataAttachments = new ArrayList<>();
        //options to increase downloading big attachments
        mailProps.put("mail.imaps.partialfetch", "true");
        mailProps.put("mail.imaps.fetchsize", "819200");
        if (!isPOP3) { //imaps
            MailSSLSocketFactory socketFactory = new MailSSLSocketFactory();
            socketFactory.setTrustAllHosts(true);
            mailProps.put("mail.imaps.ssl.socketFactory", socketFactory);
            mailProps.setProperty("mail.store.protocol", "imaps");
        }
        Session emailSession = Session.getInstance(mailProps);
        Store emailStore = emailSession.getStore(isPOP3 ? "pop3" : "imaps");
        if (receivePortAccount != null)
            emailStore.connect(receiveHostAccount, receivePortAccount, nameAccount, passwordAccount);
        else
            emailStore.connect(receiveHostAccount, nameAccount, passwordAccount);

        List<Folder> folders = getSubFolders(emailStore.getFolder("INBOX"));

        for (Folder emailFolder : folders) {

            emailFolder.open(Folder.READ_WRITE);

            Timestamp dateTimeReceivedEmail = new Timestamp(Calendar.getInstance().getTime().getTime());
            Timestamp minDateTime = null;
            if (lastDaysAccount != null) {
                Calendar calendar = Calendar.getInstance();
                calendar.add(Calendar.DATE, -lastDaysAccount);
                minDateTime = new Timestamp(calendar.getTime().getTime());
            }

            Message[] messages = emailFolder.getMessages();
            ServerLoggers.mailLogger.info(String.format("Account %s, folder %s: found %s emails", nameAccount, emailFolder.getFullName(), messages.length));
            for (Message message : messages) {
                Timestamp dateTimeSentEmail = getSentDate(message);
                if (minDateTime == null || dateTimeSentEmail == null || minDateTime.compareTo(dateTimeSentEmail) <= 0) {
                    String fromAddressEmail = ((InternetAddress) message.getFrom()[0]).getAddress();
                    String subjectEmail = message.getSubject();
                    String idEmail = getEmailId(dateTimeSentEmail, fromAddressEmail, subjectEmail);
                    if (!skipEmails.contains(idEmail)) {
                        message.setFlag(deleteMessagesAccount ? Flags.Flag.DELETED : Flags.Flag.SEEN, true);
                        Object messageContent = getEmailContent(message);
                        MultipartBody messageEmail = messageContent instanceof Multipart ? getMultipartBody(subjectEmail, (Multipart) messageContent, unpack) :
                                messageContent instanceof BASE64DecoderStream ? getMultipartBody64(subjectEmail, (BASE64DecoderStream) messageContent, decodeFileName(message.getFileName()), unpack) :
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
            }

            emailFolder.close(true);
        }
        emailStore.close();

        return Arrays.asList(dataEmails, dataAttachments);
    }

    private List<Folder> getSubFolders(Folder folder) throws MessagingException {
        List<Folder> folders = new ArrayList<>();
        folders.add(folder);
        //pop3 doesn't allow subfolders
        if(!(folder instanceof POP3Folder)) {
            for (Folder f : folder.list()) {
                folders.addAll(getSubFolders(f));
            }
        }
        return folders;
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

    private String getEmailId(Timestamp dateTime, String fromAddress, String subject) {
        return String.format("%s/%s/%s", dateTime == null ? "" : dateTime.getTime(), fromAddress, subject == null ? "" : subject);
    }

    private MultipartBody getMultipartBody(String subjectEmail, Multipart mp, boolean unpack) throws IOException, MessagingException {
        String body = "";
        Map<String, byte[]> attachments = new HashMap<>();
        for (int i = 0; i < mp.getCount(); i++) {
            BodyPart bp = mp.getBodyPart(i);
            String disp = bp.getDisposition();
            if (disp != null && (disp.equalsIgnoreCase(BodyPart.ATTACHMENT))) {
                String fileName = decodeFileName(bp.getFileName());
                
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

                    attachments.putAll(unpack(IOUtils.getFileBytes(f), fileName, unpack));

                } catch (IOException ioe) {
                    ServerLoggers.mailLogger.error("Error reading attachment '" + fileName + "' from email '" + subjectEmail + "'");
                    throw ioe;
                } finally {
                    if(!f.delete())
                        f.deleteOnExit();
                }
            } else {
                Object content = bp.getContent();
                if (content instanceof BASE64DecoderStream) {
                    byte[] byteArray = IOUtils.readBytesFromStream((BASE64DecoderStream) content);
                    String fileName = decodeFileName(bp.getFileName());
                    attachments.putAll(unpack(byteArray, fileName, unpack));
                } else if (content instanceof MimeMultipart) {
                    body = getMultipartBody(subjectEmail, (Multipart) content, unpack).message;
                } else
                    body = String.valueOf(content);
            }
        }
        return new MultipartBody(body, attachments);
    }

    private MultipartBody getMultipartBody64(String subjectEmail, BASE64DecoderStream base64InputStream, String fileName, boolean unpack) throws IOException, MessagingException {
        byte[] byteArray = IOUtils.readBytesFromStream(base64InputStream);
        Map<String, byte[]> attachments = new HashMap<>();
        attachments.putAll(unpack(byteArray, fileName, unpack));
        return new MultipartBody(subjectEmail, attachments);
    }

    private String decodeFileName(String value) throws UnsupportedEncodingException {
        if (value == null)
            value = "attachment.txt";
        else {
            Pattern p = Pattern.compile("\\=\\?[^?]*\\?\\w\\?[^?]*\\?\\=");
            Matcher m = p.matcher(value);
            while (m.find()) {
                value = value.replace(m.group(), MimeUtility.decodeText(m.group()));
            }
            value = MimeUtility.decodeText(value);
        }
        return value;
    }

    private class MultipartBody {
        String message;
        Map<String, byte[]> attachments;

        private MultipartBody(String message, Map<String, byte[]> attachments) {
            this.message = message;
            this.attachments = attachments;
        }
    }

    private Map<String, byte[]> unpack(byte[] byteArray, String fileName, boolean unpack) {
        Map<String, byte[]> attachments = new HashMap<>();
        String[] fileNameAndExt = fileName.split("\\.");
        String fileExtension = fileNameAndExt.length > 1 ? fileNameAndExt[fileNameAndExt.length - 1].trim() : "";
        if (unpack) {
            if (fileExtension.toLowerCase().equals("rar")) {
                attachments.putAll(unpackRARFile(byteArray));
            } else if (fileExtension.toLowerCase().equals("zip")) {
                attachments.putAll(unpackZIPFile(byteArray));
            }
        }
        if (attachments.isEmpty())
            attachments.put(fileName, BaseUtils.mergeFileAndExtension(byteArray, fileExtension.getBytes()));
        return attachments;
    }

    private Map<String, byte[]> unpackRARFile(byte[] fileBytes) {

        Map<String, byte[]> result = new HashMap<>();
        File inputFile = null;
        File outputFile = null;
        try {
            inputFile = File.createTempFile("email", ".rar");
            try (FileOutputStream stream = new FileOutputStream(inputFile)) {
                stream.write(fileBytes);
            }

            List<File> dirList = new ArrayList<>();
            File outputDirectory = new File(inputFile.getParent() + "/" + getFileNameWithoutExt(inputFile));
            if(inputFile.exists() && (outputDirectory.exists() || outputDirectory.mkdir())) {
                dirList.add(outputDirectory);
                Archive a = new Archive(new FileVolumeManager(inputFile));

                FileHeader fh = a.nextFileHeader();

                while (fh != null) {
                    String fileName = (fh.isUnicode() ? fh.getFileNameW() : fh.getFileNameString());
                    outputFile = new File(outputDirectory.getPath() + "/" + fileName);
                    File dir = outputFile.getParentFile();
                    dir.mkdirs();
                    if(!dirList.contains(dir))
                        dirList.add(dir);
                    if(!outputFile.isDirectory()) {
                        try (FileOutputStream os = new FileOutputStream(outputFile)) {
                            a.extractFile(fh, os);
                        }
                        result.put(getFileName(result, fileName), BaseUtils.mergeFileAndExtension(IOUtils.getFileBytes(outputFile), BaseUtils.getFileExtension(outputFile).getBytes()));
                        if(!outputFile.delete())
                            outputFile.deleteOnExit();
                    }
                    fh = a.nextFileHeader();
                }
                a.close();
            }

            for(File dir : dirList)
                if(dir != null && dir.exists() && !dir.delete())
                    dir.deleteOnExit();

        } catch (RarException | IOException e) {
            throw Throwables.propagate(e);
        } finally {
            if(inputFile != null && !inputFile.delete())
                inputFile.deleteOnExit();
            if(outputFile != null && !outputFile.delete())
                outputFile.deleteOnExit();
        }
        return result;
    }

    private Map<String, byte[]> unpackZIPFile(byte[] fileBytes) {

        Map<String, byte[]> result = new HashMap<>();
        File inputFile = null;
        File outputFile = null;
        try {
            inputFile = File.createTempFile("email", ".zip");
            try (FileOutputStream stream = new FileOutputStream(inputFile)) {
                stream.write(fileBytes);
            }

            byte[] buffer = new byte[1024];
            Set<File> dirList = new HashSet<>();
            File outputDirectory = new File(inputFile.getParent() + "/" + getFileNameWithoutExt(inputFile));
            if(inputFile.exists() && (outputDirectory.exists() || outputDirectory.mkdir())) {
                dirList.add(outputDirectory);
                ZipInputStream inputStream = new ZipInputStream(new FileInputStream(inputFile), Charset.forName("cp866"));

                ZipEntry ze = inputStream.getNextEntry();
                while (ze != null) {
                    if(ze.isDirectory()) {
                        File dir = new File(outputDirectory.getPath() + "/" + ze.getName());
                        dir.mkdirs();
                        dirList.add(dir);
                    }
                    else {
                        String fileName = ze.getName();
                        outputFile = new File(outputDirectory.getPath() + "/" + fileName);
                        FileOutputStream outputStream = new FileOutputStream(outputFile);
                        int len;
                        while ((len = inputStream.read(buffer)) > 0) {
                            outputStream.write(buffer, 0, len);
                        }
                        outputStream.close();
                        result.put(getFileName(result, fileName), BaseUtils.mergeFileAndExtension(IOUtils.getFileBytes(outputFile), BaseUtils.getFileExtension(outputFile).getBytes()));
                        if(!outputFile.delete())
                            outputFile.deleteOnExit();
                    }
                    ze = inputStream.getNextEntry();
                }
                inputStream.closeEntry();
                inputStream.close();
            }

            for(File dir : dirList)
                if(dir != null && dir.exists() && !dir.delete())
                    dir.deleteOnExit();

        } catch (IOException e) {
            throw Throwables.propagate(e);
        } finally {
            if(inputFile != null && !inputFile.delete())
                inputFile.deleteOnExit();
            if(outputFile != null && !outputFile.delete())
                outputFile.deleteOnExit();
        }
        return result;
    }

    private String getFileName(Map<String, byte[]> files, String fileName) {
        if (files.containsKey(fileName)) {
            String name = getFileNameWithoutExt(fileName);
            String extension = BaseUtils.getFileExtension(fileName);
            int count = 1;
            while (files.containsKey(name + "_" + count + (extension.isEmpty() ? "" : ("." + extension)))) {
                count++;
            }
            fileName = name + "_" + count + (extension.isEmpty() ? "" : ("." + extension));
        }
        return fileName;
    }

    private String getFileNameWithoutExt(File file) {
        String name = file.getName();
        int index = name.lastIndexOf(".");
        return (index == -1) ? name : name.substring(0, index);
    }

    private String getFileNameWithoutExt(String name) {
        int index = name.lastIndexOf(".");
        return (index == -1) ? name : name.substring(0, index);
    }
}


