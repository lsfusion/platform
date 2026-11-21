package lsfusion.tests;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.net.CookieManager;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

// A user with the second factor - the setting on, an e-mail - logs in to the web client by the password and the code from
// the mail, on the login page; any other login by the password alone runs nothing as the user (HttpSecondFactor.lsf)
public class UiSecondFactorIT extends UiTest {

    private static final String PASSWORD = "usfPassword1";
    private static final String WHO = "/exec?action=HttpSecondFactor.hsfWho";

    // the login reads the setting through a cache the server flushes every second
    @BeforeClass
    public static void secondFactorOn() throws Exception {
        check("HttpSecondFactor.hsfOn");
        Thread.sleep(2_000);
    }

    @AfterClass
    public static void secondFactorOff() throws Exception {
        check("HttpSecondFactor.hsfOff");
    }

    private static String user(String prefix) throws Exception {
        String login = prefix + System.nanoTime();
        check("HttpSecondFactor.hsfUser", login, PASSWORD);
        return login;
    }

    // the login page asks for the code after the password; the code logs the user in
    @Test
    public void loginPageAsksForTheCode() throws Exception {
        String login = user("usfForm");
        page.navigate(webClient() + "/login");
        page.locator("#username").fill(login);
        page.locator("#password").fill(PASSWORD);
        page.locator("#login-form [type=submit]").click();
        assertThat(page.locator("#submit-2fa-form")).isVisible();

        check("HttpSecondFactor.hsfSetCode", login, "123456"); // the code the mail carries
        page.locator("#code").fill("123456");
        page.locator("#submit-2fa-form [type=submit]").click();
        page.locator("[lsfusion-container='System.root']").waitFor();

        assertEquals(login, page.context().request().post(webClient() + WHO).text());
    }

    // the password in the API's Basic login runs nothing as the user
    @Test
    public void basicLoginRunsNothing() throws Exception {
        String login = user("usfBasic");
        HttpResponse<String> response = HttpClient.newHttpClient().send(HttpRequest.newBuilder(URI.create(webClient() + WHO))
                .header("Authorization", "Basic " + Base64.getEncoder().encodeToString((login + ":" + PASSWORD).getBytes(StandardCharsets.UTF_8)))
                .timeout(Duration.ofMinutes(1)).POST(HttpRequest.BodyPublishers.noBody()).build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(response.body(), 401, response.statusCode());
    }

    // the password in the address runs nothing as the user: the address - of a request, of the web client's page - is
    // opened without the login
    @Test
    public void addressLoginRunsNothing() throws Exception {
        String login = user("usfAddress");
        String credentials = "user=" + URLEncoder.encode(login, StandardCharsets.UTF_8) + "&password=" + PASSWORD;
        HttpClient client = HttpClient.newBuilder().cookieHandler(new CookieManager()).followRedirects(HttpClient.Redirect.NORMAL).build();
        HttpResponse<String> response = client.send(HttpRequest.newBuilder(URI.create(webClient() + WHO + "&" + credentials))
                .timeout(Duration.ofMinutes(1)).POST(HttpRequest.BodyPublishers.noBody()).build(), HttpResponse.BodyHandlers.ofString());
        assertNotEquals(login, response.body());

        page.navigate(webClient() + "/main?" + credentials);
        assertNotEquals(login, page.context().request().post(webClient() + WHO).text());
    }
}
