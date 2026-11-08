package lsfusion.tests;

import com.microsoft.playwright.Locator;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

// System.forms drawn by a React component whose tabs reuse the standard tabs' class names (src/ui/formstabs:
// UiFormsTabs.lsf, UiFormsTabs.js), with the user's setting that hides an inactive tab's close button on. System.forms
// is on every page of its application, so the class runs against a test server of its own, on the suite's web client
public class UiFormsTabsIT extends UiTest {

    private static final String FOLDER = "UiFormsTabs.uiFormsTabs";
    private static final String DATABASE = "lsfusion_test_ui_forms_tabs_" + ProcessHandle.current().pid();
    private static Process server;
    private static int rmiPort;

    @BeforeClass
    public static void startFormsTabsServer() throws Exception {
        int[] ports = TestServer.freePorts(4);
        rmiPort = ports[1];
        server = startServer(DATABASE, ports, TestServer.BASE.resolve("src/ui/formstabs"));
    }

    @AfterClass
    public static void stopFormsTabsServer() throws Exception {
        if (server != null)
            TestServer.stop(server, DATABASE);
    }

    @Override
    protected String main() {
        return "/main?port=" + rmiPort;
    }

    // the setting hides the close button of an inactive standard tab by a class on the window, which is not given: a tab
    // the component draws keeps its close button, the standard tabs' class names notwithstanding
    @Test
    public void anInactiveTabKeepsItsCloseButton() {
        String mode = pinMode();
        try {
            pinMode(ALL_PINNED);
            hideInactiveCloseButtons();
            assertThat(open(FOLDER, "UiFormsTabs.uiFormsTabsFirst")).isVisible();
            assertThat(open(FOLDER, "UiFormsTabs.uiFormsTabsSecond")).isVisible();
            settled();

            Locator inactive = page.locator(".ui-forms-tabs .nav-link:not(.active)");
            assertThat(inactive).hasCount(1);
            assertThat(inactive.locator(".tab-close-button")).isVisible();
            assertThat(page.locator(".dont-show-close-button")).hasCount(0);
        } finally {
            pinMode(mode);
        }
    }

    // ... by the server: the window's class has none of it, while the mobile web client, which draws the standard tabs
    // whatever the window's CUSTOM, gets it (the standard layout's is UiWindowsLayoutIT's control)
    @Test
    public void theServerGivesOnlyStandardTabsTheCloseButtonClass() {
        hideInactiveCloseButtons();
        Locator form = open(FOLDER, "UiFormsTabs.uiFormsTabsClasses");
        assertThat(value(form, "formsWindowClass")).hasText("bg-body-tertiary");
        assertThat(value(form, "uiFormsTabsMobileClass")).containsText("dont-show-close-button");
    }

    private void hideInactiveCloseButtons() {
        trackRequests();
        page.waitForResponse(response -> carries(response.request().postData(), "ExecuteNavigatorAction"),
                () -> page.locator("[lsfusion-container='UiFormsTabs.uiFormsTabsHideInactiveClose']").click()).finished();
        settled();
    }

    private static Locator value(Locator form, String property) {
        String name = "[lsfusion-container='PROPERTY(" + property + "())']";
        return form.locator(".panel-renderer-value" + name + ", " + name + " .panel-renderer-value");
    }
}
