package platform.server;


import org.apache.log4j.Logger;
import platform.base.BaseUtils;
import platform.base.ByteArray;
import platform.server.logics.EmailActionProperty;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.util.ByteArrayDataSource;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class EmailSender {
    private final static Logger logger = Logger.getLogger(EmailSender.class);

    MimeMessage message;
    Multipart mp = new MimeMultipart();
    Properties mailProps = new Properties();
    String userName;
    String password;
    List<String> emails = new ArrayList<String>();

    public static class AttachmentProperties {
        public String fileName;
        public String attachmentName;
        public EmailActionProperty.Format format;

        public AttachmentProperties(String fileName, String attachmentName, EmailActionProperty.Format format) {
            this.fileName = fileName;
            this.attachmentName = attachmentName;
            this.format = format;
        }
    }

    public EmailSender(String smtpHost, String fromAddress, List<String> targets) {
        //mailProps.setProperty("mail.debug", "true");
        mailProps.setProperty("mail.smtp.host", smtpHost);
        mailProps.setProperty("mail.from", fromAddress);
        emails = targets;
    }

    public EmailSender(String smtpHost, String smtpPort, String fromAddress, String userName, String password, List<String> targets) {
        this(smtpHost, fromAddress, targets);

        if (smtpPort != null) {
            mailProps.put("mail.smtp.port", smtpPort);
            mailProps.setProperty("mail.smtp.starttls.enable", "true");
            if (smtpPort.equals("465")) {
                mailProps.put("mail.smtp.socketFactory.port", smtpPort);
                mailProps.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
            }
        }

        if (userName != null && password != null) {
            mailProps.setProperty("mail.smtp.auth", "true");
            this.userName = userName;
            this.password = password;
        }
    }

    private Session getSession() {
        if (mailProps.containsKey("mail.smtp.auth") && mailProps.getProperty("mail.smtp.auth").equals("true")) {
            return Session.getInstance(mailProps, new Authenticator() {
                protected PasswordAuthentication getPasswordAuthentication() {
                    return (new PasswordAuthentication(userName, password));
                }
            });
        } else {
            return Session.getInstance(mailProps, null);
        }
    }

    private void setMessageHeading() throws MessagingException {
        message = new MimeMessage(getSession());
        message.setFrom();
        message.setSentDate(new java.util.Date());
        setRecipients(emails);
    }

    public void setRecipients(List<String> targets) throws MessagingException {
        InternetAddress dests[] = new InternetAddress[targets.size()];
        for (int i = 0; i < targets.size(); i++) {
            dests[i] = new InternetAddress(targets.get(i).trim().toLowerCase());
        }
        message.setRecipients(MimeMessage.RecipientType.TO, dests);
    }

    public void setText(String text) throws MessagingException, IOException {
        MimeBodyPart textPart = new MimeBodyPart();
        textPart.setDataHandler(new DataHandler(new ByteArrayDataSource(text, "text/html; charset=utf-8")));
        textPart.setDisposition(Part.INLINE);
        mp.addBodyPart(textPart);
    }

    private String getMimeType(EmailActionProperty.Format format) {
        switch (format) {
            case PDF:
                return "application/pdf";
            case DOCX:
                return "application/msword";
            case RTF:
                return "text/rtf";
            default:
                return "text/html";
        }
    }

    public void attachFile(AttachmentProperties props) throws MessagingException, IOException {
        FileDataSource fds = new FileDataSource(props.fileName);
        ByteArrayDataSource dataSource = new ByteArrayDataSource(fds.getInputStream(), getMimeType(props.format));
        attachFile(dataSource, props.attachmentName);
    }

    public void attachFile(byte[] buf, String attachmentName) throws MessagingException {
        ByteArrayDataSource dataSource = new ByteArrayDataSource(buf, getMimeType(EmailActionProperty.Format.PDF));
        attachFile(dataSource, attachmentName);
    }

    private void attachFile(DataSource source, String attachmentName) throws MessagingException {
        MimeBodyPart filePart = new MimeBodyPart();
        filePart.setDataHandler(new DataHandler(source));
        filePart.setFileName(attachmentName);
        mp.addBodyPart(filePart);
    }

    public void sendMail(String subject, List<AttachmentProperties> attachmentForms, Map<ByteArray, String> files) throws MessagingException, IOException {
        sendMail(subject, new ArrayList<String>(), attachmentForms, files);
    }

    private String createInlinePart(List<String> inlineForms) throws IOException {
        String result = "";
        for (String path : inlineForms) {
            BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(path), "utf-8"));
            while (in.ready()) {
                result += in.readLine();
            }
        }
        if (result.equals("")) {
            result = "Вам пришли печатные формы";
        }
        return result;
    }

    public void sendMail(String subject, List<String> inlineForms, List<AttachmentProperties> attachmentForms, Map<ByteArray, String> files) throws MessagingException, IOException {
        assert inlineForms != null && attachmentForms != null && files != null;

        setMessageHeading();
        message.setSubject(subject, "utf-8");

        setText(createInlinePart(inlineForms));

        for (AttachmentProperties formProps : attachmentForms) {
            attachFile(formProps);
        }
        for (Map.Entry<ByteArray, String> entry : files.entrySet()) {
            attachFile(entry.getKey().array, entry.getValue());
        }

        message.setContent(mp);
        sendMail(message, subject);
    }

    private void sendMail(final MimeMessage message, final String subject) {
        new Thread() {
            public void run() {

                String messageInfo = subject.trim();
                try {
                    messageInfo += " получатели : " + BaseUtils.toString(message.getRecipients(MimeMessage.RecipientType.TO), ",");
                } catch (MessagingException me) {
                    messageInfo += " не удалось получить список получателей " + me.toString();
                }

                boolean send = false;
                boolean error = false;
                int count = 0;
                while (!send) {
                    send = true;
                    count++;
                    try {
                        Transport.send(message);
                    } catch (MessagingException e) {
                        if (count < 40) {
                            logger.info("Неудачная попытка отсылки почты " + messageInfo);
                            send = false;
                            try {
                                Thread.sleep(30000);
                            } catch (InterruptedException e1) {
                            }
                        } else {
                            error = true;
                            logger.error("Не удалось отправить почту " + messageInfo);
                            throw new RuntimeException("Ошибка при отсылке почты " + messageInfo, e);
                        }
                    }
                }
                if (!error) {
                    logger.info("Успешная отсылка почты" + messageInfo);
                }
            }
        }.start();
    }
}
