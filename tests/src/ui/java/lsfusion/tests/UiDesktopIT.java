package lsfusion.tests;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.options.BoundingBox;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.Assert.assertEquals;

// NAVIGATOR CUSTOM: the navigator's windows laid out by a React component (src/ui/desktop: UiDesktop.lsf, UiDesktop.js).
// The component draws the whole page, so no other test could share its application: the class runs against a test
// server of its own, on the suite's web client
public class UiDesktopIT extends UiTest {

    private static final String DATABASE = "lsfusion_test_ui_desktop_" + ProcessHandle.current().pid();
    private static Process server;
    private static int rmiPort;

    @BeforeClass
    public static void startDesktopServer() throws Exception {
        int[] ports = TestServer.freePorts(4);
        rmiPort = ports[1];
        server = startServer(DATABASE, ports, TestServer.BASE.resolve("src/ui/desktop"));
    }

    @AfterClass
    public static void stopDesktopServer() throws Exception {
        if (server != null)
            TestServer.stop(server, DATABASE);
    }

    @Override
    protected String main() {
        return "/main?port=" + rmiPort;
    }

    // every window is drawn by its own view where the component put it, and the standard layout's splits are gone
    @Test
    public void theComponentLaysTheWindowsOut() {
        for (String window : new String[] {"System.root", "System.system", "System.toolbar"})
            assertThat(place(window).locator("[lsfusion-container='" + window + "']")).hasCount(1);
        assertEquals(1, place("System.log").evaluate("host => host.childElementCount")); // the log's panel, which has no name of its own
        assertThat(place("System.root").locator("[lsfusion-container='UiDesktop.uiDesktopSection']")).isVisible();
        assertThat(place("System.toolbar").locator("[lsfusion-container='UiDesktop.uiDesktopFirst']")).isVisible();
        assertThat(page.locator(".split-vert, .split-horz")).hasCount(0);

        // ... and fills it - the pin modes are the standard layout's way of laying the navigator and the log out,
        // so a window the component lays out is not given their classes
        for (String window : new String[] {"System.toolbar", "System.log"}) {
            Locator view = place(window).locator(":scope > *");
            assertThat(view).not().containsClass("navbar-popup-over-selected-hover");
            assertEquals(window, box(place(window)), box(view));
        }
        assertThat(place("System.log").locator(":scope > *")).not().containsClass("navbar-always-hide-unpinned");
    }

    // the server gives the windows none of the pin modes' classes - the toolbar's and the log's popping out, the top
    // strip's captions on hover -, the component laying the windows out. Asked with the navigator unpinned (the server's
    // default pins the bar), and switched back after
    @Test
    public void theServerGivesTheWindowsNoPinModeClasses() {
        String mode = pinMode();
        try {
            pinMode(UNPINNED);
            Locator form = open("UiDesktop.uiDesktopSection", "UiDesktop.uiDesktopClasses");
            for (String property : new String[] {"toolbarWindowClass", "logsWindowClass"})
                assertThat(value(form, property)).hasText("bg-body-secondary");
            for (String property : new String[] {"logoWindowClass", "rootWindowClass", "systemWindowClass"})
                assertThat(value(form, property)).not().containsText("navbar-text-on-hover");
            // the vertical navbar's: none either, while without the component the logo and system windows have it
            assertThat(value(form, "uiDesktopVerticalClasses")).not().containsText("navbar-text-on-hover");
            assertThat(value(form, "uiDesktopVerticalStandardClasses")).containsText("navbar-text-on-hover");
        } finally {
            pinMode(mode);
        }
    }

    // a form opened from the navigator is drawn where the component put System.forms
    @Test
    public void aFormOpensWhereTheComponentPutTheFormsWindow() {
        open("UiDesktop.uiDesktopSection", "UiDesktop.uiDesktopFirst");
        assertThat(place("System.forms").locator("[lsfusion-form='UiDesktop.uiDesktopFirst']")).isVisible();
    }

    // a navigator window with nothing to draw is hidden: the component is told so, and the place <Platform> rendered for
    // it hides; once there is something to draw again, it is shown again
    @Test
    public void aWindowWithNothingToDrawIsHidden() {
        Locator desktop = page.locator(".ui-desktop"), toolbar = place("System.toolbar");
        assertThat(desktop).hasAttribute("data-toolbar-hidden", "false");
        assertThat(toolbar).isVisible();

        page.locator("[lsfusion-container='UiDesktop.uiDesktopOther']").click();
        assertThat(desktop).hasAttribute("data-toolbar-hidden", "true");
        assertThat(toolbar).isHidden();

        page.locator("[lsfusion-container='UiDesktop.uiDesktopSection']").click();
        assertThat(desktop).hasAttribute("data-toolbar-hidden", "false");
        assertThat(toolbar).isVisible();
    }

    // a window the component places nowhere is not shown, and lives on: a form opened into it is there, hidden, and is
    // drawn once the component places the window
    @Test
    public void aWindowPlacedNowhereLivesOn() {
        page.locator("[lsfusion-container='UiDesktop.uiDesktopSecondIntoAside']").click();
        Locator second = form("UiDesktop.uiDesktopSecond");
        assertThat(second).hasCount(1);
        assertThat(second).isHidden();

        page.evaluate("() => window.uiDesktop.show('aside')");
        assertThat(place("UiDesktop.uiDesktopAside").locator("[lsfusion-form='UiDesktop.uiDesktopSecond']")).isVisible();
    }

    // the full screen draws System.forms alone, as in the standard layout: the component's page is hidden meanwhile and
    // not taken down, so what the component keeps in its state - here the aside it was asked to show - is there after
    @Test
    public void theFullScreenHidesThePageAndKeepsIt() {
        Locator first = open("UiDesktop.uiDesktopSection", "UiDesktop.uiDesktopFirst");
        page.evaluate("() => window.uiDesktop.show('aside')");
        assertThat(place("UiDesktop.uiDesktopAside")).hasCount(1);

        press("Alt+F11");
        assertThat(page.locator(".ui-desktop")).hasCount(1);
        assertThat(page.locator(".ui-desktop")).isHidden();
        assertThat(first).isVisible();

        press("Alt+F11");
        assertThat(place("System.forms").locator("[lsfusion-form='UiDesktop.uiDesktopFirst']")).isVisible();
        assertThat(place("UiDesktop.uiDesktopAside")).hasCount(1);
    }

    // there is no size of the standard layout to store: leaving the page stores none
    @Test
    public void noWindowSizeIsStored() {
        page.evaluate("() => localStorage.clear()");
        page.reload();
        page.locator(".ui-desktop").waitFor();
        assertEquals(false, page.evaluate("() => Object.keys(localStorage).some(key => key.endsWith('_System.log'))"));
    }

    // a name no window has is said in the place the component gave it
    @Test
    public void aWrongNameIsSaidInThePage() {
        page.evaluate("() => window.uiDesktop.show('wrong')");
        assertThat(page.locator(".ui-desktop-wrong .lsf-view-error")).containsText("'UiDesktop.nope' is not a window");
    }

    // the value a panel property of a form shows
    private static Locator value(Locator form, String property) {
        String name = "[lsfusion-container='PROPERTY(" + property + "())']";
        return form.locator(".panel-renderer-value" + name + ", " + name + " .panel-renderer-value");
    }

    // the place the component gave a window: the host <Platform> rendered, marked with the window's name
    private Locator place(String window) {
        return page.locator(".ui-desktop [data-lsf-sid='" + window + "']");
    }

    private static String box(Locator element) {
        BoundingBox box = element.boundingBox();
        return Math.round(box.x) + "," + Math.round(box.y) + " " + Math.round(box.width) + "x" + Math.round(box.height);
    }
}
