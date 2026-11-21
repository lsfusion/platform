package lsfusion.tests;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.Response;
import org.junit.Test;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertEquals;

// The pages a browser gets before a login write what a request gives them as text: the login page the name a failed
// login keeps, the page of a notification its id, which goes into the page's script only as a number. Each page opens
// in a tab of its own in the test's context
public class UiLoginPagesIT extends UiTest {

    // drawn as markup, it closes the field's value and adds an element
    private static final String MARKUP = "\"><b id=\"written\">x</b>";

    @Test
    public void theLoginPageShowsTheNameOfAFailedLoginAsText() {
        Page tab = page.context().newPage();
        tab.navigate(webClient() + "/main?user=" + encode(MARKUP) + "&password=x");
        assertEquals(MARKUP, tab.locator("#username").inputValue());
        assertEquals(0, tab.locator("#written").count());
    }

    @Test
    public void theNotificationPageTakesOnlyANumberForItsId() {
        Page tab = page.context().newPage();
        Response response = tab.navigate(webClient() + "/push-notification?notification_id=" + encode("</script><script>window.written = true</script>"));
        assertEquals(200, response.status());
        assertEquals(1, tab.locator("body#push-notification").count()); // the page itself, drawn
        assertEquals(false, tab.evaluate("() => window.written === true"));
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
