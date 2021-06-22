package lsfusion.server.physics.dev.integration.external.to.mail;


import com.sun.mail.smtp.SMTPMessage;
import lsfusion.base.BaseUtils;
import lsfusion.base.file.RawFileData;
import lsfusion.server.base.controller.thread.ExecutorFactory;
import lsfusion.server.base.controller.thread.ThreadLocalContext;
import lsfusion.server.language.property.LP;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.action.session.DataSession;
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
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.ScheduledExecutorService;

import static lsfusion.server.base.controller.thread.ThreadLocalContext.localize;

public class EmailSender {
    private final static Logger logger = ServerLoggers.mailLogger;
    SMTPMessage message;
    Properties mailProps = new Properties();
    String userName;
    String password;
    String smtpHost;
    String smtpPort;
    String fromAddress;
    Map<String, Message.RecipientType> emails = new HashMap<>();

    public static class AttachmentFile {
        public RawFileData file;
        public String attachmentName;
        public String extension;

        public AttachmentFile(RawFileData file, String attachmentName, String extension) {
            this.file = file;
            this.attachmentName = attachmentName;
            this.extension = extension;
        }
    }

    public EmailSender(String smtpHostAccount, String fromAddressAccount, Map<String, Message.RecipientType> targets) {
        //mailProps.setProperty("mail.debug", "true");
        mailProps.setProperty("mail.smtp.host", smtpHostAccount);
        mailProps.setProperty("mail.from", fromAddressAccount);
        mailProps.setProperty("mail.smtp.timeout", "120000");
        mailProps.setProperty("mail.smtp.connectiontimeout", "60000");
        emails = targets;
    }

    public EmailSender(String smtpHostAccount, String smtpPortAccount, String encryptedConnectionType, String fromAddressAccount, String userName, String password, Map<String, Message.RecipientType> targets) {
        this(smtpHostAccount, fromAddressAccount, targets);

        System.setProperty("mail.mime.multipart.allowempty", "true");

        if (!smtpPortAccount.isEmpty()) {
            mailProps.put("mail.smtp.port", smtpPortAccount);
        }
        if ("TLS".equals(encryptedConnectionType))
            mailProps.setProperty("mail.smtp.starttls.enable", "true");
        if ("SSL".equals(encryptedConnectionType)) {
            mailProps.put("mail.smtp.socketFactory.port", smtpPortAccount);
            mailProps.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
        }

        if (!userName.isEmpty() && !password.isEmpty()) {
            mailProps.setProperty("mail.smtp.auth", "true");
            this.userName = userName;
            this.password = password;
        }
        this.smtpHost = smtpHostAccount;
        this.smtpPort = smtpPortAccount;
        this.fromAddress = fromAddressAccount;
    }

    private Session getSession() {
        return Session.getInstance(mailProps, null);
    }

    private void setMessageHeading(String subject) throws MessagingException {
        message = new SMTPMessage(getSession());
        message.setFrom();
        message.setSentDate(new java.util.Date());
        setRecipients(emails);
        message.setSubject(subject, "utf-8");
    }


    public void setRecipients(Map<String,Message.RecipientType> targets) throws MessagingException {
        for (Map.Entry<String, Message.RecipientType> target : targets.entrySet()) {
            message.addRecipients(target.getValue(), target.getKey());
        }
    }

    public void setText(Multipart mp, String text) throws MessagingException, IOException {
        MimeBodyPart textPart = new MimeBodyPart();
        textPart.setDataHandler(new DataHandler(new ByteArrayDataSource(text, "text/html; charset=utf-8")));
        textPart.setDisposition(Part.INLINE);
        mp.addBodyPart(textPart);
    }

    private String getMimeType(String extension) {
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

    public void attachFile(Multipart mp, AttachmentFile attachment) throws MessagingException, IOException {
        ByteArrayDataSource dataSource = new ByteArrayDataSource(attachment.file.getInputStream(), getMimeType(attachment.extension));
        MimeBodyPart filePart = new MimeBodyPart();
        filePart.setDataHandler(new DataHandler(dataSource));
        filePart.setFileName(attachment.attachmentName);
        filePart.setHeader("Content-Transfer-Encoding", "base64");
        mp.addBodyPart(filePart);
    }

    public void sendMail(ExecutionContext context, final String subject, List<String> inlineFiles, List<AttachmentFile> attachments, boolean syncType) throws MessagingException, IOException {
        Multipart mp = new MimeMultipart();
        setMessageHeading(subject);

        for(String inlineFile : inlineFiles)
            if(inlineFile != null)
                setText(mp, inlineFile);

        for (AttachmentFile attachment : attachments)
            attachFile(mp, attachment);

        message.setContent(mp);
        final LP emailSent = context.getBL().emailLM.emailSent;

        if (syncType) {
            processEmail(subject, emailSent, true);
        } else {
            ScheduledExecutorService executor = ExecutorFactory.createNewThreadService(context);
            executor.submit(() -> {
                processEmail(subject, emailSent, false);
            });
            executor.shutdown();
        }
    }

    private void processEmail(String subject, LP emailSent, boolean syncType) {
        String messageInfo = subject != null ? subject.trim() : "";
        try {
            Address[] addressesTo = message.getRecipients(MimeMessage.RecipientType.TO);
            if (addressesTo == null || addressesTo.length == 0) {
                logger.error(localize("{mail.failed.to.send.mail}")+" " + messageInfo + " : "+localize("{mail.recipient.not.specified}"));
                throw new RuntimeException(localize("{mail.error.send.mail}") + " " + messageInfo + " : "+localize("{mail.recipient.not.specified}"));
            }
            messageInfo += " " + localize("{mail.sender}") + " : " + fromAddress;
            messageInfo += " " + localize("{mail.recipients}") + " : " + BaseUtils.toString(",", addressesTo);
        } catch (MessagingException me) {
            messageInfo += " "+localize("{mail.failed.to.get.list.of.recipients}")+" " + me.toString();
        }

        boolean send = false;
        int count = 0;
        try {
            while (!send) {
                send = true;
                count++;
                try {
                     sendMessage(message, smtpHost, smtpPort, userName, password);
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
        } finally {
            try {
                try (DataSession session = ThreadLocalContext.createSession()){
                    emailSent.change(send ? true : null, session);
                }
            } catch (Exception e) {
                logger.error("emailSent writing error", e);
            }
        }
    }

    private void sendMessage(SMTPMessage message, String smtpHost, String smtpPort, String userName, String password) throws MessagingException {
        trustAllCerts();
        Integer port = parsePort(smtpPort);
        Transport transport = message.getSession().getTransport(port != null && port.equals(25) ? "smtp" : "smtps");
        if(port == null)
            transport.connect(smtpHost, userName, password);
        else
            transport.connect(smtpHost, port, userName, password);
        transport.sendMessage(message, message.getAllRecipients());
    }

    private void trustAllCerts() {
        SSLContext ctx;
        TrustManager[] trustAllCerts = new X509TrustManager[]{new X509TrustManager(){
            public java.security.cert.X509Certificate[] getAcceptedIssuers(){return null;}
            public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType){}
            public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType){}
        }};
        try {
            ctx = SSLContext.getInstance("SSL");
            ctx.init(null, trustAllCerts, null);
            SSLContext.setDefault(ctx);
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            logger.info("Error loading ssl context {}", e);
        }
    }

    private Integer parsePort(String port) {
        try {
            return Integer.parseInt(port);
        } catch (Exception e) {
            return null;
        }
    }

    @Deprecated
    public static class AttachmentProperties {
        public String fileName;
        public String attachmentName;
        public AttachmentFormat format;

        public AttachmentProperties(String fileName, String attachmentName, AttachmentFormat format) {
            this.fileName = fileName;
            this.attachmentName = attachmentName;
            this.format = format;
        }
    }

    @Deprecated
    public void sendSimpleMail(ExecutionContext context, String subject, List<AttachmentProperties> attachments) throws MessagingException, IOException {
        assert attachments != null;

        Multipart mp = new MimeMultipart();
        setMessageHeading(subject);

        for (AttachmentProperties attachment : attachments) {
            attachFile(mp, new AttachmentFile(new RawFileData(attachment.fileName), attachment.attachmentName, attachment.format.getExtension()));
        }
        message.setContent(mp);

        String messageInfo = subject.trim();
        try {
            Address[] addressesTo = message.getRecipients(MimeMessage.RecipientType.TO);
            if (addressesTo == null || addressesTo.length == 0) {
                logger.error(localize("{mail.failed.to.send.mail}")+" " + messageInfo + " : "+localize("{mail.recipient.not.specified}"));
                throw new RuntimeException(localize("{mail.error.send.mail}") + " " + messageInfo + " : "+localize("{mail.recipient.not.specified}"));
            }
            messageInfo += " "+localize("{mail.recipients}")+" : " + BaseUtils.toString(",", addressesTo);
        } catch (MessagingException me) {
            messageInfo += " "+localize("{mail.failed.to.get.list.of.recipients}")+" " + me.toString();
        }

        boolean send = false;
        try {
            sendMessage(message, smtpHost, smtpPort, userName, password);
            send = true;
        } catch (MessagingException e) {
            throw new RuntimeException(localize("{mail.error.send.mail}") + " " + messageInfo, e);
        } finally {
            try {
                if (context != null) {
                    LP emailSent = context.getBL().emailLM.findProperty("emailSent[]");
                    if(emailSent != null)
                        emailSent.change(send ? true : null, context.getSession());
                }
            } catch (Exception e) {
                logger.error("emailSent writing error", e);
            }
        }
    }
}
