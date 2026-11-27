package lsfusion.tests;

import com.microsoft.playwright.APIResponse;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.TimeoutError;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.Assert.assertEquals;

// the web client's own texts follow the user's language, not a stale locale cookie (#1103)
public class UiLocaleIT extends UiTest {

    private static final String PASSWORD = "uiLocalePassword1";
    private static final String FOLDER = "UiLocale.uiLocale", FORM = "UiLocale.uiLocaleDates";

    @Before
    public void russianInTheWar() {
        APIResponse mappings = page.context().request().get(webClient() + "/main/compilation-mappings.txt");
        assertEquals(mappings.text(), 200, mappings.status());
        Assume.assumeTrue("the war has no Russian permutation", mappings.text().contains("locale ru"));
    }

    private String login(String language) throws Exception {
        String login = "uiLocale" + System.nanoTime();
        exec("UiLocale.uiLocaleUser", login, PASSWORD, language);
        page.navigate(webClient() + "/login");
        page.locator("#username").fill(login);
        page.locator("#password").fill(PASSWORD);
        page.locator("#login-form [type=submit]").click();
        root();
        return login;
    }

    private void root() {
        page.locator("[lsfusion-container='System.root']").waitFor();
    }

    private void datePicker(String today) {
        Locator form = open(FOLDER, FORM);
        Locator date = form.locator("[lsfusion-container='PROPERTY(uiLocaleDate())']");
        Locator range = page.locator(".daterangepicker .ranges li >> visible=true").first();
        date.click();
        try {
            range.waitFor(new Locator.WaitForOptions().setTimeout(2_000));
        } catch (TimeoutError e) {
            date.dblclick();
        }
        assertThat(range).hasText(today);
        page.keyboard().press("Escape");
        close(form);
    }

    @Test
    public void languageChangedOnTheServer() throws Exception {
        String login = login("en");
        datePicker("Today");

        exec("UiLocale.uiLocaleLanguage", login, "ru");
        page.reload();
        root();
        datePicker("Сегодня");
    }

    @Test
    public void languageInCapitals() throws Exception {
        String login = login("en");
        exec("UiLocale.uiLocaleLanguage", login, "RU");
        page.reload();
        root();
        datePicker("Сегодня");
    }

    @Test
    public void anonymousAfterLogout() throws Exception {
        login("ru");
        datePicker("Сегодня");

        page.navigate(webClient() + "/logout");
        page.navigate(webClient() + "/main");
        root();
        datePicker("Today");
    }
}
