package lsfusion.server.physics.dev.integration.external.to.mail;


import com.github.junrar.Archive;
import com.github.junrar.exception.RarException;
import com.github.junrar.impl.FileVolumeManager;
import com.github.junrar.rarfile.FileHeader;
import com.google.common.base.Throwables;
import com.sun.mail.imap.IMAPBodyPart;
import com.sun.mail.imap.IMAPInputStream;
import com.sun.mail.pop3.POP3Folder;
import com.sun.mail.util.FolderClosedIOException;
import com.sun.mail.util.MailSSLSocketFactory;
import lsfusion.base.BaseUtils;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.base.file.FileData;
import lsfusion.base.file.IOUtils;
import lsfusion.base.file.RawFileData;
import lsfusion.interop.action.MessageClientAction;
import lsfusion.server.data.expr.key.KeyExpr;
import lsfusion.server.data.query.build.QueryBuilder;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.value.DataObject;
import lsfusion.server.data.value.NullValue;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.language.ScriptingErrorLog;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.data.time.DateTimeClass;
import lsfusion.server.logics.classes.user.ConcreteCustomClass;
import lsfusion.server.physics.admin.log.ServerLoggers;
import lsfusion.server.physics.dev.integration.service.*;
import org.apache.http.entity.ContentType;
import org.apache.poi.hmef.Attachment;
import org.apache.poi.hmef.HMEFMessage;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.internet.MimeUtility;
import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.security.GeneralSecurityException;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static lsfusion.server.base.controller.thread.ThreadLocalContext.localize;
import static lsfusion.server.logics.classes.data.time.DateTimeConverter.*;
import static lsfusion.server.physics.dev.integration.external.to.mail.AccountType.*;
import static org.apache.commons.io.FileUtils.copyInputStreamToFile;

public class EmailReceiver {

    public static void receiveEmail(ExecutionContext context, EmailLogicsModule LM, DataObject accountObject, String receiveHost, Integer receivePort,
                                    String user, String password, AccountType accountType, boolean startTLS, boolean deleteMessages, Integer lastDays,
                                    Integer maxMessages) throws MessagingException, IOException, SQLException, ScriptingErrorLog.SemanticErrorException, SQLHandledException, GeneralSecurityException {

        boolean unpack = LM.findProperty("unpack[Account]").read(context, accountObject) != null;
        boolean ignoreExceptions = LM.findProperty("ignoreExceptions[Account]").read(context, accountObject) != null;
        Timestamp minDateTime = lastDays != null ? localDateTimeToSqlTimestamp(LocalDateTime.now().minusDays(lastDays)) : null;

        List<List<List<Object>>> data = downloadEmailList(context, receiveHost, receivePort, user, password, accountType, startTLS,
                deleteMessages, lastDays, maxMessages, getSkipEmails(context, LM, accountObject, user, minDateTime), unpack, ignoreExceptions);

        importEmails(context, LM, accountObject, data.get(0));
        importAttachments(context, LM, data.get(1));

        LM.findAction("formRefresh[]").execute(context);
    }

    public static Store getEmailStore(String receiveHost, AccountType accountType, boolean startTLS) throws GeneralSecurityException, NoSuchProviderException {
        Properties mailProps = new Properties();
        mailProps.setProperty(accountType == POP3 ? "mail.pop3.host" : "mail.imap.host", receiveHost);

        boolean imap = accountType == IMAP;
        boolean imaps = accountType == IMAPS;
        if (imap || imaps) { //imaps
            MailSSLSocketFactory socketFactory = new MailSSLSocketFactory();
            socketFactory.setTrustAllHosts(true);
            mailProps.put(imap ? "mail.imap.ssl.socketFactory" : "mail.imaps.ssl.socketFactory", socketFactory);
            mailProps.setProperty("mail.store.protocol", accountType.getProtocol());
            mailProps.setProperty(imap ? "mail.imap.timeout" : "mail.imaps.timeout", "5000");
            if(startTLS) {
                mailProps.setProperty("mail.imap.starttls.enable", "true");
            }
            if(imaps) {
                //options to increase downloading big attachments
                mailProps.put("mail.imaps.partialfetch", "true");
                mailProps.put("mail.imaps.fetchsize", "819200");
            }
        }

        return Session.getInstance(mailProps).getStore(accountType.getProtocol());
    }

    private static Set<String> getSkipEmails(ExecutionContext context, EmailLogicsModule LM, DataObject accountObject, String user, Timestamp minDateTime) {
        Set<String> skipEmails = new HashSet<>();
        try {
            KeyExpr emailExpr = new KeyExpr("email");
            ImRevMap<Object, KeyExpr> emailKeys = MapFact.singletonRev("email", emailExpr);

            LocalDateTime minDate = sqlTimestampToLocalDateTime(minDateTime);
            ObjectValue minDateObject = minDate != null ?  new DataObject(minDate, DateTimeClass.instance) : NullValue.instance;

            QueryBuilder<Object, Object> emailQuery = new QueryBuilder<>(emailKeys);
            emailQuery.addProperty("fromAddressEmail", LM.findProperty("fromAddress[Email]").getExpr(emailExpr));
            emailQuery.addProperty("dateTimeSentEmail", LM.findProperty("dateTimeSent[Email]").getExpr(emailExpr));
            emailQuery.addProperty("subjectEmail", LM.findProperty("subject[Email]").getExpr(emailExpr));
            emailQuery.and(LM.findProperty("skipFilter[Email,Account,DATETIME]").getExpr(emailExpr, accountObject.getExpr(), minDateObject.getExpr()).getWhere());

            ImOrderMap<ImMap<Object, Object>, ImMap<Object, Object>> emailResult = emailQuery.execute(context);
            ServerLoggers.mailLogger.info("reading skip emails:");
            for(ImMap<Object, Object> entry : emailResult.values()) {
                String emailId = getEmailId(localDateTimeToSqlTimestamp((LocalDateTime) entry.get("dateTimeSentEmail")), (String) entry.get("fromAddressEmail"),
                        (String) entry.get("subjectEmail"), null);
                ServerLoggers.mailLogger.info(emailId);
                skipEmails.add(emailId);
            }

        } catch (Exception e) {
            ServerLoggers.mailLogger.error(String.format("Account %s: read emails from base failed", user), e);
        }
        return skipEmails;
    }

    private static void importEmails(ExecutionContext context, EmailLogicsModule LM, DataObject accountObject, List<List<Object>> data) throws ScriptingErrorLog.SemanticErrorException, SQLException, SQLHandledException {

        List<ImportProperty<?>> props = new ArrayList<>();
        List<ImportField> fields = new ArrayList<>();
        List<ImportKey<?>> keys = new ArrayList<>();

        ImportField idEmailField = new ImportField(LM.findProperty("id[Email]"));
        ImportKey<?> emailKey = new ImportKey((ConcreteCustomClass) LM.findClass("Email"),
                LM.findProperty("emailId[STRING[200]]").getMapping(idEmailField));
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

        try (ExecutionContext.NewSession newContext = context.newSession()) {
            IntegrationService service = new IntegrationService(newContext, table, keys, props);
            service.synchronize(true, false);
            newContext.apply();
        }
    }

    private static void importAttachments(ExecutionContext context, EmailLogicsModule LM, List<List<Object>> data) throws ScriptingErrorLog.SemanticErrorException, SQLException, SQLHandledException {

        List<ImportProperty<?>> props = new ArrayList<>();
        List<ImportField> fields = new ArrayList<>();
        List<ImportKey<?>> keys = new ArrayList<>();

        ImportField idEmailField = new ImportField(LM.findProperty("id[Email]"));
        ImportKey<?> emailKey = new ImportKey((ConcreteCustomClass) LM.findClass("Email"),
                LM.findProperty("emailId[STRING[100]]").getMapping(idEmailField));
        emailKey.skipKey = true;
        keys.add(emailKey);
        fields.add(idEmailField);

        ImportField idAttachmentEmailField = new ImportField(LM.findProperty("id[AttachmentEmail]"));
        ImportKey<?> attachmentEmailKey = new ImportKey((ConcreteCustomClass) LM.findClass("AttachmentEmail"),
                LM.findProperty("attachmentEmail[STRING[100],STRING[100]]").getMapping(idAttachmentEmailField, idEmailField));
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

        try (ExecutionContext.NewSession newContext = context.newSession()) {
            IntegrationService service = new IntegrationService(newContext, table, keys, props);
            service.synchronize(true, false);
            newContext.apply();
        }
    }

    private static List<List<List<Object>>> downloadEmailList(ExecutionContext context, String receiveHost, Integer receivePort, String user, String password,
                                                              AccountType accountType, boolean startTLS, boolean deleteMessages, Integer lastDays, Integer maxMessages,
                                                              Set<String> skipEmails, boolean unpack, boolean ignoreExceptions) throws MessagingException, IOException, GeneralSecurityException {

        List<List<Object>> dataEmails = new ArrayList<>();
        List<List<Object>> dataAttachments = new ArrayList<>();
        System.setProperty("mail.mime.base64.ignoreerrors", "true"); //ignore errors decoding base64

        Store emailStore = getEmailStore(receiveHost, accountType, startTLS);
        if (receivePort != null)
            emailStore.connect(receiveHost, receivePort, user, password);
        else
            emailStore.connect(receiveHost, user, password);

        List<Folder> folders = getSubFolders(emailStore.getFolder("INBOX"));

        for (Folder emailFolder : folders) {

            emailFolder.open(Folder.READ_WRITE);

            LocalDateTime dateTimeReceivedEmail = LocalDateTime.now();
            Timestamp minDateTime = null;
            if (lastDays != null) {
                minDateTime = localDateTimeToSqlTimestamp(LocalDateTime.now().minusDays(lastDays));
            }

            int count = 0;
            int messageCount = emailFolder.getMessageCount();
            ServerLoggers.mailLogger.info(String.format("Account %s, folder %s: found %s emails", user, emailFolder.getFullName(), messageCount));
            Set<String> usedEmails = new HashSet<>();
            while(count < messageCount && (maxMessages == null ||  count < maxMessages)) {
                try {
                    ServerLoggers.mailLogger.info(String.format("Reading email %s of %s (max %s)", count + 1, messageCount, maxMessages));
                    Message message = emailFolder.getMessage(messageCount - count);
                    Timestamp dateTimeSentEmail = getSentDate(message);
                    ServerLoggers.mailLogger.info("sentDate: " + dateTimeSentEmail);
                    if (minDateTime == null || dateTimeSentEmail == null || minDateTime.compareTo(dateTimeSentEmail) <= 0) {
                        Address[] fromAddresses = message.getFrom();
                        String fromAddressEmail = fromAddresses.length > 0 ? ((InternetAddress) fromAddresses[0]).getAddress() : null;
                        String subjectEmail = message.getSubject();
                        String idEmail = getEmailId(dateTimeSentEmail, fromAddressEmail, subjectEmail, usedEmails);
                        ServerLoggers.mailLogger.info("idEmail: " + idEmail);
                        usedEmails.add(idEmail);
                        if (!skipEmails.contains(idEmail)) {
                            message.setFlag(deleteMessages ? Flags.Flag.DELETED : Flags.Flag.SEEN, true);
                            Object messageContent = getEmailContent(message);
                            MultipartBody messageEmail = getEmailMessage(subjectEmail, message, messageContent, unpack);
                            if (messageEmail == null) {
                                messageEmail = new MultipartBody(messageContent == null ? null : String.valueOf(messageContent), null);
                                ServerLoggers.mailLogger.error("Warning: missing attachment '" + messageContent + "' from email '" + subjectEmail + "'");
                            }
                            FileData emlFileEmail = new FileData(getEMLByteArray(message), "eml");
                            dataEmails.add(Arrays.asList(idEmail, sqlTimestampToLocalDateTime(dateTimeSentEmail), dateTimeReceivedEmail, fromAddressEmail, user, subjectEmail, messageEmail.message, emlFileEmail));
                            int counter = 1;
                            if (messageEmail.attachments != null) {
                                for (Map.Entry<String, FileData> entry : messageEmail.attachments.entrySet()) {
                                    dataAttachments.add(Arrays.asList(idEmail, String.valueOf(counter), BaseUtils.getFileName(entry.getKey()), entry.getValue()));
                                    counter++;
                                }
                            }
                        }
                    }
                    count++;
                } catch (FolderClosedIOException e) {
                    ServerLoggers.mailLogger.error("Ignored exception :", e);
                    emailFolder.open(Folder.READ_WRITE);
                } catch (Exception e) {
                    if(ignoreExceptions) {
                        ServerLoggers.mailLogger.error("Ignored exception :", e);
                        context.delayUserInterfaction(new MessageClientAction(e.toString(), localize("{mail.receiving}")));
                        count++;
                    } else throw e;
                }
            }

            emailFolder.close(true);
        }
        emailStore.close();

        return Arrays.asList(dataEmails, dataAttachments);
    }

    private static List<Folder> getSubFolders(Folder folder) throws MessagingException {
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

    public static MultipartBody getEmailMessage(String subjectEmail, Message message, Object messageContent, boolean unpack) throws MessagingException, IOException {
        if(messageContent instanceof Multipart) {
            ServerLoggers.mailLogger.info("messageContent is Multipart");
            return getMultipartBody(subjectEmail, (Multipart) messageContent, unpack, "");
        } else if(messageContent instanceof FilterInputStream) {
            ServerLoggers.mailLogger.info("messageContent is FilterInputStream");
            return getMultipartBodyStream(subjectEmail, (FilterInputStream) messageContent, decodeFileName(message.getFileName()), unpack);
        } else if(messageContent instanceof String) {
            ServerLoggers.mailLogger.info("messageContent is String");
            return new MultipartBody(((String) messageContent).replace("\0", ""), null);
        } else {
            return null;
        }
    }

    public static Object getEmailContent(Message email) throws IOException, MessagingException {
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

    private static Timestamp getSentDate(Message message) {
        return (Timestamp) BaseUtils.executeWithTimeout(() -> {
            Date sentDate = message.getSentDate();
            return sentDate == null ? null : new Timestamp(sentDate.getTime());
        }, 60000);
    }

    private static RawFileData getEMLByteArray(Message msg) throws IOException, MessagingException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        msg.writeTo(out); //вообще, out сначала необходимо MimeUtility.encode, а при открытии - decode, чтобы всё сохранялось корректно
        return new RawFileData(out);
    }

    private static String getEmailId(Timestamp dateTime, String fromAddress, String subject, Set<String> usedEmails) {
        String id = String.format("%s/%s/%s", dateTime == null ? "" : dateTime.getTime(), fromAddress, subject == null ? "" : subject);
        if(usedEmails != null) {
            int i = 0;
            while(usedEmails.contains(id + (i == 0 ? "" : ("/" + i)))) {
                i++;
            }
            if(i > 0) {
                id += "/" + i;
            }
        }
        return id;
    }

    private static MultipartBody getMultipartBody(String subjectEmail, Multipart mp, boolean unpack, String prefix) throws IOException, MessagingException {
        String body = "";
        Map<String, FileData> attachments = new HashMap<>();
        int parts = mp.getCount();
        for (int i = 0; i < parts; i++) {
            BodyPart bp = mp.getBodyPart(i);
            String disp = bp.getDisposition();
            ServerLoggers.mailLogger.info(String.format("%sreading attachment %s of %s", prefix, i + 1, parts));
            if (disp != null && (disp.equalsIgnoreCase(BodyPart.ATTACHMENT))) {
                String fileName = decodeFileName(bp.getFileName());
                ServerLoggers.mailLogger.info(String.format("%sattachment name: %s, size: %s", prefix, fileName, bp.getSize()));

                InputStream is = bp.getInputStream();
                File f = File.createTempFile("attachment", "");
                try {
                    copyInputStreamToFile(bp.getInputStream(), f);
                    if(bp.getContentType() != null && bp.getContentType().contains("application/ms-tnef")) {
                        attachments.putAll(extractWinMail(f).attachments);
                    } else {
                        attachments.putAll(unpack(new RawFileData(f), fileName, unpack));
                    }

                } catch (IOException ioe) {
                    ServerLoggers.mailLogger.error(prefix + "Error reading attachment '" + fileName + "' from email '" + subjectEmail + "'");
                    throw ioe;
                } finally {
                    if(!f.delete())
                        f.deleteOnExit();
                }
            } else {
                try {
                    Object content = getBodyPartContent(bp);
                    if (content instanceof FilterInputStream) {
                        RawFileData byteArray = new RawFileData((FilterInputStream) content);
                        String fileName = decodeFileName(bp.getFileName());
                        ServerLoggers.mailLogger.info(prefix + "attachment is FilterInputStream: " + fileName);
                        attachments.putAll(unpack(byteArray, fileName, unpack));
                    } else if (content instanceof MimeMultipart) {
                        ServerLoggers.mailLogger.info(prefix + "attachment is MimeMultipart");
                        body = getMultipartBody(subjectEmail, (Multipart) content, unpack, prefix + "---").message;
                    } else if(content instanceof IMAPInputStream){
                        body = parseIMAPInputStream(bp, (IMAPInputStream) content);
                        ServerLoggers.mailLogger.info(prefix + "attachment is IMAPInputStream, length: " + (body != null ? body.length() : "0"));
                    } else {
                        body = String.valueOf(content);
                        ServerLoggers.mailLogger.info(prefix + "attachment is String, length: " + (body != null ? body.length() : "0"));
                    }
                } catch (IOException e) {
                    throw new RuntimeException("Email subject: " + subjectEmail, e);
                }
            }
        }
        return new MultipartBody(body, attachments);
    }

    private static Object getBodyPartContent(BodyPart bp) throws MessagingException, IOException {
        Object content = null;
        if(bp instanceof IMAPBodyPart) {
            String encoding = ((IMAPBodyPart) bp).getEncoding();
            if (encoding != null) {
                Object plainContent = null;
                try {
                    plainContent = bp.getContent();
                } catch (Exception ignored) {
                }
                content = plainContent instanceof String ? plainContent : MimeUtility.decode(bp.getInputStream(), encoding);
            }
        }
        return content != null ? content : bp.getContent();
    }

    private static MultipartBody extractWinMail(File winMailFile) throws IOException {
        HMEFMessage msg = new HMEFMessage(Files.newInputStream(winMailFile.toPath()));
        Map<String, FileData> attachments = new HashMap<>();
        for(Attachment attach : msg.getAttachments()) {
            String attachName = attach.getFilename();
            attachments.put(attachName, new FileData(new RawFileData(attach.getContents()), BaseUtils.getFileExtension(attachName)));
        }
        return new MultipartBody(msg.getBody(), attachments);
    }

    private static MultipartBody getMultipartBodyStream(String subjectEmail, FilterInputStream filterInputStream, String fileName, boolean unpack) throws IOException {
        RawFileData byteArray = new RawFileData(filterInputStream);
        Map<String, FileData> attachments = new HashMap<>(unpack(byteArray, fileName, unpack));
        return new MultipartBody(subjectEmail, attachments);
    }

    private static String decodeFileName(String value) throws UnsupportedEncodingException {
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

    private static String parseIMAPInputStream(BodyPart bp, IMAPInputStream content) throws IOException, MessagingException {
        String contentType = bp.getContentType();
        if (contentType != null) {
            ContentType ct = null;
            try {
                ct = ContentType.parse(contentType);
            } catch (Exception ignored) {
            }
            if (ct != null) {
                String mimeType = ct.getMimeType();
                if (mimeType.equals("text/plain")) {
                    byte[] bytes = IOUtils.readBytesFromStream(content);
                    return new String(bytes, ct.getCharset());
                }
            }
        }
        return null;
    }

    public static class MultipartBody {
        String message;
        Map<String, FileData> attachments;

        public MultipartBody(String message, Map<String, FileData> attachments) {
            this.message = message;
            this.attachments = attachments;
        }
    }

    private static Map<String, FileData> unpack(RawFileData byteArray, String fileName, boolean unpack) {
        Map<String, FileData> attachments = new HashMap<>();
        String[] fileNameAndExt = fileName.split("\\.");
        String fileExtension = fileNameAndExt.length > 1 ? fileNameAndExt[fileNameAndExt.length - 1].trim() : "";
        if (unpack) {
            if (fileExtension.equalsIgnoreCase("rar")) {
                attachments.putAll(unpackRARFile(byteArray));
            } else if (fileExtension.equalsIgnoreCase("zip")) {
                attachments.putAll(unpackZIPFile(byteArray));
            }
        }
        if (attachments.isEmpty()) attachments.put(fileName, new FileData(byteArray, fileExtension));
        return attachments;
    }

    private static Map<String, FileData> unpackRARFile(RawFileData rawFile) {

        Map<String, FileData> result = new HashMap<>();
        File inputFile = null;
        File outputFile = null;
        try {
            inputFile = File.createTempFile("email", ".rar");
            rawFile.write(inputFile);

            List<File> dirList = new ArrayList<>();
            File outputDirectory = new File(inputFile.getParent() + "/" + BaseUtils.getFileName(inputFile));
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
                        result.put(getFileName(result, fileName), new FileData(new RawFileData(outputFile), BaseUtils.getFileExtension(outputFile)));
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

    private static Map<String, FileData> unpackZIPFile(RawFileData rawFile) {

        Map<String, FileData> result = new HashMap<>();
        File inputFile = null;
        File outputFile = null;
        try {
            inputFile = File.createTempFile("email", ".zip");
            try (FileOutputStream stream = new FileOutputStream(inputFile)) {
                rawFile.write(stream);
            }

            byte[] buffer = new byte[1024];
            Set<File> dirList = new HashSet<>();
            File outputDirectory = new File(inputFile.getParent() + "/" + BaseUtils.getFileName(inputFile));
            if(inputFile.exists() && (outputDirectory.exists() || outputDirectory.mkdir())) {
                dirList.add(outputDirectory);
                ZipInputStream inputStream = new ZipInputStream(Files.newInputStream(inputFile.toPath()), Charset.forName("cp866"));

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
                        File parentDir = outputFile.getParentFile();
                        if(!parentDir.exists()) {
                            if (parentDir.mkdirs()) {
                                dirList.add(parentDir);
                            } else {
                                throw new RuntimeException("Unable to unpack archive" + inputFile.getName());
                            }
                        }
                        FileOutputStream outputStream = new FileOutputStream(outputFile);
                        int len;
                        while ((len = inputStream.read(buffer)) > 0) {
                            outputStream.write(buffer, 0, len);
                        }
                        outputStream.close();
                        result.put(getFileName(result, fileName), new FileData(new RawFileData(outputFile), BaseUtils.getFileExtension(outputFile)));
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

    private static String getFileName(Map<String, FileData> files, String fileName) {
        if (files.containsKey(fileName)) {
            String name = BaseUtils.getFileName(fileName);
            String extension = BaseUtils.getFileExtension(fileName);
            int count = 1;
            while (files.containsKey(getFileName(name, count, extension))) {
                count++;
            }
            fileName = getFileName(name, count, extension);
        }
        return fileName;
    }

    private static String getFileName(String name, int count, String extension) {
        return name + "_" + count + (extension.isEmpty() ? "" : ("." + extension));
    }
}


