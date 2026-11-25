package lsfusion.tests;

import org.json.JSONObject;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Writer;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLEncoder;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** The mails of the password pages, on the server of DefaultApiIT, with the API's default access (enableAPI=0), and a
 *  mail server of the test's own : the link of a reset mail, and the token in it. */
public class PasswordResetIT {

    // the address of the web server, as an administrator sets it in the options
    private static final String ADDRESS = "http://web.example:8080/app";

    private static int httpPort;
    private static MailServer mails;

    @BeforeClass
    public static void startServer() throws Exception {
        mails = new MailServer();
        DefaultApiIT.start();
        httpPort = DefaultApiIT.httpPort;
        call("HttpPasswordReset.hprMailBy&p=" + mails.port(), "");
        call("HttpPasswordReset.hprAddress&p=" + URLEncoder.encode(ADDRESS, "UTF-8"), "");
    }

    @AfterClass
    public static void stopMails() throws Exception {
        if (mails != null)
            mails.close();
    }

    // the link leads to the web server address set in the options
    @Test
    public void theLinkLeadsToTheAddressOfTheOptions() throws Exception {
        user("prLink", "prlink@example.com");
        reset("prLink", "elsewhere.example:4444");
        String mail = mails.take("prlink@example.com");
        Assert.assertTrue(mail, link(mail).startsWith(ADDRESS + "/change-password?token="));
        Assert.assertFalse(mail, mail.contains("elsewhere.example"));
    }

    // the login is written into the mail's html as text
    @Test
    public void theMailWritesTheLoginAsText() throws Exception {
        user("pr<i>\"Quoted\"</i>", "prquoted@example.com");
        reset("pr<i>\"Quoted\"</i>", null);
        String mail = mails.take("prquoted@example.com");
        Assert.assertTrue(mail, mail.contains("pr&lt;i&gt;&quot;Quoted&quot;&lt;/i&gt;"));
    }

    // a reset answers the same whether or not there is such a login or address, and with no address in the answer
    @Test
    public void theAnswerIsTheSameForAnyLogin() throws Exception {
        user("prKnown", "prknown@example.com");
        user("prByMail", "prbymail@example.com");
        String known = reset("prKnown", null);
        mails.take("prknown@example.com");
        JSONObject answer = new JSONObject(known);
        Assert.assertTrue(known, answer.has("success") && !answer.has("error") && !known.contains("@"));

        Assert.assertEquals(known, reset("prbymail@example.com", null));
        mails.take("prbymail@example.com");
        Assert.assertEquals(known, reset("prUnknown", null));
        Assert.assertEquals(known, reset("prunknown@example.com", null));
    }

    // the token of a mail is good for an hour after the mail is sent
    @Test
    public void aTokenIsRefusedAnHourAfterItsMail() throws Exception {
        user("prAged", "praged@example.com");
        reset("prAged", null);
        String token = token(mails.take("praged@example.com"));

        call("HttpPasswordReset.hprSentAgo&p=prAged&p=3660", "");
        JSONObject refused = change(token);
        Assert.assertTrue(refused.toString(), refused.has("error") && !refused.has("success"));

        call("HttpPasswordReset.hprSentAgo&p=prAged&p=3540", "");
        JSONObject changed = change(token);
        Assert.assertTrue(changed.toString(), changed.has("success"));
    }

    // a user gets one reset mail a minute, and the token of that one stays good
    @Test
    public void aSecondResetWithinAMinuteSendsNoMail() throws Exception {
        user("prTwice", "prtwice@example.com");
        reset("prTwice", null);
        String token = token(mails.take("prtwice@example.com"));

        reset("prTwice", null);
        // a mail of a reset after the second one has come
        user("prAfter", "prafter@example.com");
        reset("prAfter", null);
        mails.take("prafter@example.com");
        Assert.assertNull(mails.poll("prtwice@example.com", 2000));

        Assert.assertTrue(change(token).has("success"));
    }

    // no mail is sent while the address of the web server is not set
    @Test
    public void noMailWhileTheAddressIsNotSet() throws Exception {
        user("prNoAddress", "prnoaddress@example.com");
        call("HttpPasswordReset.hprNoAddress", "");
        try {
            JSONObject answer = new JSONObject(reset("prNoAddress", null));
            Assert.assertTrue(answer.toString(), answer.has("error") && !answer.has("success"));
        } finally {
            call("HttpPasswordReset.hprAddress&p=" + URLEncoder.encode(ADDRESS, "UTF-8"), "");
        }
        Assert.assertNull(mails.poll("prnoaddress@example.com", 2000));
    }

    private static void user(String login, String email) throws Exception {
        call("HttpPasswordReset.hprUser&p=" + URLEncoder.encode(login, "UTF-8") + "&p=" + URLEncoder.encode(email, "UTF-8"), "");
    }

    // as the forgot password page asks for it, with the host the request names : its answer
    private static String reset(String loginOrEmail, String host) throws Exception {
        return post(host != null ? host : "localhost:" + httpPort, "/exec?action=Authentication.resetPassword%5BJSONFILE%5D",
                "[" + new JSONObject().put("userNameOrEmail", loginOrEmail) + "]");
    }

    // as the change password page asks for it
    private static JSONObject change(String token) throws Exception {
        return new JSONObject(call("Authentication.changePassword[JSONFILE]",
                "[" + new JSONObject().put("token", token).put("newPassword", "Prp-Password-1") + "]"));
    }

    // the link of a mail, as a browser reads it
    private static String link(String mail) {
        Matcher href = Pattern.compile("href=\"([^\"]*)\"").matcher(mail);
        Assert.assertTrue(mail, href.find());
        return href.group(1).replace("&amp;", "&");
    }

    private static String token(String mail) {
        Matcher token = Pattern.compile("[?&]token=([^&]+)").matcher(link(mail));
        Assert.assertTrue(mail, token.find());
        return token.group(1);
    }

    // the action with its parameters after it, its signature's brackets escaped : a body that is a json array is sent as
    // json. Its answer, which has to come
    private static String call(String action, String body) throws Exception {
        String contentType = body.startsWith("[") ? "application/json" : "text/plain";
        HttpResponse<String> response = TestServer.post(httpPort, "/exec?action=" + action.replace("[", "%5B").replace("]", "%5D"), body, contentType);
        Assert.assertEquals(action + ": " + response.body(), 200, response.statusCode());
        return response.body();
    }

    // a json request with the Host header given, which the http client of the jdk does not let set : its answer, which
    // has to come. Read by its length : the server keeps the connection open after it
    private static String post(String host, String path, String body) throws Exception {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        String head = "POST " + path + " HTTP/1.1\r\nHost: " + host + "\r\nContent-Type: application/json; charset=UTF-8\r\n" +
                "Content-Length: " + bytes.length + "\r\nConnection: close\r\n\r\n";
        try (Socket socket = new Socket("localhost", httpPort)) {
            socket.setSoTimeout(60_000);
            OutputStream out = socket.getOutputStream();
            out.write(head.getBytes(StandardCharsets.US_ASCII));
            out.write(bytes);
            out.flush();
            InputStream in = socket.getInputStream();
            ByteArrayOutputStream read = new ByteArrayOutputStream();
            while (!read.toString(StandardCharsets.ISO_8859_1).endsWith("\r\n\r\n")) {
                int b = in.read();
                Assert.assertTrue("the answer ended in its headers: " + read, b >= 0);
                read.write(b);
            }
            String headers = read.toString(StandardCharsets.ISO_8859_1);
            Assert.assertTrue(headers, headers.startsWith("HTTP/1.1 200"));
            Matcher length = Pattern.compile("(?im)^content-length:\\s*(\\d+)").matcher(headers);
            Assert.assertTrue(headers, length.find());
            byte[] answer = in.readNBytes(Integer.parseInt(length.group(1)));
            Assert.assertEquals(headers, Integer.parseInt(length.group(1)), answer.length);
            return new String(answer, StandardCharsets.UTF_8);
        }
    }

    // an smtp server that keeps the text of each message it is sent, by recipient
    private static final class MailServer implements AutoCloseable {
        // by freePorts : not a port a server of the other fork is about to take
        private final ServerSocket socket = new ServerSocket(TestServer.freePorts(1)[0]);
        private final Map<String, BlockingQueue<String>> mails = new ConcurrentHashMap<>();

        MailServer() throws IOException {
            Thread thread = new Thread(this::serve, "test mail server");
            thread.setDaemon(true);
            thread.start();
        }

        int port() {
            return socket.getLocalPort();
        }

        String take(String recipient) throws InterruptedException {
            String mail = poll(recipient, 60_000);
            Assert.assertNotNull("no mail to " + recipient, mail);
            return mail;
        }

        String poll(String recipient, long millis) throws InterruptedException {
            return queue(recipient).poll(millis, TimeUnit.MILLISECONDS);
        }

        private BlockingQueue<String> queue(String recipient) {
            return mails.computeIfAbsent(recipient.toLowerCase(Locale.ROOT), r -> new LinkedBlockingQueue<>());
        }

        // a connection each in a thread of its own : the server's mail sender leaves its connection open after a mail
        private void serve() {
            while (!socket.isClosed()) {
                try {
                    Socket client = socket.accept();
                    Thread thread = new Thread(() -> {
                        try (client) {
                            client.setSoTimeout(60_000);
                            session(client);
                        } catch (IOException e) {
                            // the client went away
                        }
                    }, "test mail connection");
                    thread.setDaemon(true);
                    thread.start();
                } catch (IOException e) {
                    // closed
                }
            }
        }

        // every command is answered as done
        private void session(Socket client) throws IOException {
            BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream(), StandardCharsets.ISO_8859_1));
            Writer out = new OutputStreamWriter(client.getOutputStream(), StandardCharsets.ISO_8859_1);
            reply(out, "220 test");
            List<String> recipients = new ArrayList<>();
            String line;
            while ((line = in.readLine()) != null) {
                String command = line.toUpperCase(Locale.ROOT);
                if (command.startsWith("RCPT TO:")) {
                    recipients.add(line.substring("RCPT TO:".length()).trim().replaceAll("^<|>$", ""));
                    reply(out, "250 ok");
                } else if (command.equals("DATA")) {
                    reply(out, "354 go on");
                    StringBuilder message = new StringBuilder();
                    while ((line = in.readLine()) != null && !line.equals("."))
                        message.append(line.startsWith(".") ? line.substring(1) : line).append("\r\n");
                    String text;
                    try {
                        text = text(message.toString());
                    } catch (IOException e) { // for the test waiting for the mail to show
                        text = "the message could not be read: " + e + "\n" + message;
                    }
                    for (String recipient : recipients)
                        queue(recipient).add(text);
                    recipients.clear();
                    reply(out, "250 ok");
                } else if (command.equals("QUIT")) {
                    reply(out, "221 bye");
                    return;
                } else
                    reply(out, "250 ok");
            }
        }

        private static void reply(Writer out, String reply) throws IOException {
            out.write(reply + "\r\n");
            out.flush();
        }

        // the decoded text of all the parts of a message
        private static String text(String message) throws IOException {
            try {
                return text(new MimeMessage(Session.getInstance(new Properties()), new ByteArrayInputStream(message.getBytes(StandardCharsets.ISO_8859_1))).getContent());
            } catch (javax.mail.MessagingException e) {
                throw new IOException(e);
            }
        }

        private static String text(Object content) throws javax.mail.MessagingException, IOException {
            if (!(content instanceof Multipart))
                return String.valueOf(content);
            Multipart parts = (Multipart) content;
            StringBuilder text = new StringBuilder();
            for (int i = 0; i < parts.getCount(); i++)
                text.append(text(parts.getBodyPart(i).getContent()));
            return text.toString();
        }

        @Override
        public void close() throws IOException {
            socket.close();
        }
    }
}
