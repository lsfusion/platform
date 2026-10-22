package lsfusion.tests;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.PlaywrightException;
import org.junit.Test;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.Assert.assertEquals;

// a property's selection under a React view (UiSelect.lsf, its views in web/init/uiSelect.jsx): who draws it, and what a
// pick through the view changes
public class UiSelectIT extends UiTest {

    // a SELECT with NOCUSTOM is drawn by the platform inside a React view, which places it - in the panel, per row -, and
    // picks as the platform's element picks; a render function given in CUSTOM draws a SELECT, given the options as that
    // element is
    @Test
    public void aSelectionThePlatformDrawsInAReactView() throws Exception {
        String[] orders = seed();
        Locator form = open("UiSelect.uiSelect", "UiSelect.uiSelectPlaced");
        assertThat(form.locator(".ui-select-panel select option:checked")).hasText("New"); // the dropdown
        Locator rows = form.locator(".ui-select-row");
        assertThat(rows).hasCount(2);
        for (int i = 0; i < 2; i++)
            for (String status : new String[] {"New", "Done", "Lost"})
                assertThat(text(rows.nth(i), status)).isVisible();
        click(rows.nth(1), "Done");
        assertThat(rows.nth(1).getByLabel("Done", new Locator.GetByLabelOptions().setExact(true))).isChecked();

        assertThat(text(form, "fn New")).hasAttribute("aria-pressed", "true");
        assertThat(text(form, "fn Lost")).hasAttribute("aria-pressed", "false");
        click(form, "fn Lost");
        assertThat(text(form, "fn Lost")).hasAttribute("aria-pressed", "true");

        save(form);
        check("UiSelect.uiSelectCheckStatus", orders[1], "Done");
        check("UiSelect.uiSelectCheckStatus", orders[0], "Lost");
        close(form);
    }

    // a pick of one value through the view: an option, its value, null to clear it; an explicit INPUT with a list is
    // given the option's view. What SELECT adds is one field, its list alone here - neither of several values nor one that
    // has to be there, and no element: how it is drawn is the view's own
    @Test
    public void aPickOfOneValue() throws Exception {
        String[] orders = seed();
        Locator form = open("UiSelect.uiSelect", "UiSelect.uiSelectPicked");
        waitFor("data.o.panelStatus.value === 'New'");
        expect("Object.keys(data.o.panelStatus.select).join()", "list");
        click(form, "ps Done");
        waitFor("data.o.panelStatus.value === 'Done' && picked(data.o.panelStatus) === 'Done' && data.o.list[0].uiSelectNameStatus.value === 'Done'");
        click(form, "ps by value Lost");
        waitFor("data.o.panelStatus.value === 'Lost' && picked(data.o.panelStatus) === 'Lost'");
        click(form, "ps clear");
        waitFor("data.o.panelStatus.value === null && picked(data.o.panelStatus) === '' && data.o.list[0].uiSelectNameStatus.value === null");
        click(form, "is Done");
        waitFor("data.o.inputStatus.value === 'Done' && data.o.panelStatus.value === 'Done'");
        save(form);
        check("UiSelect.uiSelectCheckStatus", orders[0], "Done");
        close(form);
    }

    // a pick of several values through the view: the options checked from now on - one more, then one more and one less
    // in one call, by their value as the property has it, the values joined by its separator, which the view is given
    // too, with all the options
    @Test
    public void aPickOfSeveralValues() throws Exception {
        String[] orders = seed();
        Locator form = open("UiSelect.uiSelect", "UiSelect.uiSelectPicked");
        waitFor("picked(data.o.panelTags) === 'red' && data.o.panelTags.value === 'red'");
        expect("JSON.stringify([data.o.panelTags.select.multi, data.o.panelTags.select.separator, data.o.panelTags.select.list.length])", "[true,\", \",3]");
        click(form, "pt green");
        waitFor("picked(data.o.panelTags) === 'green,red' && data.o.panelTags.value === 'green, red' && picked(data.o.list[0].uiSelectTags) === 'green,red'");
        click(form, "pt value blue, green"); // blue checked and red unchecked, in one batch
        waitFor("picked(data.o.panelTags) === 'blue,green' && data.o.panelTags.value === 'blue, green'");
        save(form);
        check("UiSelect.uiSelectCheckTags", orders[0], "blue, green");
        close(form);
    }

    // a pick in a row through the view is that row's, as the selection is told on each row's cell
    @Test
    public void aPickInARow() throws Exception {
        String[] orders = seed();
        Locator form = open("UiSelect.uiSelect", "UiSelect.uiSelectPicked");
        waitFor("data.o.list.length === 2");
        expect("[!!data.o.list[1].uiSelectNameStatus.select.multi, data.o.list[1].uiSelectTags.select.multi].join()", "false,true");
        click(form, "r2 Lost");
        waitFor("data.o.list[1].uiSelectNameStatus.value === 'Lost' && data.o.list[0].uiSelectNameStatus.value === 'New'");
        click(form, "r2 tag blue");
        waitFor("picked(data.o.list[1].uiSelectTags) === 'blue' && picked(data.o.list[0].uiSelectTags) === 'red'");
        save(form);
        check("UiSelect.uiSelectCheckStatus", orders[1], "Lost");
        check("UiSelect.uiSelectCheckTags", orders[1], "blue");
        close(form);
    }

    // two orders under names no other attempt has - what a failed attempt saved stays in the database for its rerun -:
    // the first New and red, the second with no status and no tag
    private static String[] seed() throws Exception {
        String[] orders = {"1-" + System.nanoTime(), "2-" + System.nanoTime()};
        exec("UiSelect.uiSelectSeed", orders);
        return orders;
    }

    // what the view is given (window.uiSelectData, set as it draws) as `data`, and picked(entry) - the values of the
    // options of its `select` list that are picked, sorted, joined
    private static final String DATA = "const data = window.uiSelectData;"
            + " const picked = entry => entry.select.list.filter(o => o.selected).map(o => o.value).sort().join();";

    // until a condition holds of what the view is given and nothing of it is loading - the server's answer to a pick in -,
    // failing with what it is given then
    private void waitFor(String condition) {
        try {
            page.waitForFunction("() => { " + DATA + " try { return !!data && !JSON.stringify(data).includes('\"loading\"') && (" + condition + "); } catch (e) { return false; } }",
                    null, new Page.WaitForFunctionOptions().setTimeout(10_000));
        } catch (PlaywrightException e) {
            throw new AssertionError("never " + condition + " - the view is given " + page.evaluate("() => JSON.stringify(window.uiSelectData)"), e);
        }
    }

    private void expect(String expression, String expected) {
        assertEquals(expression, expected, page.evaluate("() => { " + DATA + " return " + expression + "; }"));
    }

    // a button, a check box or a label of the view, by its text
    private static Locator text(Locator scope, String text) {
        return scope.getByText(text, new Locator.GetByTextOptions().setExact(true));
    }

    private static void click(Locator scope, String text) {
        text(scope, text).click();
    }

    private static void save(Locator form) {
        form.locator("button[lsfusion-container='PROPERTY(formApply())']").click();
    }
}
