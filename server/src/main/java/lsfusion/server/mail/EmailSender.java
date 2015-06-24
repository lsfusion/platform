package lsfusion.server.mail;


import com.sun.mail.smtp.SMTPMessage;
import lsfusion.base.BaseUtils;
import lsfusion.base.ByteArray;
import lsfusion.server.ServerLoggers;
import lsfusion.server.logics.ServerResourceBundle;
import org.apache.log4j.Logger;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.*;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.util.ByteArrayDataSource;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class EmailSender {
    private final static Logger logger = ServerLoggers.mailLogger;

    SMTPMessage message;
    Multipart mp = new MimeMultipart();
    Properties mailProps = new Properties();
    String userName;
    String password;
    String smtpHost;
    String smtpPort;
    String fromAddress;
    Map<String, Message.RecipientType> emails = new HashMap<String, Message.RecipientType>();

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

    public EmailSender(String smtpHostAccount, String fromAddressAccount, Map<String, Message.RecipientType> targets) {
        //mailProps.setProperty("mail.debug", "true");
        mailProps.setProperty("mail.smtp.host", smtpHostAccount);
        mailProps.setProperty("mail.from", fromAddressAccount);
        emails = targets;
    }

    public EmailSender(String smtpHostAccount, String smtpPortAccount, String encryptedConnectionType, String fromAddressAccount, String userName, String password, Map<String, Message.RecipientType> targets) {
        this(smtpHostAccount, fromAddressAccount, targets);

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

    private void setMessageHeading() throws MessagingException {
        message = new SMTPMessage(getSession());
        message.setFrom();
        message.setSentDate(new java.util.Date());
        setRecipients(emails);
    }


    public void setRecipients(Map<String,Message.RecipientType> targets) throws MessagingException {
        for (Map.Entry<String, Message.RecipientType> target : targets.entrySet()) {
            message.addRecipients(target.getValue(), target.getKey());
        }
    }

    public void setText(String text) throws MessagingException, IOException {
        MimeBodyPart textPart = new MimeBodyPart();
        textPart.setDataHandler(new DataHandler(new ByteArrayDataSource(text, "text/html; charset=utf-8")));
        textPart.setDisposition(Part.INLINE);
        mp.addBodyPart(textPart);
    }

    private String getMimeType(AttachmentFormat format) {
        switch (format) {
            case PDF:
                return "application/pdf; charset=utf-8";
            case DOCX:
                return "application/vnd.openxmlformats-officedocument.wordprocessingml.document; charset=utf-8";
            case RTF:
                return "text/rtf; charset=utf-8";
            case XLSX:
                return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet; charset=utf-8";
            case DBF:
                return "application/dbf; charset=utf-8";
            default:
                return "text/html; charset=utf-8";
        }
    }

    public void attachFile(AttachmentProperties attachment) throws MessagingException, IOException {
        FileDataSource fds = new FileDataSource(attachment.fileName);
        ByteArrayDataSource dataSource = new ByteArrayDataSource(fds.getInputStream(), getMimeType(attachment.format));
        attachFile(dataSource, attachment.attachmentName);
    }

    public void attachFile(byte[] buf, String attachmentName) throws MessagingException {
        ByteArrayDataSource dataSource = new ByteArrayDataSource(buf, getMimeType(AttachmentFormat.PDF));
        attachFile(dataSource, attachmentName);
    }

    private void attachFile(DataSource source, String attachmentName) throws MessagingException {
        MimeBodyPart filePart = new MimeBodyPart();
        filePart.setDataHandler(new DataHandler(source));
        filePart.setFileName(attachmentName);
        mp.addBodyPart(filePart);
    }

    private String convertFilesToUtf(List<String> files) throws IOException {
        String result = "";
        for (String path : files) {
            BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(path), "utf-8"));
            while (in.ready()) {
                result += in.readLine();
            }
        }
        return result;
    }
    
    private String createInlinePart(List<String> inlineFiles) throws IOException {
        String result = convertFilesToUtf(inlineFiles);
        if (result.equals("")) {
            result = ServerResourceBundle.getString("mail.you.have.received.reports");
        }
        return result;
    }

    public void sendMail(String subject, List<String> inlineFiles, List<AttachmentProperties> attachments, Map<ByteArray, String> files) throws MessagingException, IOException {
        assert inlineFiles != null && attachments != null && files != null;

        setMessageHeading();
        message.setSubject(subject, "utf-8");

        setText(createInlinePart(inlineFiles));

        for (AttachmentProperties attachment : attachments) {
            attachFile(attachment);
        }
        for (Map.Entry<ByteArray, String> entry : files.entrySet()) {
            attachFile(entry.getKey().array, entry.getValue());
        }

        message.setContent(mp);
        sendMail(message, subject);
    }

    public void sendSimpleMail(String subject, List<AttachmentProperties> attachments) throws MessagingException, IOException {
        assert attachments != null;

        setMessageHeading();
        message.setSubject(subject, "utf-8");
        for (AttachmentProperties attachment : attachments) {
            attachFile(attachment);
        }
        message.setContent(mp);
        
        String messageInfo = subject.trim();
        try {
            Address[] addressesTo = message.getRecipients(MimeMessage.RecipientType.TO);
            if (addressesTo == null || addressesTo.length == 0) {
                logger.error(ServerResourceBundle.getString("mail.failed.to.send.mail")+" " + messageInfo + " : "+ServerResourceBundle.getString("mail.recipient.not.specified"));
                throw new RuntimeException(ServerResourceBundle.getString("mail.error.send.mail") + " " + messageInfo + " : "+ServerResourceBundle.getString("mail.recipient.not.specified"));
            }
            messageInfo += " "+ServerResourceBundle.getString("mail.recipients")+" : " + BaseUtils.toString(",", addressesTo);
        } catch (MessagingException me) {
            messageInfo += " "+ServerResourceBundle.getString("mail.failed.to.get.list.of.recipients")+" " + me.toString();
        }

        try {
            sendMessage(message, smtpHost, smtpPort, userName, password);
        } catch (MessagingException e) {
            throw new RuntimeException(ServerResourceBundle.getString("mail.error.send.mail") + " " + messageInfo, e);
        }
    }

    public void sendPlainMail(String subject, String inlineForms, List<AttachmentProperties> attachments, Map<ByteArray, String> files) throws MessagingException, IOException {
        assert inlineForms != null && attachments != null && files != null;

        setMessageHeading();
        message.setSubject(subject, "utf-8");

        setText(inlineForms);

        for (AttachmentProperties attachment : attachments) {
            attachFile(attachment);
        }
        for (Map.Entry<ByteArray, String> entry : files.entrySet()) {
            attachFile(entry.getKey().array, entry.getValue());
        }

        message.setContent(mp);
        sendMail(message, subject);
    }

    private void sendMail(final SMTPMessage message, final String subject) {
        new Thread() {
            public void run() {

                String messageInfo = subject.trim();
                try {
                    Address[] addressesTo = message.getRecipients(MimeMessage.RecipientType.TO);
                    if (addressesTo == null || addressesTo.length == 0) {
                        logger.error(ServerResourceBundle.getString("mail.failed.to.send.mail")+" " + messageInfo + " : "+ServerResourceBundle.getString("mail.recipient.not.specified"));
                        throw new RuntimeException(ServerResourceBundle.getString("mail.error.send.mail") + " " + messageInfo + " : "+ServerResourceBundle.getString("mail.recipient.not.specified"));
                    }
                    messageInfo += " "+ServerResourceBundle.getString("mail.recipients")+" : " + BaseUtils.toString(",", addressesTo);
                } catch (MessagingException me) {
                    messageInfo += " "+ServerResourceBundle.getString("mail.failed.to.get.list.of.recipients")+" " + me.toString();
                }

                boolean send = false;
                int count = 0;
                while (!send) {
                    send = true;
                    count++;
                    try {
                        sendMessage(message, smtpHost, smtpPort, userName, password);
                    } catch (MessagingException e) {
                        if (count < 40) {
                            logger.info(ServerResourceBundle.getString("mail.unsuccessful.attempt.to.send.mail") + " " + messageInfo);
                            send = false;
                            try {
                                Thread.sleep(30000);
                            } catch (InterruptedException e1) {
                            }
                        } else {
                            logger.error(ServerResourceBundle.getString("mail.failed.to.send.mail") + " " + messageInfo);
                            throw new RuntimeException(ServerResourceBundle.getString("mail.error.send.mail")+" " + messageInfo, e);
                        }
                    }
                }
                logger.info(ServerResourceBundle.getString("mail.successful.mail.sending") + messageInfo);
            }
        }.start();
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
}
