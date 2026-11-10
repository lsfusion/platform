package lsfusion.tests;

import com.microsoft.playwright.APIResponse;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Route;
import com.microsoft.playwright.assertions.LocatorAssertions;
import com.microsoft.playwright.options.ReducedMotion;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.regex.Pattern;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

// a group's pivot in the web client: what a group in manual update holds back once it is back from its pivot, the pivot
// of a group whose design hides the XLS export button, a pivot switched on before a row change is answered, and a
// PIVOT LAST quantity
public class UiPivotIT extends UiTest {

    private static final String RENDER_ERROR = "An error occurred rendering the PivotTable results.";

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

    // a group opened as its pivot with a PIVOT LAST quantity is drawn with the quantity of the item last by name, 300,
    // not the sum: the pivot failed with a TypeError on its first drawing, while its group is pending, which has no
    // values of the order yet, then on the names of the order taken as numbers, the quantity's class, and on the
    // order's system column, which has no property to format its value with (#1968)
    @Test
    public void aPivotWithALastMeasureIsDrawn() throws Exception {
        lastMeasureIs("UiPivot.uiPivotLast", "300");
    }

    // a PIVOT LAST quantity by a number takes the row of the greatest one, 10, not of the greatest text, 9: the order's
    // values are converted by their own class, without which a boxed Integer is compared in javascript as its text
    // (#1968). The three rows fit a page, so the server does not group them and the client picks the last row itself,
    // which is what this holds
    @Test
    public void aLastMeasureByANumberTakesTheGreatest() throws Exception {
        lastMeasureIs("UiPivot.uiPivotLastRank", "200");
    }

    // ... and by a date-time, the latest, not the one of the greatest text, which begins with the day of the week
    // (#1968)
    @Test
    public void aLastMeasureByADateTimeTakesTheLatest() throws Exception {
        lastMeasureIs("UiPivot.uiPivotLastDateTime", "200");
    }

    // the pivot of the form shows the count of the three items, 3, and the quantity of the item last in the order, and no
    // other quantity
    private void lastMeasureIs(String formName, String quantity) throws Exception {
        seed();
        List<String> errors = consoleErrors();
        Locator form = open("UiPivot.uiPivot", formName);
        Locator pivot = form.locator(".subtotalouterdiv");
        assertThat(cellOf(pivot, "3")).hasCount(1);
        assertThat(cellOf(pivot, quantity)).hasCount(1);
        for (String other : new String[]{"100", "200", "300", "600"})
            if (!other.equals(quantity))
                assertThat(cellOf(pivot, other)).hasCount(0);
        assertThat(form.getByText(RENDER_ERROR)).hasCount(0);
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

    // a pivot switched on while the row change a click made is unanswered gets that answer before its own, and must
    // still open with the selected column in its rows, not only the totals
    @Test
    public void aPivotSwitchedOnBeforeTheRowChangeIsAnsweredDrawsItsRows() throws Exception {
        Page original = page;
        BrowserContext context = original.context().browser().newContext(new Browser.NewContextOptions()
                .setLocale("en-US").setTimezoneId("UTC").setViewportSize(1280, 800).setReducedMotion(ReducedMotion.REDUCE));
        page = context.newPage();
        try {
            page.clock().install();
            page.navigate(original.url());
            page.locator("[lsfusion-container='System.root']").waitFor();
            page.evaluate(REQUEST_PROBE);

            String[] items = seed();
            Locator form = open("UiPivot.uiPivot", "UiPivot.uiPivotAutomatic");
            Locator names = names(form);
            assertThat(names).hasText(items);

            Route[] row = {null}, own = {null};
            APIResponse[] rowAnswer = {null};
            page.route("**/main/dispatch", route -> {
                String body = route.request().postData();
                if (isAction(body, "ChangeGroupObject")) {
                    row[0] = route;
                    rowAnswer[0] = route.fetch();
                } else if (isAction(body, "ChangeMode") && own[0] == null) {
                    own[0] = route;
                } else
                    route.resume();
            });
            page.clock().pauseAt(((Number) page.evaluate("() => Date.now()")).longValue() + 1000); // ahead of the time the call takes
            names.nth(2).click(); // the first row is current when the form opens
            page.clock().runFor(50); // the row change is sent 50 ms after the click
            until(() -> row[0] != null, "the row change sent");
            form.locator("button[title='Pivot']").click();
            until(() -> own[0] != null, "the pivot's request sent");
            row[0].fulfill(new Route.FulfillOptions().setResponse(rowAnswer[0]));
            waitApplied("ChangeGroupObject");
            page.clock().runFor(5);
            own[0].fulfill(new Route.FulfillOptions().setResponse(own[0].fetch()));
            waitApplied("ChangeMode");
            for (int step = 0; step < 8; step++) {
                page.clock().runFor(5);
                waitApplied("ChangeMode");
            }
            page.clock().resume();
            page.unroute("**/main/dispatch");

            assertThat(form.locator(".pvtTable.bodytable .pvtRowLabel")).hasText(items,
                    new LocatorAssertions.HasTextOptions().setTimeout(15_000));
        } finally {
            context.close();
            page = original;
        }
    }

    private void until(BooleanSupplier condition, String what) {
        long deadline = System.currentTimeMillis() + 20_000;
        while (!condition.getAsBoolean()) {
            if (System.currentTimeMillis() > deadline)
                throw new AssertionError("not " + what + ": " + page.evaluate("() => JSON.stringify(window.uiPivotRequests)"));
            page.waitForTimeout(20);
        }
    }

    // every request of the action sent by now answered and applied by the client
    private void waitApplied(String action) {
        until(() -> (Boolean) page.evaluate("a => window.uiPivotRequests.filter(e => e[0] === 'send' && e[1] === a).length"
                        + " === window.uiPivotRequests.filter(e => e[0] === 'applied' && e[1] === a).length", action),
                "every " + action + " applied");
    }

    private static boolean isAction(String body, String action) {
        return body != null && body.contains("." + action + "/");
    }

    // the readystatechange listener runs after the client's own, once it has applied the answer
    private static final String REQUEST_PROBE = "() => { window.uiPivotRequests = [];"
            + " const send = XMLHttpRequest.prototype.send;"
            + " XMLHttpRequest.prototype.send = function(body) {"
            + "   const action = (String(body).match(/remote\\.action\\.[a-z.]*\\.([A-Za-z]+)\\//) || [])[1];"
            + "   if (action === 'ChangeGroupObject' || action === 'ChangeMode') {"
            + "     window.uiPivotRequests.push(['send', action]);"
            + "     this.addEventListener('readystatechange', () => {"
            + "       if (this.readyState === 4) window.uiPivotRequests.push(['applied', action]); }); }"
            + "   return send.apply(this, arguments); }; }";

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
}
