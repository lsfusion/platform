package lsfusion.tests;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.PlaywrightException;
import com.microsoft.playwright.Route;
import com.microsoft.playwright.options.AriaRole;
import org.junit.Test;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Pattern;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.Assert.assertEquals;

// a React view's controller.batch (UiReactBatch.lsf, its views UiReactBatch.js): the member changes made in it shown at
// once and sent together in one request, which the server makes in their order - one that throws sending what it made
// before, one carrying changes only, one the server fails taking the values back -, a classic custom view's batch; and
// what the platform draws in a React view, with an entry and no member (#1875)
public class UiReactBatchIT extends UiTest {

    private static final String FOLDER = "UiReactBatch.uiRb";
    private static final String REFUSAL = "a batch is running, which carries changes only: make this call before the batch or after it";
    // every load logs these two: chrome has no push api in an incognito context, which a test's is
    private static final Pattern PUSH_API = Pattern.compile("Push API|Registration failed - permission denied");

    // what the platform draws in a React view has an entry and no member - a panel action its entry saying only whether
    // it is shown, an action in each row its column's entry with what it is and no caption, a property its column's
    // entry labelling it and no cell in the rows, a property of the form its entry -, while what React draws has its
    // member; a view placing only a property the platform draws of a group has the group's data and an empty member of
    // it (#1875)
    @Test
    public void whatThePlatformDrawsHasAnEntryAndNoMember() throws Exception {
        String[] items = seed();
        Locator form = open(FOLDER, "UiReactBatch.uiRbShape");
        waitFor("board", "names() === '" + String.join(",", items) + "'");
        assertEquals("function,function,function", eval("board", "[typeof controller.i.note.change, typeof controller.i.incQty.exec,"
                + " typeof controller.toggleApprove.exec].join()"));
        assertEquals("/undefined", eval("board", "Object.keys(data.i.approve).join() + '/' + typeof controller.i.approve"));
        assertEquals("false,true,true,undefined", eval("board", "['caption' in data.i.bump, 'type' in data.i.bump, 'noSort' in data.i.bump,"
                + " typeof controller.i.bump].join()"));
        assertEquals("Qty,false,undefined", eval("board", "[data.i.qty.caption, 'qty' in row(" + js(items[1]) + "), typeof controller.i.qty].join()"));
        assertEquals("Comment,undefined", eval("board", "[data.comment.caption, typeof controller.comment].join()"));
        assertEquals("function,function,function", eval("board", "[typeof controller.exec, typeof controller.change, typeof controller.batch].join()"));

        assertEquals("Note,false,object,0,function", eval("side", "[data.i.sideNote.caption, 'list' in data.i, typeof controller.i,"
                + " Object.keys(controller.i).length, typeof controller.batch].join()"));
        assertThat(form.locator(".ui-rb-side .lsf-platform input, .ui-rb-side .lsf-platform .panel-renderer-value").first()).isVisible();
        close(form);
    }

    // the platform's editor and buttons in a React view's rows are the platform's: a value the server changes shows in
    // the editor of its row; the panel action's button runs it on the current object, an action's button in a row on
    // its row; the panel action's SHOWIF hides its entry and its host, and shows them again (#1875)
    @Test
    public void thePlatformsEditorsAndButtonsInAView() throws Exception {
        String[] items = seed();
        Locator form = open(FOLDER, "UiReactBatch.uiRbShape");
        waitFor("board", "names() === '" + String.join(",", items) + "'");
        call("board", "controller.i.incQty.exec(row(" + js(items[1]) + "))");
        waitFor("board", "row(" + js(items[1]) + ").qtyEcho.value === 12");
        assertThat(boardRow(form, items[1]).locator(".ui-rb-qty")).hasText("12");

        call("board", "controller.i.change(row(" + js(items[2]) + "))");
        waitFor("board", "current() === " + js(items[2]));
        form.locator(".ui-rb-approve button").click();
        waitFor("board", "row(" + js(items[2]) + ").approvals.value === 1");
        boardRow(form, items[0]).locator(".ui-rb-bump button").click();
        waitFor("board", "row(" + js(items[0]) + ").bumps.value === 1");
        waitFor("board", "row(" + js(items[2]) + ").bumps.value === null && row(" + js(items[2]) + ").approvals.value === 1");

        Locator approve = form.locator(".ui-rb-approve > .lsf-platform");
        call("board", "controller.toggleApprove.exec()");
        waitFor("board", "data.i.approve.hidden === true");
        assertThat(approve).isHidden();
        call("board", "controller.toggleApprove.exec()");
        waitFor("board", "!('hidden' in data.i.approve)");
        assertThat(approve).isVisible();
        save(form);
        close(form);
    }

    // a batch of a current object, a sorting, a value and an action is one request (ExecuteBatch), none of its changes
    // sent alone and nothing thrown: the server makes them in their order - the value goes to the new current row, the
    // action runs for its row, the rows come sorted (#1875)
    @Test
    public void aBatchIsOneRequest() throws Exception {
        String[] items = seed();
        String note = "b" + System.nanoTime();
        List<String> errors = errors();
        Locator form = open(FOLDER, "UiReactBatch.uiRbShape");
        waitFor("board", "names() === '" + String.join(",", items) + "'");
        call("board", "controller.i.change(row(" + js(items[0]) + "))");
        waitFor("board", "current() === " + js(items[0]));
        List<String> requests = requests();
        call("board", "controller.batch(() => { controller.i.change(row(" + js(items[1]) + ")); controller.i.orders.change({property: 'sum', desc: true});"
                + " controller.i.note.change(" + js(note) + "); controller.i.incQty.exec(row(" + js(items[2]) + ")); })");
        waitFor("board", "names() === '" + items[1] + "," + items[2] + "," + items[0] + "' && current() === " + js(items[1])
                + " && row(" + js(items[1]) + ").note.value === " + js(note) + " && row(" + js(items[2]) + ").qtyEcho.value === 13");
        assertEquals("[{\"property\":\"sum\",\"desc\":true}]", eval("board", "JSON.stringify(data.i.orders)"));
        assertEquals("null", eval("board", "row(" + js(items[0]) + ").note.value"));
        assertEquals(1, count(requests, "ExecuteBatch"));
        assertEquals(0, requests.stream().filter(request -> !request.contains(signature("ExecuteBatch")) && (request.contains(signature("ChangeGroupObject"))
                || request.contains(signature("SetPropertyOrders")) || request.contains(signature("ExecuteEventAction")))).count());
        assertEquals(Collections.emptyList(), errors);
        saveAndCheckNotes(form, items, "-", note, "-");
        close(form);
    }

    // a batch that throws ends with the throw, which reaches the view: what it made before is sent, in one request, and
    // made (#1875)
    @Test
    public void aBatchThatThrowsSendsWhatItMadeBefore() throws Exception {
        String[] items = seed();
        String note = "x" + System.nanoTime();
        Locator form = open(FOLDER, "UiReactBatch.uiRbShape");
        waitFor("board", "names() === '" + String.join(",", items) + "'");
        call("board", "controller.i.change(row(" + js(items[1]) + "))");
        waitFor("board", "current() === " + js(items[1]));
        List<String> requests = requests();
        assertEquals("stop", refusal("board", "controller.batch(() => { controller.i.note.change(" + js(note) + "); throw new Error('stop'); })"));
        waitFor("board", "row(" + js(items[1]) + ").note.value === " + js(note) + " && !row(" + js(items[1]) + ").note.loading");
        assertEquals(1, count(requests, "ExecuteBatch"));
        saveAndCheckNotes(form, items, "-", note, "-");
        close(form);
    }

    // a batch carries changes only: a server call on the controller in it is refused - its Promise rejected with the
    // reason -, so is a lookup - its fail called, the reason on the console -, neither sent, the batch going on with
    // the change made before them, in one request; another view's batch begun in it is a part of it; outside a batch
    // the server call is made (#1875)
    @Test
    public void aBatchCarriesChangesOnly() throws Exception {
        String[] items = seed();
        String note = "y" + System.nanoTime();
        Locator form = open(FOLDER, "UiReactBatch.uiRbShape");
        waitFor("board", "names() === '" + String.join(",", items) + "'");
        call("board", "controller.i.change(row(" + js(items[1]) + "))");
        waitFor("board", "current() === " + js(items[1]));
        List<String> requests = requests();
        call("board", "window.uiRbRefused = 'pending'; controller.batch(() => { controller.i.note.change('outer');"
                + " window.uiRbViews.side.controller.batch(() => controller.i.note.change(" + js(note) + "));"
                + " controller.exec('uiRbPing').then(() => window.uiRbRefused = 'made', e => window.uiRbRefused = e.message); })");
        page.waitForConsoleMessage(new Page.WaitForConsoleMessageOptions().setPredicate(message -> message.text().equals("controller.i.note.getValues(): " + REFUSAL)),
                () -> call("board", "window.uiRbLookup = 'pending'; controller.batch(() => controller.i.note.getValues('', () => window.uiRbLookup = 'made',"
                        + " () => window.uiRbLookup = 'failed'))"));
        assertEquals("controller.exec(): " + REFUSAL, eval("board", "window.uiRbRefused"));
        assertEquals("failed", eval("board", "window.uiRbLookup"));
        waitFor("board", "row(" + js(items[1]) + ").note.value === " + js(note) + " && !row(" + js(items[1]) + ").note.loading");
        assertEquals(0, requests.stream().filter(request -> request.contains(signature("ControllerExecAction")) || request.contains(signature("GetAsyncValues"))
                || request.contains(signature("GetPriorityAsyncValues"))).count());
        assertEquals(1, count(requests, "ExecuteBatch"));

        assertEquals("ok", eval("board", "controller.exec('uiRbPing').then(() => 'ok')"));
        saveAndCheckNotes(form, items, "-", note, "-");
        close(form);
    }

    // a sorting names a property of the group wherever it is drawn: a panel property the platform draws, a column the
    // platform draws per row, which has no member; the sortings list them by their names; an unknown name is refused
    @Test
    public void aSortingNamesAPropertyWhereverItIsDrawn() throws Exception {
        String[] items = seed();
        Locator form = open(FOLDER, "UiReactBatch.uiRbShape");
        waitFor("board", "names() === '" + String.join(",", items) + "'");
        call("board", "controller.i.orders.change([{property: 'weight', desc: true}])");
        waitFor("board", "names() === '" + items[0] + "," + items[2] + "," + items[1] + "' && JSON.stringify(data.i.orders) === '[{\"property\":\"weight\",\"desc\":true}]'");
        call("board", "controller.i.orders.change([{property: 'qty', desc: false}])");
        waitFor("board", "names() === '" + String.join(",", items) + "' && JSON.stringify(data.i.orders) === '[{\"property\":\"qty\",\"desc\":false}]'");
        assertEquals("controller.i.orders.change(): unknown property 'nosuch' of object group 'i'",
                refusal("board", "controller.i.orders.change({property: 'nosuch', desc: true})"));
        call("board", "controller.i.orders.change([])");
        waitFor("board", "data.i.orders.length === 0");
        close(form);
    }

    // a batch the server fails - its sorting's ON ORDER failing as the server reads the form after the batch's
    // changes, one value changed twice among them - shows the value changed at once, waiting for the answer, and then
    // as it was before the batch, not as its first change; what the server made before the failure stays in the form's
    // session, the second change, which the next answer brings (#1875)
    @Test
    public void aBatchTheServerFailsTakesTheValuesBack() throws Exception {
        String[] items = seed();
        String before = "g" + System.nanoTime();
        Locator form = open(FOLDER, "UiReactBatch.uiRbShape");
        waitFor("board", "names() === '" + String.join(",", items) + "'");
        call("board", "controller.i.note.change(" + js(before) + ", row(" + js(items[2]) + "))");
        waitFor("board", "row(" + js(items[2]) + ").note.value === " + js(before) + " && !row(" + js(items[2]) + ").note.loading");
        call("board", "controller.toggleFail.exec()"); // a request before the batch's
        // the batch's request held until the view shows its change
        List<Route> held = new CopyOnWriteArrayList<>();
        page.route("**/main/dispatch", route -> {
            if (held.isEmpty() && route.request().postData() != null && route.request().postData().contains(signature("ExecuteBatch")))
                held.add(route);
            else
                route.resume();
        });
        call("board", "controller.batch(() => { controller.i.orders.change({property: 'sum', desc: true});"
                + " controller.i.note.change('f1', row(" + js(items[2]) + ")); controller.i.note.change('f2', row(" + js(items[2]) + ")); })");
        waitFor("board", "row(" + js(items[2]) + ").note.value === 'f2' && row(" + js(items[2]) + ").note.loading === true");
        page.waitForCondition(() -> !held.isEmpty());
        held.get(0).resume();
        page.unroute("**/main/dispatch");
        Locator error = page.locator(".modal-content").filter(new Locator.FilterOptions().setHasText("uiRbFail: a sorting failed"));
        assertThat(error).isVisible();
        waitFor("board", "row(" + js(items[2]) + ").note.value === " + js(before) + " && !row(" + js(items[2]) + ").note.loading");
        error.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Close").setExact(true)).click();
        call("board", "controller.toggleFail.exec()"); // the next request, which the sorting fails no more
        waitFor("board", "row(" + js(items[2]) + ").note.value === 'f2' && !row(" + js(items[2]) + ").note.loading");
        saveAndCheckNotes(form, items, "-", "-", "f2");
        close(form);
    }

    // a classic custom view's batch: the changeProperty calls made in it go in one request, so do the deprecated
    // changeProperties' and the calls of a batch begun in it, made in their order; a lookup in it is refused - its
    // failure callback called -, and a function returning a Promise is refused, what it made sent; a changeProperty
    // outside a batch goes alone (#1875)
    @Test
    public void aClassicViewsBatch() throws Exception {
        String[] items = seed();
        String stamp = String.valueOf(System.nanoTime());
        Locator form = open(FOLDER, "UiReactBatch.uiRbClassic");
        classic("list.length === 3");
        List<String> requests = requests();
        classicCall("controller.batch(() => { controller.changeProperty('note', row(" + js(items[0]) + "), 'a" + stamp + "');"
                + " controller.changeProperty('note', row(" + js(items[1]) + "), 'b" + stamp + "'); })");
        classic("row(" + js(items[0]) + ").note === 'a" + stamp + "' && row(" + js(items[1]) + ").note === 'b" + stamp + "'");
        assertEquals(1, count(requests, "ExecuteBatch"));
        assertEquals(0, requests.stream().filter(request -> !request.contains(signature("ExecuteBatch")) && request.contains(signature("ExecuteEventAction"))).count());
        saveAndCheckNotes(form, items, "a" + stamp, "b" + stamp, "-");

        requests = requests();
        classicCall("controller.changeProperties(['note', 'note', 'nosuch'], [row(" + js(items[1]) + "), row(" + js(items[2]) + "), row(" + js(items[2]) + ")],"
                + " ['c" + stamp + "', 'd" + stamp + "', 'x'])");
        classic("row(" + js(items[1]) + ").note === 'c" + stamp + "' && row(" + js(items[2]) + ").note === 'd" + stamp + "'");
        assertEquals(1, count(requests, "ExecuteBatch"));
        saveAndCheckNotes(form, items, "a" + stamp, "c" + stamp, "d" + stamp);

        requests = requests();
        classicCall("controller.batch(() => { controller.changeProperty('note', row(" + js(items[2]) + "), 'n1" + stamp + "');"
                + " controller.changeProperties(['note'], [row(" + js(items[2]) + ")], ['n2" + stamp + "']); })");
        classic("row(" + js(items[2]) + ").note === 'n2" + stamp + "'");
        assertEquals(1, count(requests, "ExecuteBatch"));
        saveAndCheckNotes(form, items, "a" + stamp, "c" + stamp, "n2" + stamp);

        requests = requests();
        classicCall("window.uiRbLookup = 'pending'; controller.batch(() => { controller.changeProperty('note', row(" + js(items[1]) + "), 'g" + stamp + "');"
                + " controller.getPropertyValues('note', '', () => window.uiRbLookup = 'made', () => window.uiRbLookup = 'failed'); })");
        classic("row(" + js(items[1]) + ").note === 'g" + stamp + "'");
        assertEquals("failed", page.evaluate("() => window.uiRbLookup"));
        assertEquals(0, requests.stream().filter(request -> request.contains(signature("GetAsyncValues")) || request.contains(signature("GetPriorityAsyncValues"))).count());
        assertEquals(1, count(requests, "ExecuteBatch"));
        saveAndCheckNotes(form, items, "a" + stamp, "g" + stamp, "n2" + stamp);

        requests = requests();
        String promise = (String) page.evaluate("() => { const controller = window.uiRbClassic.controller, row = " + ROW + ";"
                + " try { controller.batch(() => { controller.changeProperty('note', row(" + js(items[0]) + "), 'p" + stamp + "'); return {then: () => {}}; });"
                + " return 'not refused'; } catch (e) { return e.message; } }");
        assertEquals("controller.batch(): the function returned a Promise: a batch is sent when it returns, so what follows an await in it would be"
                + " made outside it. Await before the batch", promise);
        classic("row(" + js(items[0]) + ").note === 'p" + stamp + "'");
        assertEquals(1, count(requests, "ExecuteBatch"));
        saveAndCheckNotes(form, items, "p" + stamp, "g" + stamp, "n2" + stamp);

        requests = requests();
        classicCall("controller.changeProperty('note', row(" + js(items[0]) + "), 'e" + stamp + "')");
        classic("row(" + js(items[0]) + ").note === 'e" + stamp + "'");
        assertEquals(0, count(requests, "ExecuteBatch"));
        assertEquals(1, count(requests, "ExecuteEventAction"));
        saveAndCheckNotes(form, items, "e" + stamp, "g" + stamp, "n2" + stamp);
        close(form);
    }

    // against the contract: a request a batch does not carry - a classic view's view filter set in its batch - fails
    // the batch on the server, which the client says; the change before it, made in the form's session, comes with the
    // answer to the next one, and the form goes on (#1875)
    @Test
    public void aRequestABatchDoesNotCarryFailsIt() throws Exception {
        String[] items = seed();
        String stamp = String.valueOf(System.nanoTime());
        Locator form = open(FOLDER, "UiReactBatch.uiRbClassic");
        classic("list.length === 3");
        List<String> requests = requests();
        classicCall("controller.batch(() => { controller.changeProperty('note', row(" + js(items[2]) + "), 'u" + stamp + "');"
                + " controller.setBooleanViewFilter('name', 1000); })");
        Locator error = page.locator(".modal-content").filter(new Locator.FilterOptions().setHasText("SetViewFilters is no request a batch carries"));
        assertThat(error).isVisible();
        assertEquals(1, requests.stream().filter(request -> request.contains(signature("ExecuteBatch")) && request.contains(signature("SetViewFilters"))).count());
        assertEquals(0, requests.stream().filter(request -> !request.contains(signature("ExecuteBatch")) && (request.contains(signature("SetViewFilters"))
                || request.contains(signature("ExecuteEventAction")))).count());
        error.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Close").setExact(true)).click();
        classicCall("controller.changeProperty('note', row(" + js(items[0]) + "), 'v" + stamp + "')");
        classic("row(" + js(items[0]) + ").note === 'v" + stamp + "' && row(" + js(items[2]) + ").note === 'u" + stamp + "' && list.length === 3");
        saveAndCheckNotes(form, items, "v" + stamp, "-", "u" + stamp);
        close(form);
    }

    // the form's changes saved - a form with changes in its session asks before it closes
    private static void save(Locator form) {
        form.locator("button[lsfusion-container='PROPERTY(formApply())']").click();
    }

    // ... and every item's note then as the server has it, a dash for none: a value a view shows before the server's
    // answer would not tell a change the server never made, or made in another row
    private static void saveAndCheckNotes(Locator form, String[] items, String first, String second, String third) throws Exception {
        save(form);
        check("UiReactBatch.uiRbCheckNotes", items[0] + ":" + first + ";" + items[1] + ":" + second + ";" + items[2] + ":" + third);
    }

    // three items under names no other attempt has - what a failed attempt saved stays in the database for its rerun -,
    // in this order: their sums 10, 30, 20, their weights 3, 1, 2, their quantities 1, 2, 3
    private static String[] seed() throws Exception {
        long id = System.nanoTime();
        String[] items = {"Alpha" + id, "Beta" + id, "Gamma" + id};
        exec("UiReactBatch.uiRbSeed", items);
        return items;
    }

    // the bodies of the requests the page posts from now on - GWT-RPC, naming each request's kind
    private List<String> requests() {
        List<String> requests = new CopyOnWriteArrayList<>();
        page.onRequest(request -> {
            if (request.method().equals("POST") && request.postData() != null)
                requests.add(request.postData());
        });
        return requests;
    }

    // ... the requests of a kind among them, by its GWT-RPC type signature: the client's report of an error names a
    // request's kind in its text
    private static long count(List<String> requests, String kind) {
        return requests.stream().filter(request -> request.contains(signature(kind))).count();
    }

    private static String signature(String kind) {
        return "action.form." + kind + "/";
    }

    // the errors the page throws from now on
    private List<String> errors() {
        List<String> errors = new CopyOnWriteArrayList<>();
        page.onPageError(error -> {
            if (!PUSH_API.matcher(error).find())
                errors.add(error);
        });
        return errors;
    }

    // a row the board draws, by its name
    private static Locator boardRow(Locator form, String name) {
        return form.locator(".ui-rb-row").filter(new Locator.FilterOptions().setHas(form.page().locator(".ui-rb-name")
                .filter(new Locator.FilterOptions().setHasText(Pattern.compile("^" + name + "$")))));
    }

    // a string as a JavaScript literal: the names are letters and digits
    private static String js(String text) {
        return "'" + text + "'";
    }

    // what a React view was last drawn with, its data and its controller, as a script names them; a row of the group by
    // its name, the names of the rows, the current one's
    private static String scope(String view) {
        return "const view = window.uiRbViews && window.uiRbViews." + view + ", data = view && view.data, controller = view && view.controller;"
                + " const row = name => data.i.list.find(r => r.name.value === name);"
                + " const names = () => data.i.list.map(r => r.name.value).join();"
                + " const current = () => (data.i.list.find(r => r.isCurrent) || {name: {value: null}}).name.value;";
    }

    // until a condition holds of what a view is drawn with, failing with what it is drawn with then
    private void waitFor(String view, String condition) {
        try {
            page.waitForFunction("() => { " + scope(view) + " try { return !!data && !!(" + condition + "); } catch (e) { return false; } }",
                    null, new Page.WaitForFunctionOptions().setTimeout(10_000));
        } catch (PlaywrightException e) {
            throw new AssertionError("never " + condition + " - the view is drawn with "
                    + page.evaluate("() => JSON.stringify(window.uiRbViews && window.uiRbViews." + view + " && window.uiRbViews." + view + ".data)"), e);
        }
    }

    // what an expression over what a view is drawn with comes to - the answer of a Promise -, as a string
    private String eval(String view, String expression) {
        return (String) page.evaluate("async () => { " + scope(view) + " return String(await (" + expression + ")); }");
    }

    // a call of a view's controller, made as the view's own code makes it
    private void call(String view, String call) {
        page.evaluate("() => { " + scope(view) + " " + call + "; }");
    }

    // the reason a call of a view's controller is refused with: thrown, or its Promise rejected with it
    private String refusal(String view, String call) {
        return (String) page.evaluate("async () => { " + scope(view) + " try { await (" + call + "); return 'not refused'; } catch (e) { return e.message; } }");
    }

    // the classic view's rows, a row by its name
    private static final String ROW = "name => window.uiRbClassic.list.find(r => r.name === name)";

    // until a condition holds of the classic view's rows (list, row), failing with them
    private void classic(String condition) {
        try {
            page.waitForFunction("() => { const list = window.uiRbClassic && window.uiRbClassic.list, row = " + ROW + ";"
                    + " try { return !!list && !!(" + condition + "); } catch (e) { return false; } }", null, new Page.WaitForFunctionOptions().setTimeout(10_000));
        } catch (PlaywrightException e) {
            throw new AssertionError("never " + condition + " - the classic view has " + page.evaluate("() => JSON.stringify(window.uiRbClassic && window.uiRbClassic.list)"), e);
        }
    }

    // a call of the classic view's controller, made as the view's own code makes it
    private void classicCall(String call) {
        page.evaluate("() => { const controller = window.uiRbClassic.controller, row = " + ROW + "; " + call + "; }");
    }
}
