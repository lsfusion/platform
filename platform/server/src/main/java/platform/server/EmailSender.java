package platform.server;


import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;

public class EmailSender {
    String smtpHost = "169.254.1.6";
    String fromAddress = "luxsoft@adsl.by";
    Session mailSession;
    MimeMessage message;
    Multipart mp = new MimeMultipart();

    public EmailSender(String... targets) {
        Properties mailProps = new Properties();
        mailProps.setProperty("mail.smtp.host", smtpHost);
        mailProps.put("mail.from", fromAddress);

        mailSession = Session.getInstance(mailProps, null);

        message = new MimeMessage(mailSession);
        try {
            message.setFrom();
            setRecipients(targets);
        } catch (MessagingException e) {
            e.printStackTrace();
        }
    }

    public void setRecipients(String... targets) {
        InternetAddress dests[] = new InternetAddress[targets.length];
        try {
            for (int i = 0; i < targets.length; i++) {
                dests[i] = new InternetAddress(targets[i].trim().toLowerCase());
            }
            message.setRecipients(MimeMessage.RecipientType.TO, dests);
        } catch (MessagingException e) {
            e.printStackTrace();
        }
    }

    public void setTheme(String theme) {
        try {
            message.setSubject(theme);
        } catch (MessagingException e) {
            e.printStackTrace();
        }
    }

    public void setText(String text) {
        MimeBodyPart textPart = new MimeBodyPart();
        try {

            textPart.setDataHandler((new DataHandler(new HTMLDataSource(text))));
            textPart.attachFile("d:\\test.txt");
            mp.addBodyPart(textPart);
        } catch (MessagingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static class HTMLDataSource implements DataSource {
        private String html;

        public HTMLDataSource(String htmlString) {
            html = htmlString;
        }

        public InputStream getInputStream() throws IOException {
            if (html == null) throw new IOException("null html");
            return new ByteArrayInputStream(html.getBytes());
        }

        public OutputStream getOutputStream() throws IOException {
            throw new IOException("HTMLDataHandler cannot write HTML");
        }

        public String getContentType() {
            return "text/html";
        }

        public String getName() {
            return "text/html dataSource";
        }
    }

    public void attachFile(String path) {
        try {
            mp.addBodyPart(addFile(path));
        } catch (MessagingException e) {
            e.printStackTrace();
        }
    }

    private MimeBodyPart addFile(String path) {
        MimeBodyPart filePart = new MimeBodyPart();
        FileDataSource fds = new FileDataSource(path);
        try {
            filePart.setDataHandler(new DataHandler(fds));
            filePart.setFileName(fds.getName());
        } catch (MessagingException e) {
            e.printStackTrace();
        }
        return filePart;
    }

    public void sendMail(String theme, String text, String... filePaths) {
        try {
            message.setSentDate(new java.util.Date());
            setTheme(theme);
            setText(text);
            for (String path : filePaths) {
                attachFile(path);
            }
            message.setContent(mp);
            Transport.send(message);
        } catch (MessagingException e) {
            e.printStackTrace();
        }
    }
}
