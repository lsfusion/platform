package lsfusion.tests;

import com.microsoft.playwright.Locator;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

// a navigator window a React component draws, placing the platform's buttons with <Platform> (src/ui/navigator:
// UiNavigatorWindow.lsf, UiNavigatorWindow.js). A window is on every page of its application, so the class runs against
// a test server of its own, on the suite's web client
public class UiNavigatorWindowIT extends UiTest {

    private static final String DATABASE = "lsfusion_test_ui_navigator_" + ProcessHandle.current().pid();
    private static Process server;
    private static int rmiPort;

    @BeforeClass
    public static void startNavigatorServer() throws Exception {
        int[] ports = TestServer.freePorts(4);
        rmiPort = ports[1];
        server = startServer(DATABASE, ports, TestServer.BASE.resolve("src/ui/navigator"));
    }

    @AfterClass
    public static void stopNavigatorServer() throws Exception {
        if (server != null)
            TestServer.stop(server, DATABASE);
    }

    @Override
    protected String main() {
        return "/main?port=" + rmiPort;
    }

    // the window draws no button for an element SHOWIF hides, so the place <Platform> rendered for it waited on the page,
    // empty, with the room the component gave it: the element's entry says it is hidden, and the place is hidden while
    // it does - and holds the button again once SHOWIF shows the element
    @Test
    public void thePlaceOfAButtonShowIfHidesIsHidden() {
        Locator place = place("uiNavigatorWindowSometimes"), entry = entry("uiNavigatorWindowSometimes");
        assertThat(place("uiNavigatorWindowToggle").locator(buttonOf("uiNavigatorWindowToggle"))).isVisible();
        assertThat(place).hasCount(1);
        assertHiddenByItsEntry(place);
        assertThat(entry).hasText("hidden");

        toggle(); // the answer to it brings the navigator's changes, SHOWIF's among them
        assertThat(entry).hasText("shown");
        assertThat(place.locator(buttonOf("uiNavigatorWindowSometimes"))).isVisible();

        toggle();
        assertThat(entry).hasText("hidden");
        assertHiddenByItsEntry(place);
    }

    // hidden, and by <Platform>, which gives the place `hidden` - not by anything above it
    private static void assertHiddenByItsEntry(Locator place) {
        assertThat(place).isHidden();
        assertThat(place).hasAttribute("hidden", "");
    }

    private void toggle() {
        place("uiNavigatorWindowToggle").locator(buttonOf("uiNavigatorWindowToggle")).click();
    }

    // what the component draws for an element, named by its name in the module
    private Locator place(String element) {
        return page.locator(".ui-nw-place." + element);
    }

    private Locator entry(String element) {
        return page.locator(".ui-nw-entry." + element);
    }

    private static String buttonOf(String element) {
        return "[lsfusion-container='UiNavigatorWindow." + element + "']";
    }
}
