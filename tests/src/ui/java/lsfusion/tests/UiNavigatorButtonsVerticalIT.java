package lsfusion.tests;

import com.microsoft.playwright.Locator;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

// UiNavigatorButtonsIT's application (src/ui/buttons) with the navbar vertical, the server's own setting: a test server of
// its own, as the navbar takes a reload to change
public class UiNavigatorButtonsVerticalIT extends UiTest {

    private static final String DATABASE = "lsfusion_test_ui_buttons_vertical_" + ProcessHandle.current().pid();
    private static Process server;
    private static int rmiPort;

    @BeforeClass
    public static void startButtonsServer() throws Exception {
        int[] ports = TestServer.freePorts(4);
        rmiPort = ports[1];
        server = startServer(DATABASE, ports, TestServer.BASE.resolve("src/ui/buttons"), "user.navbar=vertical");
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

    // with the vertical navbar the standard strip aligns its items to the start, which a component that draws the window
    // lays out itself: its own .navbar-nav keeps the alignment it gives it
    @Test
    public void aComponentsOwnNavbarNavKeepsItsAlignment() {
        for (String window : new String[] {"System.logo", "System.root", "System.system"}) {
            Locator own = page.locator("[lsfusion-container='" + window + "'] .ui-nb-own");
            assertThat(own).hasCount(1);
            assertThat(own).hasCSS("align-items", "center");
        }
    }
}
