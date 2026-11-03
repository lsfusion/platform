package lsfusion.tests;

import com.microsoft.playwright.Locator;
import org.junit.Test;

import java.util.List;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.Assert.assertEquals;

// window.lsfusion.useVisible(): the platform hides a React view without taking it down - in a tab not selected, in a
// collapsed container, in a popup container not open, under a SHOWIF, in a form in a background tab -, and the hook
// says whether the view is on screen, drawing it again when that changes, so that work of the view's own - here a timer
// - is done only while it is visible. Each view hands on what the hook says and its timer's ticks (UiReactVisible.js)
public class UiReactVisibleIT extends UiTest {

    // as the form opens, the control - a view the form never hides - is visible and its timer runs, while the views the
    // form hides are mounted, once, have been told nothing but that they are not visible, and their timers have not run
    @Test
    public void theHiddenViewsAreMountedAndNotVisible() throws Exception {
        List<String> errors = consoleErrors();
        Locator form = openViews();
        for (String view : new String[] {"second", "details", "pop", "gated"})
            expect(view + ".history.join() + ' ' + " + view + ".ticks + ' ' + " + view + ".running", "false 0 false");
        for (String view : new String[] {"second", "details", "pop", "gated", "plain"})
            mountedOnce(view);
        close(form);
        assertEquals("the errors the page logged", "", String.join("\n", errors));
    }

    // a view in a tab not selected is visible once the tab is selected, and not once another one is
    @Test
    public void aTabNotSelected() throws Exception {
        Locator form = openViews();
        roundTrip("second", () -> text(form, "Second tab").click(), () -> text(form, "First tab").click());
        close(form);
    }

    // a view in a collapsed container is visible once the container is expanded, and not once it is collapsed again
    @Test
    public void aCollapsedContainer() throws Exception {
        Locator form = openViews();
        roundTrip("details", () -> text(form, "Details").click(), () -> text(form, "Details").click());
        close(form);
    }

    // a view in a popup container is visible once its popup is open, and not once it is closed by a click outside it.
    // The popup takes the focus once it is shown - its fade-in over - and closes as it loses it: a click before that
    // leaves it open
    @Test
    public void aPopupContainerNotOpen() throws Exception {
        Locator form = openViews();
        roundTrip("pop", () -> {
            form.locator(".popup-panel-button").filter(new Locator.FilterOptions().setHasText("Pop")).click();
            page.waitForFunction("() => !!document.activeElement && !!document.activeElement.closest('[data-tippy-root]')");
        }, () -> form.locator(".ui-react-visible-plain").click());
        close(form);
    }

    // a view under a SHOWIF is visible once the SHOWIF shows its container, and not once it hides it again
    @Test
    public void aShowIf() throws Exception {
        Locator form = openViews();
        Locator toggle = form.locator("button[lsfusion-container='PROPERTY(uiReactVisibleToggle())']");
        roundTrip("gated", toggle::click, toggle::click);
        close(form);
    }

    // the control is not visible while its form is in a background tab - another form opened beside it -, its timer
    // standing still, and is visible again once the other form is closed
    @Test
    public void aFormInABackgroundTab() throws Exception {
        Locator form = openViews();
        form.locator("button[lsfusion-container='PROPERTY(uiReactVisibleOpenOther())']").click();
        Locator other = form("UiReactVisible.uiReactVisibleOther");
        assertThat(other).isVisible();
        hidden("plain");
        close(other);
        views.waitFor("plain.visible && plain.running");
        mountedOnce("plain");
        close(form);
    }

    // the views' form opened: the control visible and its timer running
    private Locator openViews() {
        Locator form = open("UiReactVisible.uiReactVisible", "UiReactVisible.uiReactVisibleViews");
        views.waitFor("plain.visible && plain.ticks > 0");
        return form;
    }

    // shown, the view is visible and its timer runs; hidden again, it is not, and its timer stands still - the one
    // instance mounted as the form opened all the while
    private void roundTrip(String view, Runnable show, Runnable hide) {
        show.run();
        int ticks = ticks(view);
        views.waitFor(view + ".visible && " + view + ".ticks > " + ticks);
        hide.run();
        hidden(view);
        expect(view + ".history.join()", "false,true,false");
        mountedOnce(view);
    }

    // the view mounted once and never unmounted: the instance the form opened with
    private void mountedOnce(String view) {
        expect(view + ".mounts + ' ' + " + view + ".unmounts", "1 0");
    }

    // the view not visible and its timer stopped: no tick comes in the time several would
    private void hidden(String view) {
        views.waitFor("!" + view + ".visible && !" + view + ".running");
        int ticks = ticks(view);
        page.waitForTimeout(800);
        assertEquals("the ticks of " + view + "'s timer while it is hidden", ticks, ticks(view));
    }

    // what the views hand on, window.uiReactVisible, each view under its name
    private static final String VIEWS = "const { second, details, pop, gated, plain } = window.uiReactVisible || {};";
    private final View views = new View(VIEWS, "true", "the views hand on ", "window.uiReactVisible");

    private void expect(String expression, String expected) {
        assertEquals(expression, expected, views.value("String(" + expression + ")"));
    }

    private int ticks(String view) {
        return ((Number) views.value(view + ".ticks")).intValue();
    }
}
