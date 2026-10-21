package lsfusion.tests;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Route;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.Assert.assertEquals;

// what a React view is given and changes through its members (UiReactProjection.lsf, its views in
// UiReactProjection.js): an entry the form hides, which keeps its member; the group's member choosing a row; a color
// and an interval taken back as they went out; a change waiting for its answer, whoever sends it; a page the view does
// not show, which is not read; a row's own background as the server changes it - and the classic custom view's
// changeProperty, which names a property of the whole form by the name alone
public class UiReactProjectionIT extends UiTest {

    // an entry the form hides says so, its member staying the same: a column by its SHOWIF - its cells with no value,
    // and a call through its member refused meanwhile, so that once it shows the column has the change made before
    // it, not the value of the refused call, and the member the view got first answers again -, a property of the
    // form level by a design showIf, which takes its value too, and the grid, a component like any other, whose rows
    // stay with no values in their cells
    @Test
    public void anEntryTheFormHidesKeepsItsMemberWhichRefusesItsCall() throws Exception {
        String[] items = seed();
        Locator form = open("UiReactProjection.uiRpj", "UiReactProjection.uiRpjMembers");
        Locator price = form.locator(".ui-rpj-price-entry"), rows = form.locator(".ui-rpj-row"),
                member = form.locator(".ui-rpj-same-member"), refusal = form.locator(".ui-rpj-refusal");
        assertThat(price).hasText("price shown Price");
        assertThat(rows).hasText(new String[] {items[0] + "|10", items[1] + "|20"});
        click(form, "price 42");
        assertThat(rows).hasText(new String[] {items[0] + "|42", items[1] + "|20"});

        button(form, "uiRpjTogglePrice").click();
        assertThat(price).hasText("price hidden Price");
        assertThat(rows).hasText(new String[] {items[0] + "|no value", items[1] + "|no value"});
        assertThat(member).hasText("the same member");
        click(form, "price 44");
        assertThat(refusal).hasText("controller.i.price.change(): 'price' is not shown by the form now (its entry is"
                + " hidden), so it has nobody to change it for");
        button(form, "uiRpjTogglePrice").click();
        assertThat(price).hasText("price shown Price");
        assertThat(rows).hasText(new String[] {items[0] + "|42", items[1] + "|20"});
        assertThat(member).hasText("the same member");
        click(form, "price 43 by the first member"); // the member as the view first got it answers again
        assertThat(rows).hasText(new String[] {items[0] + "|43", items[1] + "|20"});

        Locator label = form.locator(".ui-rpj-label");
        assertThat(label).hasText("shown label");
        button(form, "uiRpjToggleLabel").click();
        assertThat(label).hasText("hidden no value");
        button(form, "uiRpjToggleLabel").click();
        assertThat(label).hasText("shown label");

        Locator grid = form.locator(".ui-rpj-grid");
        assertThat(grid).hasText("grid shown");
        button(form, "uiRpjToggleGrid").click();
        assertThat(grid).hasText("grid hidden");
        assertThat(rows).hasText(new String[] {"no value|no value", "no value|no value"});
        button(form, "uiRpjToggleGrid").click();
        assertThat(grid).hasText("grid shown");
        assertThat(rows).hasText(new String[] {items[0] + "|43", items[1] + "|20"});
        save(form);
        check("UiReactProjection.uiRpjCheckPrice", items[0], "43");
        close(form);
    }

    // the group's member makes a row current where the view draws the rows: by the row, and by the key the view was
    // given it under - the row says it is current, and the server reads the current object's name
    @Test
    public void theGroupsMemberMakesARowCurrent() throws Exception {
        String[] items = seed();
        Locator form = open("UiReactProjection.uiRpj", "UiReactProjection.uiRpjMembers");
        Locator current = form.locator(".ui-rpj-current"), read = form.locator(".ui-rpj-current-name");
        assertThat(current).hasText(items[0]);
        assertThat(read).hasText(items[0]);
        click(form, "second current");
        assertThat(current).hasText(items[1]);
        assertThat(read).hasText(items[1]);
        click(form, "first current by key");
        assertThat(current).hasText(items[0]);
        assertThat(read).hasText(items[0]);
        close(form);
    }

    // a color and an interval go back as they came out: the view hands the first row's to the second row's members as
    // it was given them, and the server stores them
    @Test
    public void aColorAndAnIntervalGoBackAsTheyCameOut() throws Exception {
        String[] items = seed();
        Locator form = open("UiReactProjection.uiRpj", "UiReactProjection.uiRpjValues");
        Locator rows = form.locator(".ui-rpj-value-row");
        String values = "|#ff0000|1767225600000.1769817600000"; // January 2026, its bounds midnights of UTC
        assertThat(rows).hasText(new String[] {items[0] + values, items[1] + "|null|null"});
        click(form, "copy to the second");
        assertThat(rows).hasText(new String[] {items[0] + values, items[1] + values});
        save(form);
        check("UiReactProjection.uiRpjCheckValues", items[0], items[1]);
        close(form);
    }

    // a change says `loading` until its answer comes, whoever sends it: an action and a cell the view changes through
    // their members - the other row's cell not -, and a property and an action the platform draws, which the classic
    // custom view's changeProperty changes, the property's own ON CHANGE deciding its value: the platform's cell waits
    // with its own value, not the one given. The answers are held back here until the waiting is seen
    @Test
    public void aChangeSaysItIsLoadingUntilItsAnswerComes() throws Exception {
        String[] items = seed();
        Locator form = open("UiReactProjection.uiRpj", "UiReactProjection.uiRpjLoading");
        Locator run = form.locator(".ui-rpj-run"), counter = form.locator(".ui-rpj-counter"), qty = form.locator(".ui-rpj-qty");
        assertThat(run).hasText("run answered");
        assertThat(qty).hasText(new String[] {items[0] + "|1", items[1] + "|2"});

        hold();
        click(form, "run");
        assertThat(run).hasText("run loading");
        release();
        assertThat(run).hasText("run answered");
        assertThat(counter).hasText("1");

        hold();
        click(form, "qty 7");
        assertThat(qty).hasText(new String[] {items[0] + "|7|loading", items[1] + "|2"});
        release();
        assertThat(qty).hasText(new String[] {items[0] + "|7", items[1] + "|2"});

        Locator nativeRun = button(form, "uiRpjNativeRun");
        assertThat(loading(nativeRun)).hasCount(0);
        hold();
        page.evaluate("() => window.uiRpjClassicController.changeProperty('uiRpjNativeRun')");
        assertThat(loading(nativeRun)).hasCount(1);
        release();
        assertThat(loading(nativeRun)).hasCount(0);
        assertThat(counter).hasText("101");

        // its change runs its ON CHANGE, which asks no value: the platform draws it as text
        Locator nativeNote = form.locator("[lsfusion-container='PROPERTY(uiRpjNativeNote())'] .panel-renderer-value");
        assertThat(nativeNote).hasText("initial");
        hold();
        page.evaluate("() => window.uiRpjClassicController.changeProperty('uiRpjNativeNote', null, 'given')");
        assertThat(loading(nativeNote)).hasCount(1);
        assertThat(nativeNote).hasText("initial");
        release();
        assertThat(loading(nativeNote)).hasCount(0);
        assertThat(nativeNote).hasText("served");
        save(form);
        close(form);
    }

    // a container a view draws itself is read only while the view says it shows it (useShown): a property on a page
    // of the view's strip it does not show reads as null once it changes, as on a tab the platform does not show, while
    // one beside the pages is read; shown, the page is read again, and not again once it is not shown again
    @Test
    public void aPageTheViewDoesNotShowIsNotRead() throws Exception {
        Locator[] form = new Locator[1];
        // the view says it does not show the second page once it has drawn
        page.waitForRequest(request -> carries(request.postData(), "SetUserHidden"),
                () -> form[0] = open("UiReactProjection.uiRpj", "UiReactProjection.uiRpjPages"));
        Locator witness = form[0].locator(".ui-rpj-witness"), count = form[0].locator(".ui-rpj-count");
        Locator bump = button(form[0], "uiRpjBump");

        bump.click();
        assertThat(witness).hasText("1");
        assertThat(count).hasText("null");
        page.waitForRequest(request -> carries(request.postData(), "SetUserHidden"), () -> click(form[0], "page B"));
        assertThat(count).hasText("1");
        bump.click();
        assertThat(witness).hasText("2");
        assertThat(count).hasText("2");

        page.waitForRequest(request -> carries(request.postData(), "SetUserHidden"), () -> click(form[0], "page A"));
        bump.click();
        assertThat(witness).hasText("3");
        assertThat(count).hasText("null");
        page.waitForRequest(request -> carries(request.postData(), "SetUserHidden"), () -> click(form[0], "page B"));
        assertThat(count).hasText("3");
        close(form[0]);
    }

    // a row's own background is in the projection as the server changes what it reads: the row whose flag a change on
    // the server toggles gets the other color, the other row keeps its own
    @Test
    public void aRowsOwnBackgroundFollowsTheServer() throws Exception {
        String[] items = seed();
        Locator form = open("UiReactProjection.uiRpj", "UiReactProjection.uiRpjColors");
        Locator rows = form.locator(".ui-rpj-color-row span");
        String green = "|#d2f5d2", red = "|#ffc8c8";
        assertThat(rows).hasText(new String[] {items[0] + green, items[1] + green});
        click(form, "toggle " + items[0]);
        assertThat(rows).hasText(new String[] {items[0] + red, items[1] + green});
        click(form, "toggle " + items[0]);
        assertThat(rows).hasText(new String[] {items[0] + green, items[1] + green});
        close(form);
    }

    // the classic custom view's changeProperty names a property of the whole form by the name alone, as a React view
    // names it - with its group, or bare for the form level: a bare name an object group draws is refused with the name
    // to use, a name of the form level given a row, an unknown group, a name drawn twice and a row of another group are
    // refused, saying why; a property of another group, one of the form level and the view's own column given a row go
    // through
    @Test
    public void theClassicChangePropertyNamesAPropertyOfTheWholeForm() throws Exception {
        String[] items = seed();
        Locator form = open("UiReactProjection.uiRpj", "UiReactProjection.uiRpjClassic");
        Locator cards = form.locator(".ui-rpj-card");
        assertThat(cards).hasText(new String[] {items[0] + "|10", items[1] + "|20"});
        assertRefused("changeProperty('note', null, 'x')", "controller.changeProperty('note'): 'note' names no property of"
                + " the form level; a property of an object group is named with its group, 'c.note'");
        String formLevel = "controller.changeProperty('fnote'): 'fnote' names a property of the form level, which has no"
                + " rows: a property of an object group is named with its group, '<group>.fnote'";
        assertRefused("changeProperty('fnote', list[0], 'x')", formLevel);
        assertRefused("changeProperty('fnote', list[0])", formLevel);
        assertRefused("changeProperty('x.note', null, 'x')", "controller.changeProperty('x.note'): unknown object group 'x'");
        assertRefused("changeProperty('twice', null, 'x')", "controller.changeProperty('twice'): 'twice' is drawn more than"
                + " once on the form level, so this name does not say which of them is meant; give them explicit EXTIDs");
        assertRefused("changeProperty('c.note', list[0], 'x')", "controller.changeProperty('c.note'): that row is not a row of 'c'");

        String note = "panel " + items[0], formNote = "form " + items[0];
        classic("changeProperty('c.note', null, '" + note + "')");
        assertThat(form.locator("[lsfusion-container='PROPERTY(note)'] input")).hasValue(note);
        classic("changeProperty('fnote', null, '" + formNote + "')");
        assertThat(form.locator("[lsfusion-container='PROPERTY(fnote)'] input")).hasValue(formNote);
        classic("changeProperty('name', list[1], '" + items[1] + "*')");
        assertThat(cards).hasText(new String[] {items[0] + "|10", items[1] + "*|20"});
        save(form);
        // what the view shows may be what the client predicted: the server's is what was saved
        check("UiReactProjection.uiRpjCheckClassic", note, formNote, items[1] + "*");
        close(form);
    }

    // a category shown from now on, under names no other attempt has - what a failed attempt saved stays in the
    // database for its rerun -, with its two items: the first priced 10, of qty 1, red, of January 2026, the second
    // priced 20, of qty 2, of no color and no period
    private static String[] seed() throws Exception {
        long id = System.nanoTime();
        String[] items = {"Apple " + id, "Banana " + id};
        exec("UiReactProjection.uiRpjSeed", "Category " + id, "note " + id, items[0], items[1]);
        return items;
    }

    // a call of the classic custom view's controller with its rows, `list`, as it was given them last
    private void classic(String call) {
        page.evaluate("() => { const controller = window.uiRpjCardsState.controller, list = window.uiRpjCardsState.list; controller." + call + "; }");
    }

    private void assertRefused(String call, String refusal) {
        assertEquals(call, refusal, page.evaluate("() => { const controller = window.uiRpjCardsState.controller, list = window.uiRpjCardsState.list;"
                + " try { controller." + call + "; return 'not refused'; } catch (e) { return e.message; } }"));
    }

    // the form's requests held from now on, until let go: the server sees none of them, so their answers wait
    private final List<Route> held = new ArrayList<>();

    private void hold() {
        page.route("**/main/dispatch", held::add);
    }

    // ... the requests held let go, in the order they were made, and no more held
    private void release() {
        page.unroute("**/main/dispatch");
        for (Route route : held)
            route.resume();
        held.clear();
    }

    // a GWT-RPC request carries the class of the action it is: "<package>.<class>/<hash>"
    private static boolean carries(String body, String action) {
        return body != null && body.contains("." + action + "/");
    }

    // what says a platform's cell, or button, waits for its answer
    private static Locator loading(Locator view) {
        return view.locator(".property-toolbar-loading, .fa-spinner");
    }

    // a button the platform draws, by its property
    private static Locator button(Locator form, String action) {
        return form.locator("button[lsfusion-container='PROPERTY(" + action + "())']");
    }

    // a button of a view, by its text
    private static void click(Locator form, String text) {
        form.getByText(text, new Locator.GetByTextOptions().setExact(true)).click();
    }

    private static void save(Locator form) {
        form.locator("button[lsfusion-container='PROPERTY(formApply())']").click();
    }
}
