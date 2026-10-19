package lsfusion.tests;

import com.microsoft.playwright.Locator;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

// a group's pivot in the web client: what a group in manual update holds back once it is back from its pivot, and the
// pivot of a group whose design hides the XLS export button
public class UiPivotIT extends UiTest {

    private static final String RENDER_ERROR = "An error occurred rendering the PivotTable results.";
    private static final Pattern PUSH_API = Pattern.compile("Push API|Registration failed - permission denied");

    // a group in manual update switched to its pivot and back to its table before an update there holds its rows back
    // until it is updated, and then nothing: the update brings the rows, the platform's update button is disabled and
    // the grid is not dimmed; once the manual update is off, a change shows at once, the grid not dimmed either
    // (#1886). The count the pivot adds to the group's columns stayed held back once the pivot was gone, and the group
    // pending for good. The server's side is held by the server test forms/ManualUpdate
    @Test
    public void aManualGroupBackFromItsPivotOnThePlatformsToolbar() throws Exception {
        String[] items = seed();
        Locator form = open("UiPivot.uiPivot", "UiPivot.uiPivotManual");
        Locator update = form.locator("button[title='Update']"), grid = form.locator("table.lsf-table"), names = names(form);
        assertThat(update).isEnabled();
        assertThat(grid).hasCSS("filter", "opacity(0.5)");
        assertThat(names).hasCount(0); // manual from the start: nothing read yet

        update.click();
        assertThat(update).isDisabled();
        assertThat(grid).hasCSS("filter", "none");
        assertThat(names).hasText(items);

        form.locator("button[title='Pivot']").click();
        assertThat(form.locator(".pvtUiWrapperDiv")).isVisible();
        assertThat(update).isEnabled(); // the pivot holds its data back

        form.locator("button[title='Table']").click();
        assertThat(grid).hasCSS("filter", "opacity(0.5)"); // back in the table before an update: still held back
        assertThat(update).isEnabled();

        update.click();
        assertThat(update).isDisabled();
        assertThat(grid).hasCSS("filter", "none");
        assertThat(names).hasText(items);

        form.locator("button[title='Manual update']").click();
        assertThat(update).isHidden();
        renamed(form, names, items); // the switch answered
        assertThat(update).isHidden();
        assertThat(grid).hasCSS("filter", "none");
        close(form);
    }

    // the same on the toolbar a React view draws over the platform's grid: once updated, the update brings the rows and
    // the group's node says nothing is held back - updatePending false -, and once the manual update is turned off
    // through its member a change shows at once, the grid not dimmed (#1886)
    @Test
    public void aManualGroupBackFromItsPivotOnAViewsToolbar() throws Exception {
        String[] items = seed();
        Locator form = open("UiPivot.uiPivot", "UiPivot.uiPivotManualHeader");
        Locator state = form.locator(".ui-pivot-state"), grid = form.locator("table.lsf-table"), names = names(form);
        assertThat(state).hasText("manual true, pending true, grid");
        assertThat(names).hasCount(0); // manual from the start: nothing read yet

        text(form, "Update rows").click();
        assertThat(state).hasText("manual true, pending false, grid");
        assertThat(names).hasText(items);

        text(form, "To pivot").click();
        assertThat(form.locator(".pvtUiWrapperDiv")).isVisible();
        assertThat(state).hasText("manual true, pending true, pivot");

        text(form, "To grid").click();
        assertThat(state).hasText("manual true, pending true, grid");

        text(form, "Update rows").click();
        assertThat(state).hasText("manual true, pending false, grid");
        assertThat(grid).hasCSS("filter", "none");
        assertThat(names).hasText(items);

        text(form, "Automatic").click();
        assertThat(state).hasText("manual false, pending false, grid");
        renamed(form, names, items); // the switch answered
        assertThat(state).hasText("manual false, pending false, grid");
        assertThat(grid).hasCSS("filter", "none");
        close(form);
    }

    // the pivot of a group whose design hides the XLS export button, switched to it by the toolbar, is drawn - its
    // rows the items -, with no error of its renderer: filling the table, the pivot disables the export button and
    // enables it again, and it read the button the toolbar made only where it shows it. The fix came with #1884 and
    // #1885, and has no issue of its own
    @Test
    public void thePivotOfAGroupWhoseDesignHidesTheExportButton() throws Exception {
        String[] items = seed();
        List<String> errors = consoleErrors();
        Locator form = open("UiPivot.uiPivot", "UiPivot.uiPivotExportHidden");
        assertThat(names(form)).hasText(items);
        assertThat(form.locator("button[title='Export']")).hasCount(0);
        form.locator("button[title='Pivot']").click();
        assertPivot(form, items);
        assertEquals("the errors the page logged", "", String.join("\n", errors));
        close(form);
    }

    // ... and so is the pivot of such a group opened as a pivot, with its default totals: the count of the items and
    // the sum of their quantities, each in a cell of its own
    @Test
    public void aGroupWhoseDesignHidesTheExportButtonOpenedAsAPivot() throws Exception {
        seed();
        List<String> errors = consoleErrors();
        Locator form = open("UiPivot.uiPivot", "UiPivot.uiPivotExportHiddenPivot");
        Locator pivot = form.locator(".subtotalouterdiv");
        assertThat(cellOf(pivot, "3")).hasCount(1);
        assertThat(cellOf(pivot, "600")).hasCount(1);
        assertThat(form.getByText(RENDER_ERROR)).hasCount(0);
        assertThat(form.locator("button[title='Export']")).hasCount(0);
        assertEquals("the errors the page logged", "", String.join("\n", errors));
        close(form);
    }

    // the control: the pivot of a group with the export button is drawn as well, and its filling disables the button
    // and enables it again
    @Test
    public void thePivotOfAGroupWithTheExportButton() throws Exception {
        String[] items = seed();
        Locator form = open("UiPivot.uiPivot", "UiPivot.uiPivotExport");
        assertThat(names(form)).hasText(items);
        Locator export = form.locator("button[title='Export']");
        // every change of the button's disabled attribute from now on, by the value it had before it
        export.evaluate("button => { window.uiPivotExport = [];"
                + " new MutationObserver(records => records.forEach(record => window.uiPivotExport.push(record.oldValue === null ? 'enabled' : 'disabled')))"
                + "  .observe(button, {attributes: true, attributeFilter: ['disabled'], attributeOldValue: true}); }");
        form.locator("button[title='Pivot']").click();
        assertPivot(form, items);
        assertThat(export).isEnabled();
        // the states the button went through, each once in a row, the last its state now: disabled for each fill
        String states = (String) export.evaluate("button => window.uiPivotExport.concat([button.disabled ? 'disabled' : 'enabled'])"
                + ".filter((state, i, states) => i === 0 || state !== states[i - 1]).join()");
        assertTrue("the states of the export button: " + states, states.matches("enabled(,disabled,enabled)+"));
        close(form);
    }

    // three items under names no other attempt has - what a failed attempt saved stays in the database for its rerun -,
    // shown from now on
    private static String[] seed() throws Exception {
        long id = System.nanoTime();
        String[] items = {"Apple " + id, "Banana " + id, "Cherry " + id};
        exec("UiPivot.uiPivotSeed", items);
        return items;
    }

    // the items renamed by the form's action: the switch to the automatic update, which the client shows before it is
    // answered, is answered before it - the server takes a form's requests in their order -, and the names show at once
    private static void renamed(Locator form, Locator names, String[] items) {
        form.locator("button[lsfusion-container='PROPERTY(uiPivotRename())']").click();
        assertThat(names).hasText(Arrays.stream(items).map(item -> item + " renamed").toArray(String[]::new));
    }

    // the names in the platform's grid, a cell of a row each, in their order
    private static Locator names(Locator form) {
        return form.locator("td[lsfusion-container='PROPERTY(uiPivotName(i))']");
    }

    // the pivot drawn, its rows the items, and no error of its renderer in its place
    private static void assertPivot(Locator form, String[] items) {
        assertThat(form.locator(".subtotalouterdiv th.pvtRowLabel")).hasText(items);
        assertThat(form.getByText(RENDER_ERROR)).hasCount(0);
    }

    // a cell of the pivot showing exactly this
    private static Locator cellOf(Locator pivot, String text) {
        return pivot.locator("td").filter(new Locator.FilterOptions().setHasText(Pattern.compile("^" + text + "$")));
    }

    private static Locator text(Locator scope, String text) {
        return scope.getByText(text, new Locator.GetByTextOptions().setExact(true));
    }

    // the errors the page logs from now on, but the browser's own: chrome has no push api in the context of a test
    private List<String> consoleErrors() {
        List<String> errors = new ArrayList<>();
        page.onConsoleMessage(message -> {
            if (message.type().equals("error") && !PUSH_API.matcher(message.text()).find())
                errors.add(message.text());
        });
        page.onPageError(error -> {
            if (!PUSH_API.matcher(error).find())
                errors.add("page error: " + error);
        });
        return errors;
    }
}
