package lsfusion.tests;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.ColorScheme;
import org.junit.Test;

import java.util.regex.Pattern;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

// The color theme a page of the web client starts in: the one chosen for its user, or else the color scheme its browser
// asks for. The dark theme is the stylesheet of the page and data-bs-theme="dark" on its root element, which the styles
// of an application's own components follow; a page in the light theme has no such attribute until a theme is switched
public class UiColorThemeIT extends UiTest {

    // the page loaded anew by a browser that asks for the scheme
    private void reload(ColorScheme scheme) {
        page.emulateMedia(new Page.EmulateMediaOptions().setColorScheme(scheme));
        page.reload();
        page.locator("[lsfusion-container='System.root']").waitFor(); // the navigator is drawn after the theme is set
    }

    private void assertDark() {
        assertThat(page.locator("html")).hasAttribute("data-bs-theme", "dark");
        assertThat(page.locator("#themeCss")).hasAttribute("href", Pattern.compile("theme/dark\\.css"));
    }

    private void assertLight() {
        assertThat(page.locator("html")).not().hasAttribute("data-bs-theme", "dark");
        assertThat(page.locator("#themeCss")).hasAttribute("href", Pattern.compile("theme/light\\.css"));
    }

    // the scheme the browser asks for as the page is loaded, not the one it asked for a load ago (#2021): the client
    // reports it with its first request, before it reads its settings, while the request for the page itself is sent
    // before any script of the page runs and carries the scheme of the load before. So the first page loaded after the
    // browser has turned to the dark scheme is dark, and the first after it has gone back to the light one is light
    @Test
    public void aPageStartsInTheSchemeItsBrowserAsksFor() {
        assertLight();

        reload(ColorScheme.DARK);
        assertDark();

        reload(ColorScheme.LIGHT);
        assertLight();
    }

    // the theme chosen for the user, whatever scheme the browser asks for - the light one here (#2019: every page
    // started light). The theme is chosen as code chooses it, with no page told, so the page gets it only as it starts
    @Test
    public void aPageStartsInTheChosenTheme() throws Exception {
        assertLight();
        try {
            exec("UiColorTheme.uiColorThemeChoose", "dark");
            reload(ColorScheme.LIGHT);
            assertDark();
        } finally {
            exec("UiColorTheme.uiColorThemeChoose"); // the user is every test's
        }
    }
}
