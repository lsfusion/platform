package lsfusion.tests;

import com.microsoft.playwright.Locator;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.Assert.assertEquals;

// the platform's buttons a React component places in a navigator window (src/ui/buttons: UiNavigatorButtons.lsf,
// UiNavigatorButtons.js): System.root and System.toolbar are drawn by one component, which places their NOCUSTOM buttons
// in one flat row, whatever their depth. A window is on every page of its application, so the class runs against a test
// server of its own, on the suite's web client
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

    private Locator placed(String name) {
        return page.locator(".ui-nb-place > [lsfusion-container='UiNavigatorButtons." + name + "']");
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
}
