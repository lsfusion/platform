package lsfusion.server.physics.dev.integration.external.to.mail;

import com.github.junrar.Archive;
import com.github.junrar.exception.RarException;
import com.github.junrar.impl.FileVolumeManager;
import com.github.junrar.rarfile.FileHeader;
import com.google.common.base.Throwables;
import com.sun.mail.util.BASE64DecoderStream;
import lsfusion.base.BaseUtils;
import lsfusion.base.file.FileData;
import lsfusion.base.file.RawFileData;
import lsfusion.interop.action.MessageClientAction;
import lsfusion.server.data.value.DataObject;
import lsfusion.server.data.value.NullValue;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.classes.user.ConcreteCustomClass;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.admin.log.ServerLoggers;
import org.apache.poi.hmef.Attachment;
import org.apache.poi.hmef.HMEFMessage;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.internet.MimeUtility;
import java.io.*;
import java.nio.charset.Charset;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static lsfusion.server.base.controller.thread.ThreadLocalContext.localize;
import static lsfusion.server.logics.classes.data.time.DateTimeConverter.sqlTimestampToLocalDateTime;

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
                    emailLM.findProperty("name[AttachmentEmail]").change(getFileNameWithoutExt(attachment.name), session, attachmentObject);
                    emailLM.findProperty("file[AttachmentEmail]").change(attachment.file, session, attachmentObject);
                }
                String result = session.applyMessage();
                if (result != null) {
                    throw new RuntimeException(result);
                }
            }

        } catch (Exception e) {
            logger.error(localize("{mail.failed.to.receive.mail}"), e);
            context.delayUserInterfaction(new MessageClientAction(localize("{mail.failed.to.receive.mail}") + " : " + e.toString(), localize("{mail.receiving}")));
        }

    }

    private Email parseEML(boolean unpack, FileData eml) throws MessagingException, IOException {

        Session emailSession = Session.getInstance(new Properties());
        MimeMessage message = new MimeMessage(emailSession, new ByteArrayInputStream(eml.getRawFile().getBytes()));

        Timestamp dateTimeSent = getSentDate(message);
        String fromAddress = ((InternetAddress) message.getFrom()[0]).getAddress();
        String subjectEmail = message.getSubject();

        Object messageContent = getEmailContent(message);
        MultipartBody messageEmail = messageContent instanceof Multipart ? getMultipartBody(subjectEmail, (Multipart) messageContent, unpack) : messageContent instanceof BASE64DecoderStream ? getMultipartBody64(subjectEmail, (BASE64DecoderStream) messageContent, decodeFileName(message.getFileName()), unpack) : messageContent instanceof String ? new MultipartBody((String) messageContent, null) : null;
        if (messageEmail == null) {
            messageEmail = new MultipartBody(messageContent == null ? null : String.valueOf(messageContent), null);
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

    private Object getEmailContent(Message email) throws IOException, MessagingException {
        Object content;
        try {
            content = email.getContent();
        } catch (MessagingException | IOException | NullPointerException e) {
            // did this due to a bug
            try {
                content = new MimeMessage((MimeMessage) email).getContent();
            } catch (IOException e1) {
                if ("Unknown encoding: utf-8".equalsIgnoreCase(e1.getMessage())) content = null;
                else throw e;
            }
        }
        return content;
    }

    private MultipartBody getMultipartBody(String subjectEmail, Multipart mp, boolean unpack) throws IOException, MessagingException {
        String body = "";
        Map<String, FileData> attachments = new HashMap<>();
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

                    if(bp.getContentType() != null && bp.getContentType().contains("application/ms-tnef")) {
                        attachments.putAll(extractWinMail(f).attachments);
                    } else {
                        attachments.putAll(unpack(new RawFileData(f), fileName, unpack));
                    }

                } catch (IOException ioe) {
                    ServerLoggers.mailLogger.error("Error reading attachment '" + fileName + "' from email '" + subjectEmail + "'");
                    throw ioe;
                } finally {
                    if (!f.delete()) f.deleteOnExit();
                }
            } else {
                Object content = bp.getContent();
                if (content instanceof BASE64DecoderStream) {
                    RawFileData byteArray = new RawFileData((BASE64DecoderStream) content);
                    String fileName = decodeFileName(bp.getFileName());
                    attachments.putAll(unpack(byteArray, fileName, unpack));
                } else if (content instanceof MimeMultipart) {
                    body = getMultipartBody(subjectEmail, (Multipart) content, unpack).message;
                } else body = String.valueOf(content);
            }
        }
        return new MultipartBody(body, attachments);
    }

    private MultipartBody extractWinMail(File winMailFile) throws IOException {
        HMEFMessage msg = new HMEFMessage(new FileInputStream(winMailFile));
        Map<String, FileData> attachments = new HashMap<>();
        for(Attachment attach : msg.getAttachments()) {
            String attachName = attach.getFilename();
            attachments.put(attachName, new FileData(new RawFileData(attach.getContents()), BaseUtils.getFileExtension(attachName)));
        }
        return new MultipartBody(msg.getBody(), attachments);
    }

    private MultipartBody getMultipartBody64(String subjectEmail, BASE64DecoderStream base64InputStream, String fileName, boolean unpack) throws IOException {
        RawFileData byteArray = new RawFileData(base64InputStream);
        Map<String, FileData> attachments = new HashMap<>(unpack(byteArray, fileName, unpack));
        return new MultipartBody(subjectEmail, attachments);
    }

    private String decodeFileName(String value) throws UnsupportedEncodingException {
        if (value == null) value = "attachment.txt";
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
        Map<String, FileData> attachments;

        private MultipartBody(String message, Map<String, FileData> attachments) {
            this.message = message;
            this.attachments = attachments;
        }
    }

    private Map<String, FileData> unpack(RawFileData byteArray, String fileName, boolean unpack) {
        Map<String, FileData> attachments = new HashMap<>();
        String[] fileNameAndExt = fileName.split("\\.");
        String fileExtension = fileNameAndExt.length > 1 ? fileNameAndExt[fileNameAndExt.length - 1].trim() : "";
        if (unpack) {
            if (fileExtension.toLowerCase().equals("rar")) {
                attachments.putAll(unpackRARFile(byteArray));
            } else if (fileExtension.toLowerCase().equals("zip")) {
                attachments.putAll(unpackZIPFile(byteArray));
            }
        }
        if (attachments.isEmpty()) attachments.put(fileName, new FileData(byteArray, fileExtension));
        return attachments;
    }

    private Map<String, FileData> unpackRARFile(RawFileData rawFile) {

        Map<String, FileData> result = new HashMap<>();
        File inputFile = null;
        File outputFile = null;
        try {
            inputFile = File.createTempFile("email", ".rar");
            rawFile.write(inputFile);

            List<File> dirList = new ArrayList<>();
            File outputDirectory = new File(inputFile.getParent() + "/" + getFileNameWithoutExt(inputFile));
            if (inputFile.exists() && (outputDirectory.exists() || outputDirectory.mkdir())) {
                dirList.add(outputDirectory);
                Archive a = new Archive(new FileVolumeManager(inputFile));

                FileHeader fh = a.nextFileHeader();

                while (fh != null) {
                    String fileName = (fh.isUnicode() ? fh.getFileNameW() : fh.getFileNameString());
                    outputFile = new File(outputDirectory.getPath() + "/" + fileName);
                    File dir = outputFile.getParentFile();
                    dir.mkdirs();
                    if (!dirList.contains(dir)) dirList.add(dir);
                    if (!outputFile.isDirectory()) {
                        try (FileOutputStream os = new FileOutputStream(outputFile)) {
                            a.extractFile(fh, os);
                        }
                        result.put(getFileName(result, fileName), new FileData(new RawFileData(outputFile), BaseUtils.getFileExtension(outputFile)));
                        if (!outputFile.delete()) outputFile.deleteOnExit();
                    }
                    fh = a.nextFileHeader();
                }
                a.close();
            }

            for (File dir : dirList)
                if (dir != null && dir.exists() && !dir.delete()) dir.deleteOnExit();

        } catch (RarException | IOException e) {
            throw Throwables.propagate(e);
        } finally {
            if (inputFile != null && !inputFile.delete()) inputFile.deleteOnExit();
            if (outputFile != null && !outputFile.delete()) outputFile.deleteOnExit();
        }
        return result;
    }

    private Map<String, FileData> unpackZIPFile(RawFileData rawFile) {

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
            File outputDirectory = new File(inputFile.getParent() + "/" + getFileNameWithoutExt(inputFile));
            if (inputFile.exists() && (outputDirectory.exists() || outputDirectory.mkdir())) {
                dirList.add(outputDirectory);
                ZipInputStream inputStream = new ZipInputStream(new FileInputStream(inputFile), Charset.forName("cp866"));

                ZipEntry ze = inputStream.getNextEntry();
                while (ze != null) {
                    if (ze.isDirectory()) {
                        File dir = new File(outputDirectory.getPath() + "/" + ze.getName());
                        dir.mkdirs();
                        dirList.add(dir);
                    } else {
                        String fileName = ze.getName();
                        outputFile = new File(outputDirectory.getPath() + "/" + fileName);
                        File parentDir = outputFile.getParentFile();
                        if (!parentDir.exists()) {
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
                        if (!outputFile.delete()) outputFile.deleteOnExit();
                    }
                    ze = inputStream.getNextEntry();
                }
                inputStream.closeEntry();
                inputStream.close();
            }

            for (File dir : dirList)
                if (dir != null && dir.exists() && !dir.delete()) dir.deleteOnExit();

        } catch (IOException e) {
            throw Throwables.propagate(e);
        } finally {
            if (inputFile != null && !inputFile.delete()) inputFile.deleteOnExit();
            if (outputFile != null && !outputFile.delete()) outputFile.deleteOnExit();
        }
        return result;
    }

    private String getFileName(Map<String, FileData> files, String fileName) {
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