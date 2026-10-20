package lsfusion.tests;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.PlaywrightException;
import com.microsoft.playwright.options.WaitForSelectorState;
import org.junit.Test;

import java.util.regex.Pattern;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.Assert.assertEquals;

// the platform's toolbar and filters over the rows a React view draws (UiReactRowsChrome.lsf, its views
// UiReactRowsChrome.js): left in the group's box, and placed back in the view with custom = NULL - over the rows alone,
// and over the rows and the conditions the view draws
public class UiReactRowsChromeIT extends UiTest {

    private static final String FOLDER = "UiReactRowsChrome.uiRrc";

    // over the rows a view draws, the platform's toolbar has the buttons that need no table of its own - the filter
    // button, the count, the export, the manual update -, not the view switch, the settings or a column's sum; the
    // count is of the rows the view draws
    @Test
    public void thePlatformsToolbarOverTheRowsAViewDraws() throws Exception {
        String[] items = seed();
        Locator form = open(FOLDER, "UiReactRowsChrome.uiRrcCards");
        assertThat(rows(form)).hasText(items);
        for (String title : new String[] {"Show filter controls", "Number of Entries", "Export", "Manual update"})
            assertThat(button(form, title)).isVisible();
        for (String title : new String[] {"Table", "Pivot", "Grid Preferences", "Calculate Sum"})
            assertThat(button(form, title)).hasCount(0);
        button(form, "Number of Entries").click();
        popup("Number of Entries: 2");
        close(form);
    }

    // the platform's filter panel over the rows a view draws: the filter button starts a condition on an empty panel,
    // on the first column, with the value of the row the view made current, and the rows the view draws follow what the
    // panel states; the panel's add starts one so too, and its reset takes them off, hiding the controls
    @Test
    public void thePlatformsFilterPanelOverTheRowsAViewDraws() throws Exception {
        String[] items = seed();
        Locator form = open(FOLDER, "UiReactRowsChrome.uiRrcCards");
        Locator rows = rows(form), conditions = form.locator(".filter:visible");
        assertThat(rows).hasText(items);
        button(form, "Show filter controls").click();
        assertThat(button(form, "Add filter")).isVisible();
        assertThat(button(form, "Reset all filters")).isVisible();
        assertThat(conditions).hasCount(1);
        Locator value = conditions.locator("input");
        assertThat(value).hasValue(items[0]);
        press(value, "Enter");
        assertThat(rows).hasText(new String[] {items[0]});
        button(form, "Reset all filters").click();
        assertThat(rows).hasText(items);
        assertThat(conditions).hasCount(0);

        rows.nth(1).click();
        assertThat(rows.nth(1)).hasClass(Pattern.compile("ui-rrc-current"));
        button(form, "Show filter controls").click(); // the reset hid them, as on the platform's grid
        assertThat(conditions.locator("input")).hasValue(items[1]);
        button(form, "Add filter").click();
        assertThat(conditions).hasCount(2);
        // only the condition being edited has an input
        assertThat(form.locator(".filter input:visible")).hasValue(items[1]);
        button(form, "Reset all filters").click();
        assertThat(rows).hasText(items);
        assertThat(button(form, "Add filter")).isHidden();
        close(form);
    }

    // the platform's manual update over the rows a view draws: a change held back lights the update - the view's rows
    // get no values meanwhile, as the platform's grid shows none - and the update brings it; back in automatic update,
    // a change comes at once
    @Test
    public void thePlatformsManualUpdateOverTheRowsAViewDraws() throws Exception {
        String[] items = seed();
        Locator form = open(FOLDER, "UiReactRowsChrome.uiRrcCards");
        Locator update = button(form, "Update");
        assertThat(rows(form)).hasText(items);
        button(form, "Manual update").click();
        assertThat(update).isDisabled();
        rename(form);
        assertThat(update).isEnabled();
        waitFor("data.i.list.length === 2 && data.i.list.every(row => row.name.value === null)");
        update.click();
        assertThat(rows(form)).hasText(new String[] {items[0] + "!", items[1]});
        button(form, "Manual update").click();
        assertThat(update).isHidden();
        rename(form);
        assertThat(rows(form)).hasText(new String[] {items[0] + "!!", items[1]});
        close(form);
    }

    // a view drawing the whole group places the platform's toolbar and filter box back: the toolbar has the buttons
    // that need no table, its filter button shows the controls in the placed box, and the view's rows follow the panel
    @Test
    public void aToolbarAndAFilterBoxPlacedBack() throws Exception {
        String[] items = seed();
        Locator form = open(FOLDER, "UiReactRowsChrome.uiRrcPlaced");
        Locator toolbar = host(form, "TOOLBARSYSTEM(i)"), filters = host(form, "FILTERBOX(i)");
        assertThat(rows(form)).hasText(items);
        for (String title : new String[] {"Show filter controls", "Number of Entries", "Export", "Manual update"})
            assertThat(button(toolbar, title)).isVisible();
        assertThat(button(filters, "Add filter")).isHidden();
        button(toolbar, "Show filter controls").click();
        assertThat(button(filters, "Add filter")).isVisible();
        Locator conditions = filters.locator(".filter:visible");
        assertThat(conditions).hasCount(1);
        press(conditions.locator("input"), "Enter");
        assertThat(rows(form)).hasText(new String[] {items[0]});
        button(filters, "Reset all filters").click();
        assertThat(rows(form)).hasText(items);
        close(form);
    }

    // a view drawing the whole group but the filter box, placed back: the platform draws the conditions, so the view
    // gets none of them, and no toolbar but the view's - whose filter button's member shows the platform's controls in
    // the placed box, with a condition to fill in; the panel's reset hides them, and the view's toolbar says so
    @Test
    public void aFilterBoxPlacedBackUnderTheViewsToolbar() throws Exception {
        String[] items = seed();
        Locator form = open(FOLDER, "UiReactRowsChrome.uiRrcFilterBox");
        Locator filters = host(form, "FILTERBOX(i)");
        assertThat(rows(form)).hasText(items);
        assertEquals("false,false,false", eval("['filters' in data.i, 'filters' in controller.i, data.i.filterControlsVisible].join()"));
        assertThat(button(form, "Number of Entries")).hasCount(0);
        assertThat(button(filters, "Add filter")).isHidden();

        call("controller.i.filterControlsVisible.change(true)");
        assertThat(button(filters, "Add filter")).isVisible();
        Locator conditions = filters.locator(".filter:visible");
        assertThat(conditions).hasCount(1);
        press(conditions.locator("input"), "Enter");
        assertThat(rows(form)).hasText(new String[] {items[0]});
        button(filters, "Reset all filters").click();
        assertThat(rows(form)).hasText(items);
        assertThat(button(filters, "Add filter")).isHidden();
        waitFor("data.i.filterControlsVisible === false");
        close(form);
    }

    // a view drawing the rows and the conditions places the platform's toolbar and filter controls back: the filter
    // button shows the controls - an add and a reset, no apply, and no condition started, as the view draws them -; the
    // add puts in the condition of the first column with the value of the row the view made current, at once, in the
    // place of the one on that column - one the view states -, its comparison kept; the reset takes them off, leaving
    // the controls shown
    @Test
    public void thePlatformsControlsOverTheConditionsTheViewDraws() throws Exception {
        String[] items = seed();
        Locator form = open(FOLDER, "UiReactRowsChrome.uiRrcControls");
        Locator toolbar = host(form, "TOOLBARSYSTEM(i)"), controls = host(form, "FILTERCONTROLS(i)");
        assertThat(rows(form)).hasText(items);
        for (String title : new String[] {"Show filter controls", "Number of Entries", "Export", "Manual update"})
            assertThat(button(toolbar, title)).isVisible();
        assertThat(button(controls, "Add filter")).isHidden();

        button(toolbar, "Show filter controls").click();
        assertThat(button(controls, "Add filter")).isVisible();
        assertThat(button(controls, "Reset all filters")).isVisible();
        assertThat(button(controls, "Apply filters")).isHidden();
        // asked after the show, the count would be of a condition it started
        button(toolbar, "Number of Entries").click();
        popup("Number of Entries: 2");
        waitFor("data.i.filters.length === 0");

        rows(form).nth(1).click();
        assertThat(rows(form).nth(1)).hasClass(Pattern.compile("ui-rrc-current"));
        button(controls, "Add filter").click();
        waitFor("conditions() === 'name = " + items[1] + "'");
        assertThat(rows(form)).hasText(new String[] {items[1]});
        String id = items[0].substring("Apple".length()); // in both names
        call("controller.i.filters.change({property: 'name', compare: '_', value: '" + id + "'})");
        waitFor("conditions() === 'name _ " + id + "'");
        assertThat(rows(form)).hasText(items);
        rows(form).nth(1).click();
        assertThat(rows(form).nth(1)).hasClass(Pattern.compile("ui-rrc-current"));
        button(controls, "Add filter").click();
        waitFor("conditions() === 'name _ " + items[1] + "'"); // the comparison of the one there
        assertThat(rows(form)).hasText(new String[] {items[1]});
        button(controls, "Reset all filters").click();
        waitFor("data.i.filters.length === 0");
        assertThat(rows(form)).hasText(items);
        assertThat(button(controls, "Add filter")).isVisible();
        button(toolbar, "Hide filter controls").click();
        assertThat(button(controls, "Add filter")).isHidden();
        close(form);
    }

    // two items under names no other attempt has - what a failed attempt saved stays in the database for its rerun -,
    // the dearer one first by name
    private static String[] seed() throws Exception {
        long id = System.nanoTime();
        String[] items = {"Apple" + id, "Banana" + id};
        exec("UiReactRowsChrome.uiRrcSeed", items);
        return items;
    }

    // the rows the view draws, a name each
    private static Locator rows(Locator form) {
        return form.locator(".ui-rrc-row");
    }

    // the host the view places a component the platform draws in
    private static Locator host(Locator form, String component) {
        return form.locator(".lsf-platform[data-lsf-sid='" + component + "']");
    }

    // a button of the platform's toolbar or filter controls, by its title
    private static Locator button(Locator scope, String title) {
        return scope.getByTitle(title, new Locator.GetByTitleOptions().setExact(true));
    }

    // the form's own action: the dearer item renamed and the change applied, which a manual update holds back
    private static void rename(Locator form) {
        form.locator("button[lsfusion-container='PROPERTY(uiRrcRename())']").click();
    }

    // what the popup of the count shows; the popup let go then - shown, it takes the focus, and hides as it loses it,
    // as to a click on nothing
    private void popup(String text) {
        Locator popup = page.locator("[data-tippy-root]");
        assertThat(popup).hasText(text);
        page.waitForFunction("() => !!document.activeElement && !!document.activeElement.closest('[data-tippy-root]')");
        page.evaluate("() => document.activeElement.blur()");
        assertThat(popup).hasCount(0);
    }

    // a key into a condition's editor once no request is in flight: the client drops the keys typed meanwhile
    private void press(Locator editor, String key) {
        page.locator(".lsf-blocking").waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.HIDDEN));
        editor.press(key);
    }

    // what the view was last drawn with, its data and its controller, as a script names them, and the conditions it is
    // given, each as `property compare value`
    private static final String VIEW = "const view = window.uiRrcView, data = view && view.data, controller = view && view.controller;"
            + " const conditions = () => data.i.filters.map(f => f.property + ' ' + f.compare + ' ' + f.value).join('; ');";

    // until a condition holds of what the view is drawn with, failing with what it is drawn with then
    private void waitFor(String condition) {
        try {
            page.waitForFunction("() => { " + VIEW + " try { return !!data && !!(" + condition + "); } catch (e) { return false; } }",
                    null, new Page.WaitForFunctionOptions().setTimeout(10_000));
        } catch (PlaywrightException e) {
            throw new AssertionError("never " + condition + " - the view is drawn with " + page.evaluate("() => JSON.stringify(window.uiRrcView && window.uiRrcView.data)"), e);
        }
    }

    // what an expression over what the view is drawn with comes to, as a string
    private String eval(String expression) {
        return (String) page.evaluate("async () => { " + VIEW + " return String(await (" + expression + ")); }");
    }

    // a call of the view's controller, made as the view's own code makes it
    private void call(String call) {
        page.evaluate("() => { " + VIEW + " " + call + "; }");
    }
}
