package lsfusion.tests;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.options.BoundingBox;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.Assert.assertTrue;

// windows declared with one POSITION, which share that place as tabs (src/ui/tabs: UiTabbedWindows.lsf). A window is on
// every page of its application, so the class runs against a test server of its own, on the suite's web client
public class UiTabbedWindowsIT extends UiTest {

    private static final String DATABASE = "lsfusion_test_ui_tabs_" + ProcessHandle.current().pid();
    private static Process server;
    private static int rmiPort;

    @BeforeClass
    public static void startTabsServer() throws Exception {
        int[] ports = TestServer.freePorts(4);
        rmiPort = ports[1];
        server = startServer(DATABASE, ports, TestServer.BASE.resolve("src/ui/tabs"));
    }

    @AfterClass
    public static void stopTabsServer() throws Exception {
        if (server != null)
            TestServer.stop(server, DATABASE);
    }

    @Override
    protected String main() {
        return "/main?port=" + rmiPort;
    }

    // with the navigator not pinned entirely - the server's default pins only the bar -, the web client gave System.toolbar
    // in a tab the classes that make it pop out over the forms beside it: in a tab it had no height, and its items were
    // drawn from there over the tab or the top of the page (#1977). It is drawn as any window in a tab: filling the tab
    // under the tabs' bar, with its items - of the section the navigator selects at the start - inside it
    @Test
    public void theToolbarIsDrawnInItsTab() {
        Locator toolbar = window("System.toolbar");
        Locator panel = page.locator(".tab-panel:has(> [lsfusion-container='System.toolbar'])");
        int index = ((Number) panel.evaluate("panel => [...panel.children].filter(child => child.classList.contains('tab-pane'))"
                + ".findIndex(child => child.getAttribute('lsfusion-container') === 'System.toolbar')")).intValue();
        assertTrue("System.toolbar is not a tab", index >= 0);
        panel.locator(":scope > .tab-bar .nav-link").nth(index).click();

        assertThat(toolbar).isVisible();
        assertThat(toolbar).not().containsClass("navbar-popup-over-selected-hover");

        BoundingBox bar = panel.locator(":scope > .tab-bar").boundingBox(), box = toolbar.boundingBox();
        assertTrue("System.toolbar is " + box.height + " px high", box.height > 100);
        assertTrue("System.toolbar starts at " + box.y + ", the tabs' bar ends at " + (bar.y + bar.height), box.y >= bar.y + bar.height - 1);

        Locator items = toolbar.locator(".nav-link");
        assertThat(items.first()).isVisible(); // the navigator's elements may be drawn after its windows
        BoundingBox first = items.first().boundingBox(), last = items.last().boundingBox();
        assertTrue("System.toolbar's first item starts at " + first.y + ", the toolbar at " + box.y, first.y >= box.y - 1);
        assertTrue("System.toolbar's last item ends at " + (last.y + last.height) + ", the toolbar at " + (box.y + box.height),
                last.y + last.height <= box.y + box.height + 1);
    }

    private Locator window(String name) {
        return page.locator("[lsfusion-container='" + name + "']");
    }
}
