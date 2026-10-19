package lsfusion.tests;

import com.microsoft.playwright.Locator;
import org.junit.Test;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

// an object group shown as its current object alone - in POPUP, TOOLBAR or PANEL view - in the web client: the form
// opens, and the group's view is the default view of its properties, which the client draws there, the value on the
// page. Where the server places them, and the box of a group in POPUP view, are held by the server test
// forms/GroupViews
public class UiGroupViewIT extends UiTest {

    // a form with a group in POPUP view opens (#1890): a property that names PANEL is drawn in the group's panel, and
    // the group's own property is not but in the popup container its popup button shows (#1891)
    @Test
    public void aGroupInPopupView() throws Exception {
        String[] item = seed();
        Locator form = open("UiGroupView.uiGroupView", "UiGroupView.uiGroupViewPopup");
        shown(form.locator("[lsfusion-container='PANEL(i)']"), "uiGroupViewCode(i)", item[1]);
        assertThat(property(form.locator("[lsfusion-container='PANEL(i)']"), "uiGroupViewName(i)")).isHidden();
        form.locator("[lsfusion-container='TOOLBARLEFT(i)'] .popup-panel-button").click();
        shown(page.locator("[data-tippy-root] [lsfusion-container='POPUP(i)']"), "uiGroupViewName(i)", item[0]);
        close(form);
    }

    // a group in TOOLBAR view has its property drawn for its current object in its toolbar (#1891)
    @Test
    public void aGroupInToolbarView() throws Exception {
        String[] item = seed();
        Locator form = open("UiGroupView.uiGroupView", "UiGroupView.uiGroupViewToolbar");
        shown(form.locator("[lsfusion-container='TOOLBAR(i)']"), "uiGroupViewName(i)", item[0]);
        close(form);
    }

    // the control: a group in PANEL view, which always opened, has its property drawn for its current object in its
    // panel
    @Test
    public void aGroupInPanelView() throws Exception {
        String[] item = seed();
        Locator form = open("UiGroupView.uiGroupView", "UiGroupView.uiGroupViewPanel");
        shown(form.locator("[lsfusion-container='PANEL(i)']"), "uiGroupViewName(i)", item[0]);
        close(form);
    }

    // a property drawn in a container, on the page, with its value
    private static void shown(Locator container, String property, String value) {
        Locator drawn = property(container, property);
        assertThat(drawn).isVisible();
        assertThat(drawn).containsText(value);
    }

    private static Locator property(Locator container, String property) {
        return container.locator("[lsfusion-container='PROPERTY(" + property + ")']");
    }

    // an item under a name and a code no other attempt has - what a failed attempt saved stays in the database for its
    // rerun -, the group's current object from now on
    private static String[] seed() throws Exception {
        long id = System.nanoTime();
        String[] item = {"Apple " + id, "A" + id};
        exec("UiGroupView.uiGroupViewSeed", item);
        return item;
    }
}
