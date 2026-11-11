package lsfusion.tests;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import org.junit.Test;

import java.util.regex.Pattern;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.Assert.assertEquals;

// what a custom view's controller does (UiCustomView.lsf, its views in UiCustomView.js): the places of a React view
// that can hold no view - one outside the page, a second one of a row's editor -, a value a React view gives a member,
// the calls to the server a React view makes through the form's controller, a custom cell renderer's controller, the
// React components a property's and a group's CUSTOM name, and the controller an INTERNAL CLIENT action of a form and
// of the navigator gives the JS function it runs
public class UiCustomViewIT extends UiTest {

    private static final String FOLDER = "UiCustomView.uiCustomView";

    // the place a React view renders first for a child the platform draws is in a node it never puts in the page: once
    // the render is over, the place gives the child up, saying so on the console, and the place in the page takes it
    // (#1756)
    @Test
    public void aPlaceOutsideThePageGivesItsChildUp() throws Exception {
        Locator form = open(FOLDER, "UiCustomView.uiCustomViewStray");
        Locator live = form.locator(".ui-custom-view-live");
        assertThat(live.locator("[lsfusion-container='PROPERTY(uiCustomViewNote())']")).isVisible();
        assertThat(live.locator(".lsf-view-error")).hasCount(0);
        close(form);
    }

    // two places of the platform's editor of one row: the first keeps it and the second says so, and once the view
    // takes the first one away the second gets the editor (#1754)
    @Test
    public void theSecondPlaceOfARowsEditorTakesItOnceTheFirstGoes() throws Exception {
        String[] items = seed();
        Locator form = open(FOLDER, "UiCustomView.uiCustomViewRows");
        Locator row = form.locator(".ui-custom-view-row").filter(new Locator.FilterOptions().setHasText(items[0]));
        Locator first = row.locator(".ui-custom-view-first"), second = row.locator(".ui-custom-view-second");
        assertThat(first).hasText("1");
        place("twice");
        assertThat(second.locator(".lsf-view-error"))
                .hasText("lsFusion: 'i.uiCustomViewQuantity' is already placed by another <Platform>");
        assertThat(first).hasText("1");

        place("second");
        assertThat(first).hasCount(0);
        assertThat(second.locator(".lsf-view-error")).hasCount(0);
        assertThat(second).hasText("1");
        close(form);
    }

    // a 0 a view gives the member of an integer property is 0, not NULL - a JS 0 held as an object reads as null in the
    // client's Java: the view is given 0 back once the change is answered, its cell no longer loading - the value the
    // change shows at once is the view's own -, and 0 is what the form saves
    @Test
    public void aZeroAViewGivesIsZero() throws Exception {
        String[] items = seed();
        Locator form = open(FOLDER, "UiCustomView.uiCustomViewQuantities");
        Locator row = form.locator(".ui-custom-view-quantity-row").filter(new Locator.FilterOptions().setHasText(items[0]));
        Locator quantity = row.locator(".ui-custom-view-quantity");
        assertThat(quantity).hasText("number 1");
        row.locator(".ui-custom-view-zero").click();
        assertThat(quantity).hasText("number 0");
        Locator save = form.locator("button[lsfusion-container='PROPERTY(formApply())']");
        assertThat(save).isEnabled(); // the server has the change
        save.click();
        check("UiCustomView.uiCustomViewCheckQuantity", items[0], "0");
        close(form);
    }

    // the calls a React view makes to the server through the form's controller resolve to what the server answers, of
    // its type - eval to the RETURN of its script, exec to the RETURN of the action, evalAction and change to nothing -
    // and run in the form's session, a change seen by the calls after it and by the form; a call that fails is rejected
    // with its error
    @Test
    public void aViewsCallsToTheServer() throws Exception {
        Locator form = open(FOLDER, "UiCustomView.uiCustomViewCalls");
        Locator results = form.locator(".ui-custom-view-results li");
        call(form, "eval");
        assertThat(results).hasText(new String[] {"eval: number 42"});
        call(form, "evalAction");
        call(form, "read");
        assertThat(results).hasText(new String[] {"eval: number 42", "evalAction: undefined undefined", "read: number 5"});
        assertThat(value(form, "uiCustomViewCount")).hasText("5");
        call(form, "exec");
        call(form, "change");
        assertThat(results).hasText(new String[] {"eval: number 42", "evalAction: undefined undefined", "read: number 5",
                "exec: number 8", "change: undefined undefined"});
        assertThat(value(form, "uiCustomViewCount")).hasText("9");
        call(form, "broken");
        call(form, "missing");
        assertThat(results).hasText(new Pattern[] {Pattern.compile("^eval: number 42$"),
                Pattern.compile("^evalAction: undefined undefined$"), Pattern.compile("^read: number 5$"),
                Pattern.compile("^exec: number 8$"), Pattern.compile("^change: undefined undefined$"),
                Pattern.compile("^broken rejected: .+"), Pattern.compile("^missing rejected: .+")});
        close(form);
    }

    // a call made through the controller of a form the server has closed throws nothing: the promise it returns is
    // rejected - Form is closed - and does not hang. A close the client only foresees, taking the form off the page as
    // an action that may close it is clicked, is not that yet: the server may refuse it, so a call made the moment the
    // form's element goes - a view's input committed as the form goes - is sent and runs after the close's request, on
    // the form the user gets back as on the one that closes
    @Test
    public void aCallOfAClosedFormIsRejected() throws Exception {
        Locator form = open(FOLDER, "UiCustomView.uiCustomViewCalls");
        call(form, "eval");
        assertThat(form.locator(".ui-custom-view-results li")).hasText(new String[] {"eval: number 42"});
        page.evaluate("() => window.uiCustomViewCalls.form.evalAction('UiCustomView.uiCustomViewKeep() <- TRUE;')");
        Locator tryClose = form.locator("button[lsfusion-container='PROPERTY(uiCustomViewTryClose())']");

        callAsTheFormGoes("controller.form.change('uiCustomViewCount', 7)");
        tryClose.click();
        assertEquals("resolved", goneCall());
        assertThat(form).isVisible();
        assertThat(value(form, "uiCustomViewCount")).hasText("7");

        callAsTheFormGoes("controller.form.change('uiCustomViewCount', 8)");
        tryClose.click();
        assertEquals("resolved", goneCall());
        assertThat(form).hasCount(0);
        assertEquals("Form is closed", page.evaluate("() => window.uiCustomViewCalls.form.eval('run() { RETURN 1; }')"
                + ".then(() => 'resolved', error => error.message)"));
    }

    // a close asked while the client foresees one - a view's member running a close action as the view is unmounted,
    // while the form is being taken out of its window - is not foreseen again: the server refusing the first close and
    // taking the second, the form the first answer puts back goes with the second, as a form the server closes does
    @Test
    public void aCloseAskedWhileOneIsForeseenClosesTheForm() throws Exception {
        Locator form = open(FOLDER, "UiCustomView.uiCustomViewCalls");
        call(form, "eval");
        assertThat(form.locator(".ui-custom-view-results li")).hasText(new String[] {"eval: number 42"});
        page.evaluate("() => window.uiCustomViewCalls.form.evalAction('UiCustomView.uiCustomViewKeep() <- TRUE;')");

        page.evaluate("() => { window.uiCustomViewGoneCall = undefined; window.uiCustomViewUnmounted = controller =>"
                + " controller.uiCustomViewClose.exec().then(() => { window.uiCustomViewGoneCall = 'resolved'; },"
                + " error => { window.uiCustomViewGoneCall = error.message; }); }");
        form.locator("button[lsfusion-container='PROPERTY(uiCustomViewTryClose())']").click();
        assertEquals("resolved", goneCall());
        assertThat(form).hasCount(0);
    }

    // a call through the view's controller - `controller` in it - made the moment the form's element leaves the page,
    // what came of it - resolved, or the message it was rejected with - read by goneCall
    private void callAsTheFormGoes(String call) {
        page.evaluate("() => { window.uiCustomViewGoneCall = undefined;"
                + " const form = document.querySelector(\"[lsfusion-form='UiCustomView.uiCustomViewCalls']\"),"
                + " controller = window.uiCustomViewCalls;"
                + " const observer = new MutationObserver(() => { if (!form.isConnected) { observer.disconnect();"
                + " " + call + ".then(() => { window.uiCustomViewGoneCall = 'resolved'; },"
                + " error => { window.uiCustomViewGoneCall = error.message; }); } });"
                + " observer.observe(document.body, {childList: true, subtree: true}); }");
    }
    private String goneCall() {
        return (String) page.waitForFunction("() => window.uiCustomViewGoneCall", null,
                new Page.WaitForFunctionOptions().setTimeout(10_000)).jsonValue();
    }

    // the JS function an INTERNAL CLIENT action runs is given the action's parameters and the form's controller: what
    // it returns is the action's answer (TO), and a change it makes through the controller - one that declares the
    // callbacks, run asynchronously - is the form's, made in its session; the client function after it in the action
    // runs once the function says it is done, the change over, and it is given both callbacks
    @Test
    public void aClientActionIsGivenTheFormsController() throws Exception {
        Locator form = open(FOLDER, "UiCustomView.uiCustomViewClient");
        Locator answer = value(form, "uiCustomViewAnswer");
        form.locator("button[lsfusion-container='PROPERTY(uiCustomViewOnClient())']").click();
        assertThat(answer).hasText("42, with a controller");
        form.locator("button[lsfusion-container='PROPERTY(uiCustomViewThroughServer())']").click();
        assertThat(answer).hasText("the server says 63");
        assertEquals("after the change, given function function", page.waitForFunction(
                "() => window.uiCustomViewAfterServerSaw", null, new Page.WaitForFunctionOptions().setTimeout(10_000)).jsonValue());
        close(form);
    }

    // the JS function an INTERNAL CLIENT action of the navigator runs is given the navigator's controller, which calls
    // the server as a form's does: a script it evaluates resolves to its RETURN, and an action it runs by name runs
    @Test
    public void aClientActionOfTheNavigatorIsGivenItsController() throws Exception {
        seed();
        open(FOLDER, "UiCustomView.uiCustomViewFromNavigator");
        check("UiCustomView.uiCustomViewCheckNavigatorMarks", "navigator number 42");
    }

    // a custom cell renderer changes its property through its controller - the server has the value, which the form
    // shows in a property of its own - and reaches the form's controller from there, whose call resolves to what the
    // server answers
    @Test
    public void aCustomCellRendererChangesItsPropertyAndCallsTheServer() throws Exception {
        Locator form = open(FOLDER, "UiCustomView.uiCustomViewCell");
        Locator field = form.locator(".ui-custom-view-cell-field");
        field.fill("typed");
        field.blur();
        assertThat(value(form, "uiCustomViewCellEcho")).hasText("typed");
        form.locator(".ui-custom-view-cell-exec").click();
        assertThat(form.locator(".ui-custom-view-cell-result")).hasText("exec: number 8");
        close(form);
    }

    // a property's CUSTOM that is a React component: the platform draws each cell with it, and the component's
    // controller changes the cell it stands in - a row of the grid that is not the current one, the current item on
    // the panel, the one value of a property of no group -, while its host is out of the page too, and names no row.
    // READONLYIF is the server's to refuse, as with a classic render function. A cell the form hides is unmounted, and
    // a call through the controller it had is refused while the form hides the property
    @Test
    public void reactCustomCellsUseTheirOwnKeysAndUnmountWhenHidden() throws Exception {
        String[] items = seed();
        Locator form = open(FOLDER, "UiCustomView.uiCustomViewReactCells");
        Locator cells = form.locator(".data-grid-cell .ui-custom-react-cell");
        assertThat(cells).hasText(new String[] {"1", "2"});
        assertThat(cells.first()).isDisabled();
        cells.first().evaluate("async button => { await button.testController.change(99); }");
        assertThat(cells.first()).hasText("1");
        cells.nth(1).evaluate("button => {"
                + "const host = button.closest('lsfusion-react-cell');"
                + "const parent = host.parentNode, next = host.nextSibling;"
                + "host.remove();"
                + "try { return button.testController.change(12); }"
                + "finally { parent.insertBefore(host, next); }"
                + "}");
        assertThat(cells).hasText(new String[] {"1", "12"});
        String noRow = "takes no row: the component draws one cell, and this is that cell's controller";
        assertEquals("controller.change(): " + noRow, // a 0 is a row given too
                cells.nth(1).evaluate("button => { try { button.testController.change(5, 0); } catch (e) { return e.message; } }"));
        assertEquals("controller.getValues(): " + noRow,
                cells.nth(1).evaluate("button => { try { button.testController.getValues('1', {row: {}}); } catch (e) { return e.message; } }"));

        value(form, "uiCustomViewCount").locator(".ui-custom-react-cell").click();
        assertThat(value(form, "uiCustomViewCountEcho")).hasText("10");
        assertThat(cells.first()).isEnabled();
        assertThat(cells.first()).hasText("1");

        form.getByText(items[1], new Locator.GetByTextOptions().setExact(true)).click();
        form.locator(".panel-renderer-value .ui-custom-react-cell")
                .filter(new Locator.FilterOptions().setHasText("12")).click();
        assertThat(cells).hasText(new String[] {"1", "22"});

        page.evaluate("() => { window.uiCustomViewReleasedCell = null; }");
        form.locator("button[lsfusion-container='PROPERTY(uiCustomViewToggleCells())']").click();
        assertThat(cells).hasCount(0);
        page.waitForFunction("() => !!window.uiCustomViewReleasedCell");
        assertEquals("controller.change(): 'uiCustomViewQuantity' is not shown by the form now"
                        + " (its entry is hidden), so it has nobody to change it for",
                page.evaluate("() => { try { window.uiCustomViewReleasedCell.change(99); } catch (e) { return e.message; } }"));

        form.locator("button[lsfusion-container='PROPERTY(uiCustomViewToggleCells())']").click();
        assertThat(cells).hasText(new String[] {"1", "22"});
        form.locator("button[lsfusion-container='PROPERTY(formApply())']").click();
        check("UiCustomView.uiCustomViewCheckQuantity", items[0], "1");
        check("UiCustomView.uiCustomViewCheckQuantity", items[1], "22");
        close(form);
    }

    // a grid hands its cells other rows as they are sorted: the component of a cell stays mounted, with its state and
    // the controller it was first given, which changes the row the cell shows now. The row a grid sizes its columns by
    // draws no component
    @Test
    public void reactCustomCellsKeepTheirComponentsWhenRowsMove() throws Exception {
        String[] items = seed();
        Locator form = open(FOLDER, "UiCustomView.uiCustomViewReactCells");
        Locator cells = form.locator(".data-grid-cell .ui-custom-react-cell");
        assertThat(cells).hasText(new String[] {"1", "2"});
        Locator sizingRow = form.locator(".data-grid-column-row").first();
        assertThat(sizingRow).isAttached();
        assertThat(sizingRow.locator(".ui-custom-react-cell")).hasCount(0);
        String firstInstance = cells.first().getAttribute("data-instance");
        String secondInstance = cells.last().getAttribute("data-instance");

        form.locator("th[lsfusion-container='PROPERTY(uiCustomViewName(i))']:visible").dblclick();
        assertThat(cells).hasText(new String[] {"2", "1"});
        assertThat(cells.first()).hasAttribute("data-instance", firstInstance);
        assertThat(cells.last()).hasAttribute("data-instance", secondInstance);
        assertThat(cells.first()).hasAttribute("data-matching-context", "true");
        assertThat(cells.last()).hasAttribute("data-matching-context", "true");
        assertThat(cells.first()).hasAttribute("data-stable-controller", "true");
        assertThat(cells.last()).hasAttribute("data-stable-controller", "true");

        cells.first().click();
        assertThat(cells).hasText(new String[] {"12", "1"});
        form.locator("button[lsfusion-container='PROPERTY(formApply())']").click();
        check("UiCustomView.uiCustomViewCheckQuantity", items[0], "1");
        check("UiCustomView.uiCustomViewCheckQuantity", items[1], "12");
        close(form);
    }

    // a group's CUSTOM that is a React component: it draws the rows it is given, with the group's OPTIONS, and changes
    // the current object and a row's value through the classic controller of a custom view; the grid switched to the
    // table and back draws it again
    @Test
    public void reactCustomGroupUsesClassicRowsAndSwitchesViews() throws Exception {
        String[] items = seed();
        Locator form = open(FOLDER, "UiCustomView.uiCustomViewReactGroup");
        Locator cards = form.locator(".ui-custom-cards");
        assertThat(cards).containsText("Cards");
        Locator rows = cards.locator(".ui-custom-card");
        assertThat(rows).hasCount(2);
        Locator second = rows.nth(1);
        second.getByText(items[1]).click();
        assertThat(second).hasAttribute("data-current", "true");
        second.locator(".ui-custom-card-quantity").click();
        assertThat(second.locator(".ui-custom-card-quantity")).hasText("12");
        assertThat(rows.first().locator(".ui-custom-card-quantity")).hasText("1");

        form.locator("[title='Table']").click();
        assertThat(cards).hasCount(0);
        form.locator("[title='Custom view']").click();
        assertThat(cards).containsText("Cards");
        assertThat(rows.nth(1).locator(".ui-custom-card-quantity")).hasText("12");

        form.locator("button[lsfusion-container='PROPERTY(formApply())']").click();
        check("UiCustomView.uiCustomViewCheckQuantity", items[0], "1");
        check("UiCustomView.uiCustomViewCheckQuantity", items[1], "12");
        close(form);
    }

    // a group in CUSTOM view inside a React container: the platform draws its grid in that view with no custom = NULL
    // on it, and the container places it - the group's React component draws the rows there and changes one
    @Test
    public void reactContainerPlacesTheGridOfACustomGroup() throws Exception {
        String[] items = seed();
        Locator form = open(FOLDER, "UiCustomView.uiCustomViewPlacedGroup");
        Locator rows = form.locator(".ui-custom-cards-outer .lsf-platform .ui-custom-cards .ui-custom-card");
        assertThat(rows).hasCount(2);
        rows.nth(1).locator(".ui-custom-card-quantity").click();
        assertThat(rows.nth(1).locator(".ui-custom-card-quantity")).hasText("12");
        form.locator("button[lsfusion-container='PROPERTY(formApply())']").click();
        check("UiCustomView.uiCustomViewCheckQuantity", items[1], "12");
        close(form);
    }

    // a React view places what the platform draws with a React component of a property's CUSTOM as it places the
    // platform's own editor: a row's cell in the row, the panel's value where the view puts it, each changing its own
    @Test
    public void reactFormPlacesPlatformCustomPropertiesInGridAndPanel() throws Exception {
        String[] items = seed();
        Locator form = open(FOLDER, "UiCustomView.uiCustomViewPlacedProperties");
        Locator rows = form.locator(".ui-custom-placed-grid .ui-custom-placed-row");
        Locator panel = form.locator(".ui-custom-placed-panel lsfusion-react-cell .ui-custom-react-cell");
        assertThat(rows).hasCount(2);
        assertThat(form.locator("lsfusion-react-cell")).hasCount(3);
        assertThat(rows.first().locator(".ui-custom-react-cell")).hasText("1");
        assertThat(rows.first().locator(".ui-custom-standard-cell")).hasText("1");
        assertThat(panel).hasAttribute("data-matching-context", "true");
        Locator second = rows.nth(1);
        Locator cell = second.locator(".ui-custom-react-cell");
        assertThat(cell).hasAttribute("data-matching-context", "true");
        rows.first().getByText(items[0]).click();
        assertThat(panel).hasText("1");
        cell.click();
        assertThat(cell).hasText("12");
        assertThat(panel).hasText("1");
        second.getByText(items[1]).click();
        assertThat(second).hasAttribute("data-current", "true");
        assertThat(panel).hasText("12");
        panel.click();
        assertThat(panel).hasText("22");
        assertThat(cell).hasText("22");
        assertThat(cell).hasAttribute("data-stable-controller", "true");
        assertThat(panel).hasAttribute("data-stable-controller", "true");
        assertThat(rows.first().locator(".ui-custom-react-cell")).hasText("1");
        form.locator("button[lsfusion-container='PROPERTY(formApply())']").click();
        check("UiCustomView.uiCustomViewCheckQuantity", items[0], "1");
        check("UiCustomView.uiCustomViewCheckQuantity", items[1], "22");
        close(form);
    }

    // a classic render function of a group named as a component is, and not registered as one, is called as before
    @Test
    public void anUppercaseClassicCustomGroupStillUsesCallbacks() throws Exception {
        seed();
        Locator form = open(FOLDER, "UiCustomView.uiCustomViewClassicGroup");
        assertThat(form.getByText("2 classic rows")).isVisible();
        close(form);
    }

    // a property the form hides leaves a group's custom view, and what it was drawn with leaves with it: the property
    // after it keeps its own
    @Test
    public void aCustomGroupDropsAHiddenPropertyWithItsAttributes() throws Exception {
        seed();
        Locator form = open(FOLDER, "UiCustomView.uiCustomViewClassicGroup");
        Locator view = form.locator("[data-name-background]");
        Locator toggle = form.locator("button[lsfusion-container='PROPERTY(uiCustomViewToggleCells())']");
        assertThat(view).hasAttribute("data-marked", "true");
        assertThat(view).hasAttribute("data-name-background", "none");
        toggle.click();
        assertThat(view).hasAttribute("data-marked", "false");
        assertThat(view).hasAttribute("data-name-background", "none");
        toggle.click();
        assertThat(view).hasAttribute("data-marked", "true");
        assertThat(view).hasAttribute("data-name-background", "none");
        close(form);
    }

    // two items under names no other attempt has - what a failed attempt saved stays in the database for its rerun -,
    // of the quantities 1 and 2
    private static String[] seed() throws Exception {
        long id = System.nanoTime();
        String[] items = {"First " + id, "Second " + id};
        exec("UiCustomView.uiCustomViewSeed", items);
        return items;
    }

    private static void call(Locator form, String name) {
        form.locator(".ui-custom-view-" + name).click();
    }

    private static Locator value(Locator form, String property) {
        String container = "[lsfusion-container='PROPERTY(" + property + "())']";
        return form.locator(container + ".panel-renderer-value, " + container + " .panel-renderer-value");
    }

    // what the first row of the rows view places from now on, see UiCustomView.js
    private void place(String what) {
        page.waitForFunction("() => !!window.uiCustomViewRows");
        page.evaluate("what => window.uiCustomViewRows.place(what)", what);
    }
}
