package lsfusion.tests;

import com.microsoft.playwright.Locator;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.regex.Pattern;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.Assert.assertEquals;

// the platform's buttons a React component places in a navigator window (src/ui/buttons: UiNavigatorButtons.lsf,
// UiNavigatorButtons.js): System.logo, System.root, System.system and System.toolbar are drawn by one component, which
// places their NOCUSTOM buttons in one flat row, whatever their depth. A window is on every page of its application, so
// the class runs against a test server of its own, on the suite's web client
public class UiNavigatorButtonsIT extends UiTest {

    private static final String DATABASE = "lsfusion_test_ui_buttons_" + ProcessHandle.current().pid();
    private static Process server;
    private static int rmiPort;

    @BeforeClass
    public static void startButtonsServer() throws Exception {
        int[] ports = TestServer.freePorts(4);
        rmiPort = ports[1];
        server = startServer(DATABASE, ports, "src/ui/buttons");
    }

    @AfterClass
    public static void stopButtonsServer() throws Exception {
        if (server != null)
            TestServer.stop(server, DATABASE);
    }

    @Override
    protected String main() {
        return "/main?port=" + rmiPort;
    }

    // captions shown only on hover are how the standard toolbar draws an unpinned navigator's windows: a button the
    // component places keeps its caption while a form is open and the mouse is away - also once the pin mode is
    // switched round to unpinned again, when the server computes the windows' classes anew
    @Test
    public void aPlacedButtonKeepsItsCaption() {
        String mode = pinMode();
        try {
            pinMode(UNPINNED);
            placed("uiNbForm").click();
            assertThat(form("UiNavigatorButtons.uiNbForm")).isVisible();
            captionsShown();

            pinMode(NAVBAR_PINNED); // and round to unpinned again
            pinMode(UNPINNED);
            captionsShown();
        } finally {
            pinMode(mode);
        }
    }

    // the depth is the standard toolbar's tree indent, and the component lays its buttons out itself: a placed button is
    // drawn at no level - the toolbar's would be indented 1-3rem, the root's (text under the icon) carry the class of
    // their depth -, as it is built, as a refresh changes it (a folder selected) and once the pin mode is switched
    @Test
    public void aPlacedButtonHasNoLevelIndent() {
        trackRequests();
        noIndent();

        placed("uiNbOne").click();
        settled();
        assertThat(placed("uiNbOne")).containsClass("active");
        noIndent();

        String mode = pinMode();
        try {
            pinMode(UNPINNED);
            noIndent();
        } finally {
            pinMode(mode);
        }
    }

    // the standard strip's own layout - its items aligned to the start with the vertical navbar, the logo's icon as tall
    // as the horizontal strip - is not given to a window the component draws, and is to one the platform draws
    @Test
    public void theServerGivesTheWindowsNoStripLayoutClasses() {
        placed("uiNbClasses").click();
        Locator form = form("UiNavigatorButtons.uiNbClasses");
        assertThat(value(form, "logoWindowClass")).not().containsText("navbar-auto-icon-font-size");
        assertThat(value(form, "uiNbStandardLogoClasses")).containsText("navbar-auto-icon-font-size");
        assertThat(value(form, "uiNbVerticalClasses")).not().containsText("navbar-force-align-start");
        for (String window : new String[] {"Logo", "Root", "System"})
            assertThat(value(form, "uiNbStandardVertical" + window + "Classes")).containsText("navbar-force-align-start");
    }

    private Locator placed(String name) {
        return page.locator(".ui-nb-place > [lsfusion-container='UiNavigatorButtons." + name + "']");
    }

    // the value a panel property of a form shows
    private static Locator value(Locator form, String property) {
        String name = "[lsfusion-container='PROPERTY(" + property + "())']";
        return form.locator(".panel-renderer-value" + name + ", " + name + " .panel-renderer-value");
    }

    private Locator placedButtons(String window) {
        return page.locator("[lsfusion-container='" + window + "'] .ui-nb-place > .nav-link");
    }

    // a form open and the mouse away: the root's placed buttons show their captions, and nothing above them has the
    // class that hides captions until hover - the window layout moves a window's class to the panel it shares with others
    private void captionsShown() {
        page.mouse().move(640, 400);
        navigatorSettled(page.locator("[lsfusion-container='System.root']"));
        Locator buttons = placedButtons("System.root");
        assertThat(buttons).hasCount(3);
        for (Locator button : buttons.all()) {
            caption(button, true);
            assertEquals(false, button.evaluate("button => !!button.closest('.navbar-text-on-hover')"));
        }
    }

    private void noIndent() {
        for (String window : new String[]{"System.toolbar", "System.root"}) {
            Locator buttons = placedButtons(window);
            assertThat(buttons).hasCount(3);
            for (Locator button : buttons.all()) {
                assertThat(button).hasCSS("margin-left", "0px");
                assertThat(button).containsClass(window.equals("System.root") ? "nav-link-vert-0" : "nav-link-horz-0");
                assertThat(button).not().hasClass(Pattern.compile("\\bnav-link-(horz|vert)-[1-9][0-9]*\\b"));
            }
        }
    }
}
