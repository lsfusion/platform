package lsfusion.server.physics.dev.integration.external.to.mail;


import com.sun.mail.smtp.SMTPMessage;
import com.sun.mail.util.MailSSLSocketFactory;
import lsfusion.base.BaseUtils;
import lsfusion.base.file.RawFileData;
import lsfusion.server.base.controller.thread.ExecutorFactory;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.physics.admin.log.ServerLoggers;
import org.apache.log4j.Logger;

import javax.activation.DataHandler;
import javax.mail.*;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.util.ByteArrayDataSource;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ScheduledExecutorService;

import static lsfusion.base.BaseUtils.trimToEmpty;
import static lsfusion.server.base.controller.thread.ThreadLocalContext.localize;

public class EmailSender {
    private final static Logger logger = ServerLoggers.mailLogger;

    public static void sendMail(ExecutionContext context, String fromAddress, Map<String, Message.RecipientType> recipients, final String subject, List<String> inlineFiles, List<AttachmentFile> attachments,
                                String smtpHost, String smtpPort, String encryptedConnectionType, String user, String password, boolean syncType, boolean insecureSSL) throws MessagingException, IOException, GeneralSecurityException {

        String protocol = "SSL".equals(encryptedConnectionType) ? "smtps" : "smtp";

        Properties mailProps = new Properties();
        mailProps.setProperty("mail.host", smtpHost);
        mailProps.setProperty("mail.from", fromAddress);

        mailProps.setProperty("mail." + protocol + ".timeout", "120000");
        mailProps.setProperty("mail." + protocol + ".connectiontimeout", "60000");

        System.setProperty("mail.mime.multipart.allowempty", "true");

        if (!smtpPort.isEmpty()) {
            mailProps.setProperty("mail." + protocol + ".port", smtpPort);
        }
        if ("TLS".equals(encryptedConnectionType))
            mailProps.setProperty("mail.smtp.starttls.enable", "true");
        if ("SSL".equals(encryptedConnectionType)) {
            mailProps.put("mail.smtps.socketFactory.port", smtpPort);

            if (insecureSSL) {
                MailSSLSocketFactory socketFactory = new MailSSLSocketFactory();
                socketFactory.setTrustAllHosts(true);
                mailProps.put("mail.smtps.socketFactory", socketFactory);
            } else
                mailProps.put("mail.smtps.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
        }

        if (!user.isEmpty() && !password.isEmpty()) {
            mailProps.setProperty("mail." + protocol + ".auth", "true");
        }

        SMTPMessage message = new SMTPMessage(Session.getInstance(mailProps, null));
        message.setFrom();
        message.setSentDate(new java.util.Date());
        message.setSubject(subject, "utf-8");

        for (Map.Entry<String, Message.RecipientType> recipient : recipients.entrySet()) {
            message.addRecipients(recipient.getValue(), recipient.getKey());
        }

        Multipart mp = new MimeMultipart();
        for(String inlineFile : inlineFiles)
            if(inlineFile != null)
                setText(mp, inlineFile);
        for (AttachmentFile attachment : attachments)
            attachFile(mp, attachment);

        message.setContent(mp);

        if (syncType) {
            processEmail(subject, true, smtpHost, smtpPort, protocol, user, password, fromAddress, message);
        } else {
            ScheduledExecutorService executor = ExecutorFactory.createNewThreadService(context);
            executor.submit(() -> processEmail(subject, false, smtpHost, smtpPort, protocol, user, password, fromAddress, message));
            executor.shutdown();
        }
    }

    private static void setText(Multipart mp, String text) throws MessagingException, IOException {
        MimeBodyPart textPart = new MimeBodyPart();
        textPart.setDataHandler(new DataHandler(new ByteArrayDataSource(text, "text/html; charset=utf-8")));
        textPart.setDisposition(Part.INLINE);
        mp.addBodyPart(textPart);
    }

    private static void attachFile(Multipart mp, AttachmentFile attachment) throws MessagingException, IOException {
        ByteArrayDataSource dataSource = new ByteArrayDataSource(attachment.file.getInputStream(), getMimeType(attachment.extension));
        MimeBodyPart filePart = new MimeBodyPart();
        filePart.setDataHandler(new DataHandler(dataSource));
        filePart.setFileName(attachment.attachmentName);
        filePart.setHeader("Content-Transfer-Encoding", "base64");
        mp.addBodyPart(filePart);
    }

    private static String getMimeType(String extension) {
        switch (extension) {
            case "pdf":
                return "application/pdf; charset=utf-8";
            case "docx":
                return "application/vnd.openxmlformats-officedocument.wordprocessingml.document; charset=utf-8";
            case "rtf":
                return "text/rtf; charset=utf-8";
            case "xls":
            case "xlsx":
                return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet; charset=utf-8";
            case "dbf":
                return "application/dbf; charset=utf-8";
            case "csv":
                return "text/csv; charset=utf-8";
            default:
                return "text/html; charset=utf-8";
        }
    }

    private static void processEmail(String subject, boolean syncType, String smtpHost, String smtpPort, String protocol,
                                     String user, String password, String fromAddress, SMTPMessage message) {
        String messageInfo = trimToEmpty(subject);
        try {
            Address[] addressesTo = message.getRecipients(MimeMessage.RecipientType.TO);
            checkNoAddressTo(addressesTo, messageInfo);

            messageInfo += " " + localize("{mail.sender}") + " : " + fromAddress;
            messageInfo += " " + localize("{mail.recipients}") + " : " + BaseUtils.toString(",", addressesTo);
        } catch (MessagingException me) {
            messageInfo += " " + localize("{mail.failed.to.get.list.of.recipients}") + " " + me;
        }

        boolean send = false;
        int count = 0;
        while (!send) {
            send = true;
            count++;
            try {
                sendMessage(message, smtpHost, smtpPort, protocol, user, password);
                logger.info(localize("{mail.successful.mail.sending}") + " : " + messageInfo);
            } catch (MessagingException e) {
                if (!syncType) {
                    send = false;
                    if (count < 40) {
                        logger.info(localize("{mail.unsuccessful.attempt.to.send.mail}") + " : " + e.getMessage() + " " + messageInfo);
                        try {
                            Thread.sleep(30000);
                        } catch (InterruptedException ignored) {
                        }
                    } else {
                        logger.error(localize("{mail.failed.to.send.mail}") + " : " + messageInfo, e);
                        throw new RuntimeException(localize("{mail.error.send.mail}") + " : " + messageInfo, e);
                    }
                } else {
                    logger.error(localize("{mail.failed.to.send.mail}") + " : " + messageInfo, e);
                    throw new RuntimeException(localize("{mail.error.send.mail}") + " : " + messageInfo, e);
                }
            }
        }
    }

    private static void sendMessage(SMTPMessage message, String smtpHost, String smtpPort, String protocol,
                                    String user, String password) throws MessagingException {
        Integer port = parsePort(smtpPort);
        Transport transport = message.getSession().getTransport(protocol);
        if(port == null)
            transport.connect(smtpHost, user, password);
        else
            transport.connect(smtpHost, port, user, password);
        transport.sendMessage(message, message.getAllRecipients());
    }

    private static void checkNoAddressTo(Address[] addressesTo, String messageInfo) {
        if (addressesTo == null || addressesTo.length == 0) {
            logger.error(localize("{mail.failed.to.send.mail}") + " " + messageInfo + " : " + localize("{mail.recipient.not.specified}"));
            throw new RuntimeException(localize("{mail.error.send.mail}") + " " + messageInfo + " : " + localize("{mail.recipient.not.specified}"));
        }
    }

    private static Integer parsePort(String port) {
        try {
            return Integer.parseInt(port);
        } catch (Exception e) {
            return null;
        }
    }

    public static class AttachmentFile {
        private RawFileData file;
        private String attachmentName;
        private String extension;

        public AttachmentFile(RawFileData file, String attachmentName, String extension) {
            this.file = file;
            this.attachmentName = attachmentName;
            this.extension = extension;
        }
    }
}
