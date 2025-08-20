package lsfusion.server.physics.dev.integration.external.to.mail;

import com.sun.mail.imap.DefaultFolder;
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
import lsfusion.interop.form.property.Compare;
import lsfusion.server.data.expr.key.KeyExpr;
import lsfusion.server.data.query.build.QueryBuilder;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.value.DataObject;
import lsfusion.server.data.value.NullValue;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.language.ScriptingErrorLog;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.data.time.DateTimeClass;
import lsfusion.server.physics.admin.Settings;
import lsfusion.server.physics.admin.log.ServerLoggers;
import lsfusion.server.physics.dev.integration.external.to.file.ZipUtils;
import lsfusion.server.physics.dev.integration.service.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.hc.core5.http.ContentType;
import org.apache.poi.hmef.Attachment;
import org.apache.poi.hmef.HMEFMessage;

import javax.mail.*;
import javax.mail.internet.*;
import java.io.*;
import java.nio.file.Files;
import java.security.GeneralSecurityException;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static lsfusion.base.BaseUtils.nullEmpty;
import static lsfusion.server.base.controller.thread.ThreadLocalContext.localize;
import static lsfusion.base.DateConverter.*;
import static lsfusion.server.physics.dev.integration.external.to.mail.AccountType.*;
import static org.apache.commons.io.FileUtils.copyInputStreamToFile;

public class EmailReceiver {

    public static void receiveEmail(ExecutionContext context, EmailLogicsModule LM, DataObject accountObject, String receiveHost, Integer receivePort,
                                    String user, String password, AccountType accountType, boolean startTLS, boolean deleteMessages, Integer lastDays,
                                    Integer maxMessages, boolean insecureSSL, boolean readAllFolders) throws MessagingException, IOException, SQLException, ScriptingErrorLog.SemanticErrorException, SQLHandledException, GeneralSecurityException {

        boolean unpack = LM.unpackAccount.read(context, accountObject) != null;
        boolean ignoreExceptions = LM.ignoreExceptionsAccount.read(context, accountObject) != null;
        LocalDateTime minDateTime = lastDays != null ? LocalDateTime.now().minusDays(lastDays) : null;

        List<List<List<Object>>> data = downloadEmailList(context, receiveHost, receivePort, user, password, accountType, startTLS, deleteMessages, maxMessages,
                getSkipEmails(context, LM, accountObject, minDateTime), minDateTime, unpack, ignoreExceptions, insecureSSL, readAllFolders);

        importFolders(context, LM, accountObject, data.get(0), data.get(1));
        importEmails(context, LM, accountObject, data.get(2));
        importAttachments(context, LM, data.get(3), accountObject);

        LM.findAction("formRefresh[]").execute(context);
    }

    public static Store getEmailStore(String receiveHost, AccountType accountType, boolean startTLS, boolean insecureSSL) throws GeneralSecurityException, NoSuchProviderException {
        Properties mailProps = new Properties();

        String protocol = accountType.getProtocol();
        boolean imaps = accountType == IMAPS;
        boolean pop3s = accountType == POP3S;

        mailProps.setProperty("mail." + protocol + ".host", receiveHost);

        if (imaps || pop3s) {
            if (insecureSSL) {
                MailSSLSocketFactory socketFactory = new MailSSLSocketFactory();
                socketFactory.setTrustAllHosts(true);
                mailProps.put("mail." + protocol + ".ssl.socketFactory", socketFactory);
            } else
                mailProps.put("mail." + protocol + ".ssl.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
        }

        mailProps.setProperty("mail.store.protocol", protocol);
        mailProps.setProperty("mail." + protocol + ".timeout", String.valueOf(Settings.get().getMailReceiveTimeout()));

        if (startTLS) {
            mailProps.setProperty("mail." + protocol + ".starttls.enable", "true");
        }
        //options to increase downloading big attachments
        mailProps.put("mail." + protocol + ".partialfetch", "true");
        mailProps.put("mail." + protocol + ".fetchsize", "819200");

        if (Settings.get().isIgnoreBodyStructureSizeFix()) {
            mailProps.put("mail." + protocol + ".ignorebodystructuresize", "true");
        }

        return Session.getInstance(mailProps).getStore(protocol);
    }

    private static Map<String, EmailData> getSkipEmails(ExecutionContext context, EmailLogicsModule LM, DataObject accountObject, LocalDateTime minDateTime) throws SQLException, SQLHandledException {
        Map<String, EmailData> skipEmails = new HashMap<>();

        ObjectValue minDateObject = minDateTime != null ? new DataObject(minDateTime, DateTimeClass.instance) : NullValue.instance;

        KeyExpr emailExpr = new KeyExpr("email");
        ImRevMap<Object, KeyExpr> emailKeys = MapFact.singletonRev("email", emailExpr);

        QueryBuilder<Object, Object> emailQuery = new QueryBuilder<>(emailKeys);
        emailQuery.addProperty("id", LM.idEmail.getExpr(emailExpr));
        emailQuery.addProperty("dateTimeSent", LM.dateTimeSentEmail.getExpr(emailExpr));
        emailQuery.addProperty("skip", LM.skipFilter.getExpr(emailExpr, accountObject.getExpr(), minDateObject.getExpr()));
        emailQuery.and(LM.accountEmail.getExpr(emailExpr).compare(accountObject.getExpr(), Compare.EQUALS));

        ImOrderMap<ImMap<Object, Object>, ImMap<Object, Object>> emailResult = emailQuery.execute(context);
        for (ImMap<Object, Object> entry : emailResult.values()) {
            skipEmails.put((String) entry.get("id"), new EmailData((LocalDateTime) entry.get("dateTimeSent"), entry.get("skip") != null));
        }
        return skipEmails;
    }

    private static void importFolders(ExecutionContext context, EmailLogicsModule LM, DataObject accountObject, List<List<Object>> dataFolderElements, List<List<Object>> dataFolderParents) throws SQLException, SQLHandledException {
        importFoldersElements(context, LM, accountObject, dataFolderElements);
        importFolderParents(context, LM, accountObject, dataFolderParents);
    }

    private static void importFoldersElements(ExecutionContext context, EmailLogicsModule LM, DataObject accountObject, List<List<Object>> data) throws SQLException, SQLHandledException {
        List<ImportProperty<?>> props = new ArrayList<>();
        List<ImportField> fields = new ArrayList<>();
        List<ImportKey<?>> keys = new ArrayList<>();

        ImportField idFolderField = new ImportField(LM.idFolder);
        ImportKey<?> folderKey = new ImportKey(LM.folder, LM.folderAccountId.getMapping(accountObject, idFolderField));
        keys.add(folderKey);
        props.add(new ImportProperty(idFolderField, LM.idFolder.getMapping(folderKey)));
        props.add(new ImportProperty(accountObject, LM.accountFolder.getMapping(folderKey)));
        fields.add(idFolderField);

        runIntegrationService(context, props, fields, keys, data);
    }

    private static void importFolderParents(ExecutionContext context, EmailLogicsModule LM, DataObject accountObject, List<List<Object>> data) throws SQLException, SQLHandledException {
        List<ImportProperty<?>> props = new ArrayList<>();
        List<ImportField> fields = new ArrayList<>();
        List<ImportKey<?>> keys = new ArrayList<>();

        ImportField idFolderField = new ImportField(LM.idFolder);
        ImportField idParentFolderField = new ImportField(LM.idFolder);

        ImportKey<?> keyElement = new ImportKey(LM.folder, LM.folderAccountId.getMapping(accountObject, idFolderField));
        keys.add(keyElement);
        ImportKey<?> keyParent = new ImportKey(LM.folder, LM.folderAccountId.getMapping(accountObject, idParentFolderField));
        keys.add(keyParent);
        props.add(new ImportProperty(idParentFolderField, LM.parentFolder.getMapping(keyElement), LM.object(LM.folder).getMapping(keyParent)));
        fields.add(idFolderField);
        fields.add(idParentFolderField);

        runIntegrationService(context, props, fields, keys, data);
    }

    private static void importEmails(ExecutionContext context, EmailLogicsModule LM, DataObject accountObject, List<List<Object>> data) throws SQLException, SQLHandledException {
        List<ImportProperty<?>> props = new ArrayList<>();
        List<ImportField> fields = new ArrayList<>();
        List<ImportKey<?>> keys = new ArrayList<>();

        ImportField idEmailField = new ImportField(LM.idEmail);
        ImportKey<?> emailKey = new ImportKey(LM.email, LM.emailId.getMapping(accountObject, idEmailField));
        keys.add(emailKey);
        props.add(new ImportProperty(idEmailField, LM.idEmail.getMapping(emailKey)));
        props.add(new ImportProperty(accountObject, LM.accountEmail.getMapping(emailKey)));
        fields.add(idEmailField);

        ImportField idFolderField = new ImportField(LM.idFolder);
        ImportKey<?> folderKey = new ImportKey(LM.email, LM.folderAccountId.getMapping(accountObject, idFolderField));
        keys.add(folderKey);
        props.add(new ImportProperty(idFolderField, LM.folderEmail.getMapping(emailKey), LM.object(LM.folder).getMapping(folderKey)));

        fields.add(idFolderField);

        ImportField dateTimeSentEmailField = new ImportField(LM.dateTimeSentEmail);
        props.add(new ImportProperty(dateTimeSentEmailField, LM.dateTimeSentEmail.getMapping(emailKey), true));
        fields.add(dateTimeSentEmailField);

        ImportField dateTimeReceivedEmailField = new ImportField(LM.dateTimeReceivedEmail);
        props.add(new ImportProperty(dateTimeReceivedEmailField, LM.dateTimeReceivedEmail.getMapping(emailKey), true));
        fields.add(dateTimeReceivedEmailField);

        ImportField fromAddressEmailField = new ImportField(LM.fromAddressEmail);
        props.add(new ImportProperty(fromAddressEmailField, LM.fromAddressEmail.getMapping(emailKey), true));
        fields.add(fromAddressEmailField);

        ImportField toAddressEmailField = new ImportField(LM.toAddressEmail);
        props.add(new ImportProperty(toAddressEmailField, LM.toAddressEmail.getMapping(emailKey), true));
        fields.add(toAddressEmailField);

        ImportField ccAddressEmailField = new ImportField(LM.ccAddressEmail);
        props.add(new ImportProperty(ccAddressEmailField, LM.ccAddressEmail.getMapping(emailKey), true));
        fields.add(ccAddressEmailField);

        ImportField bccAddressEmailField = new ImportField(LM.bccAddressEmail);
        props.add(new ImportProperty(bccAddressEmailField, LM.bccAddressEmail.getMapping(emailKey), true));
        fields.add(bccAddressEmailField);

        ImportField subjectEmailField = new ImportField(LM.subjectEmail);
        props.add(new ImportProperty(subjectEmailField, LM.subjectEmail.getMapping(emailKey), true));
        fields.add(subjectEmailField);

        ImportField messageEmailField = new ImportField(LM.messageEmail);
        props.add(new ImportProperty(messageEmailField, LM.messageEmail.getMapping(emailKey), true));
        fields.add(messageEmailField);

        ImportField emlFileEmailField = new ImportField(LM.emlFileEmail);
        props.add(new ImportProperty(emlFileEmailField, LM.emlFileEmail.getMapping(emailKey), true));
        fields.add(emlFileEmailField);

        runIntegrationService(context, props, fields, keys, data);
    }

    private static void importAttachments(ExecutionContext context, EmailLogicsModule LM, List<List<Object>> data, DataObject accountObject) throws SQLException, SQLHandledException {
        List<ImportProperty<?>> props = new ArrayList<>();
        List<ImportField> fields = new ArrayList<>();
        List<ImportKey<?>> keys = new ArrayList<>();

        ImportField idEmailField = new ImportField(LM.idEmail);
        ImportKey<?> emailKey = new ImportKey(LM.email, LM.emailId.getMapping(accountObject, idEmailField));
        emailKey.skipKey = true;
        keys.add(emailKey);
        fields.add(idEmailField);

        ImportField idAttachmentEmailField = new ImportField(LM.idAttachmentEmail);
        ImportKey<?> attachmentEmailKey = new ImportKey(LM.attachmentEmail, LM.attachmentEmailIdEmail.getMapping(idAttachmentEmailField, idEmailField));
        keys.add(attachmentEmailKey);
        props.add(new ImportProperty(idAttachmentEmailField, LM.idAttachmentEmail.getMapping(attachmentEmailKey)));
        props.add(new ImportProperty(idEmailField, LM.emailAttachmentEmail.getMapping(attachmentEmailKey),
                LM.object(LM.email).getMapping(emailKey)));
        fields.add(idAttachmentEmailField);

        ImportField nameAttachmentEmailField = new ImportField(LM.nameAttachmentEmail);
        props.add(new ImportProperty(nameAttachmentEmailField, LM.nameAttachmentEmail.getMapping(attachmentEmailKey)));
        fields.add(nameAttachmentEmailField);

        ImportField fileAttachmentEmailField = new ImportField(LM.fileAttachmentEmail);
        props.add(new ImportProperty(fileAttachmentEmailField, LM.fileAttachmentEmail.getMapping(attachmentEmailKey)));
        fields.add(fileAttachmentEmailField);

        runIntegrationService(context, props, fields, keys, data);
    }

    private static List<List<List<Object>>> downloadEmailList(ExecutionContext context, String receiveHost, Integer receivePort, String user, String password,
                                                              AccountType accountType, boolean startTLS, boolean deleteMessages, Integer maxMessages,
                                                              Map<String, EmailData> skipEmails, LocalDateTime minDateTime,
                                                              boolean unpack, boolean ignoreExceptions, boolean insecureSSL,
                                                              boolean readAllFolders)
            throws MessagingException, IOException, GeneralSecurityException {

        List<List<Object>> dataFolderElements = new ArrayList<>();
        List<List<Object>> dataFolderParents = new ArrayList<>();
        List<List<Object>> dataEmails = new ArrayList<>();
        List<List<Object>> dataAttachments = new ArrayList<>();
        System.setProperty("mail.mime.base64.ignoreerrors", "true"); //ignore errors decoding base64

        Store emailStore = getEmailStore(receiveHost, accountType, startTLS, insecureSSL);
        if (receivePort != null)
            emailStore.connect(receiveHost, receivePort, user, password);
        else
            emailStore.connect(receiveHost, user, password);

        List<Folder> folders = getSubFolders(readAllFolders ? emailStore.getDefaultFolder() : emailStore.getFolder("INBOX"));

        for (Folder emailFolder : folders) {

            emailFolder.open(Folder.READ_WRITE);
            String idFolder = emailFolder.getName();
            String parentFolder = nullEmpty(emailFolder.getParent().getName());

            dataFolderElements.add(Collections.singletonList(idFolder));
            dataFolderParents.add(Arrays.asList(idFolder, parentFolder));

            LocalDateTime dateTimeReceivedEmail = LocalDateTime.now();

            int count = 1;
            Message[] messages = emailFolder.getMessages();
            int messageCount = messages.length;
            ServerLoggers.mailLogger.info(String.format("Account %s, folder %s: found %s emails", user, emailFolder.getFullName(), messageCount));
            Set<String> usedEmails = new HashSet<>();
            int folderClosedCount = 0;
            while (count <= messageCount && (maxMessages == null || dataEmails.size() < maxMessages)) {
                try {
                    Message message = messages[messageCount - count];

                    String uid = getMessageUID(emailFolder, message);
                    EmailData emailData = skipEmails.get(uid);
                    LocalDateTime dateTimeSentEmail = emailData != null ? emailData.dateTimeSent : getSentDate(message);
                    boolean skip = emailData != null && emailData.skip;

                    if (!skip && !(minDateTime != null && dateTimeSentEmail != null && minDateTime.isAfter(dateTimeSentEmail))) {
                        ServerLoggers.mailLogger.info(String.format("Reading email %s of %s, date %s (%s of %s)", dataEmails.size() + 1, maxMessages, dateTimeSentEmail, count, messageCount));
                        String from = joinAddresses(message.getFrom());
                        String to = joinAddresses(message.getRecipients(Message.RecipientType.TO));
                        String cc = joinAddresses(message.getRecipients(Message.RecipientType.CC));
                        String bcc = joinAddresses(message.getRecipients(Message.RecipientType.BCC));
                        String subjectEmail = message.getSubject();

                        String idEmail = getEmailId(localDateTimeToSqlTimestamp(dateTimeSentEmail), from, subjectEmail, usedEmails);
                        ServerLoggers.mailLogger.info("idEmail: " + idEmail);
                        usedEmails.add(idEmail);

                        message.setFlag(deleteMessages ? Flags.Flag.DELETED : Flags.Flag.SEEN, true);
                        Object messageContent = getEmailContent(message);
                        MultipartBody messageEmail = getEmailMessage(subjectEmail, message, messageContent, unpack);
                        if (messageEmail == null) {
                            messageEmail = new MultipartBody(messageContent == null ? null : String.valueOf(messageContent), null);
                            ServerLoggers.mailLogger.error("Warning: missing attachment '" + messageContent + "' from email '" + subjectEmail + "'");
                        }
                        FileData emlFileEmail = new FileData(getEMLByteArray(message), "eml");
                        dataEmails.add(Arrays.asList(uid, idFolder, dateTimeSentEmail, dateTimeReceivedEmail, from, to, cc, bcc, subjectEmail, messageEmail.message, emlFileEmail));
                        int counter = 1;
                        if (messageEmail.attachments != null) {
                            for (Map.Entry<String, FileData> entry : messageEmail.attachments.entrySet()) {
                                dataAttachments.add(Arrays.asList(uid, String.valueOf(counter), BaseUtils.getFileName(entry.getKey()), entry.getValue()));
                                counter++;
                            }
                        }
                    } else {
                        ServerLoggers.mailLogger.info(String.format("Skipping email %s of %s, date %s", count, messageCount, dateTimeSentEmail));
                        if (minDateTime != null && dateTimeSentEmail != null && minDateTime.minusDays(1).isAfter(dateTimeSentEmail)) {
                            ServerLoggers.mailLogger.info("Breaking reading, all next emails will be older then minimum date");
                            break;
                        }
                        if (emailData == null) {
                            dataEmails.add(Arrays.asList(uid, idFolder, dateTimeSentEmail, null, null, user, null, null, null, null, null));
                        }
                    }

                    count++;
                    folderClosedCount = 0;
                } catch (FolderClosedException | FolderClosedIOException e) {
                    if (folderClosedCount < 2) {
                        folderClosedCount++;
                        ServerLoggers.mailLogger.error("Ignored exception :", e);
                        emailFolder.open(Folder.READ_WRITE);
                    } else {
                        if (ignoreExceptions)
                            count++;
                        else
                            throw e;
                    }
                } catch (Exception e) {
                    if (ignoreExceptions) {
                        ServerLoggers.mailLogger.error("Ignored exception :", e);
                        context.messageError(e.toString(), localize("{mail.receiving}"));
                        count++;
                        folderClosedCount = 0;
                    } else throw e;
                }
            }

            emailFolder.close(true);
        }
        emailStore.close();

        return Arrays.asList(dataFolderElements, dataFolderParents, dataEmails, dataAttachments);
    }

    private static String joinAddresses(Address[] addresses) {
        List<String> result = new ArrayList<>();
        if(addresses != null) {
            for (int i = 0; i < addresses.length; i++) {
                Address address = addresses[i];
                if (address instanceof InternetAddress) {
                    result.add(((InternetAddress) address).getAddress());
                }
            }
        }
        return result.isEmpty() ? null : StringUtils.join(result, ", ");
    }

    private static String getMessageUID(Folder folder, Message message) throws MessagingException {
        if(folder instanceof UIDFolder) {
            return String.valueOf(((UIDFolder) folder).getUID(message));
        } else {
            return ((POP3Folder) folder).getUID(message);
        }
    }

    private static List<Folder> getSubFolders(Folder folder) throws MessagingException {
        List<Folder> folders = new ArrayList<>();
        if (!(folder instanceof DefaultFolder))
            folders.add(folder);
        //pop3 doesn't allow subfolders
        if (!(folder instanceof POP3Folder)) {
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
            ServerLoggers.mailLogger.error("getEmailContent exception : ", e);
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

    private static LocalDateTime getSentDate(Message message) {
        return (LocalDateTime) BaseUtils.executeWithTimeout(() -> {
            Date sentDate = message.getSentDate();
            return sentDate == null ? null : sqlTimestampToLocalDateTime(new Timestamp(sentDate.getTime()));
        }, 60000L);
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

                File f = File.createTempFile("attachment", "");
                try {
                    copyInputStreamToFile(bp.getInputStream(), f);
                    RawFileData file = new RawFileData(f);

                    byte[] bytes = file.getBytes();
                    if(Settings.get().ignoreBodyStructureSizeFix && fileName.endsWith(".dbf") && bytes[bytes.length - 1] == 0x0d && bytes[bytes.length - 1] == 0x0a) {
                        file = new RawFileData(Arrays.copyOfRange(bytes, 0, bytes.length - 2));
                    }

                    if (bp.getContentType() != null && bp.getContentType().contains("application/ms-tnef")) {
                        attachments.putAll(extractWinMail(f).attachments);
                    } else {
                        attachments.putAll(unpack(file, fileName, unpack));
                    }

                } catch (IOException e) {
                    ServerLoggers.mailLogger.error(prefix + "Error reading attachment '" + fileName + "' from email '" + subjectEmail + "'");
                    throw e;
                } finally {
                    BaseUtils.safeDelete(f);
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
                        body = parseContent(content);
                        ServerLoggers.mailLogger.info(prefix + "attachment is String, length: " + body.length());
                    }
                } catch (IOException e) {
                    throw new RuntimeException("Email subject: " + subjectEmail, e);
                }
            }
        }
        return new MultipartBody(body, attachments);
    }

    //pgsql doesn't support string with 0x00, throws ERROR: invalid byte sequence for encoding "UTF8": 0x00
    private static String parseContent(Object content) {
        return String.valueOf(content).replace("\u0000", "");
    }

    private static Object getBodyPartContent(BodyPart bp) throws MessagingException, IOException {
        Object content = null;
        if(bp instanceof IMAPBodyPart) {
            String encoding = ((IMAPBodyPart) bp).getEncoding();
            if (encoding != null) {
                Object plainContent = null;
                try {
                    plainContent = bp.getContent();
                } catch (FolderClosedException | FolderClosedIOException e) {
                    throw e;
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
        String extension = BaseUtils.getFileExtension(fileName);
        Map<String, FileData> attachments = unpack ? ZipUtils.unpackFile(byteArray, extension, false) : new HashMap<>();
        if (attachments.isEmpty()) {
            attachments.put(fileName, new FileData(byteArray, extension));
        }
        return attachments;
    }

    private static void runIntegrationService(ExecutionContext context, List<ImportProperty<?>> props, List<ImportField> fields, List<ImportKey<?>> keys, List<List<Object>> data) throws SQLException, SQLHandledException {
        try (ExecutionContext.NewSession newContext = context.newSession()) {
            new IntegrationService(newContext, new ImportTable(fields, data), keys, props).synchronize(true, false);
            newContext.apply();
        }
    }

    private static class EmailData {
        private LocalDateTime dateTimeSent;
        private boolean skip;

        public EmailData(LocalDateTime dateTimeSent, boolean skip) {
            this.dateTimeSent = dateTimeSent;
            this.skip = skip;
        }
    }
}


