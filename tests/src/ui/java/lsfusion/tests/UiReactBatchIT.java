package lsfusion.tests;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Route;
import com.microsoft.playwright.options.AriaRole;
import org.junit.Test;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Pattern;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

// a React view's controller.batch (UiReactBatch.lsf, its views UiReactBatch.js): the member changes made in it shown at
// once and sent together in one request, which the server makes in their order - one that throws sending what it made
// before, one carrying changes only, one the server fails taking the values back -, a classic custom view's batch; what
// the platform draws in a React view, with an entry and no member (#1875); and the Promise a member's call returns - of
// its change's request, of a batch's, of its lookup's answers
public class UiReactBatchIT extends UiTest {

    private static final String FOLDER = "UiReactBatch.uiRb";
    private static final String REFUSAL = "a batch is running, which carries changes only: make this call before the batch or after it";

    // what the platform draws in a React view has an entry and no member - a panel action its entry saying only whether
    // it is shown, an action in each row its column's entry with what it is and no caption, a property its column's
    // entry labelling it and no cell in the rows, a property of the form its entry -, while what React draws has its
    // member; a view placing only a property the platform draws of a group has the group's data and an empty member of
    // it (#1875)
    @Test
    public void whatThePlatformDrawsHasAnEntryAndNoMember() throws Exception {
        String[] items = seed();
        Locator form = open(FOLDER, "UiReactBatch.uiRbShape");
        view("board").waitFor("names() === '" + String.join(",", items) + "'");
        assertEquals("function,function,function", view("board").eval("[typeof controller.i.note.change, typeof controller.i.incQty.exec,"
                + " typeof controller.toggleApprove.exec].join()"));
        assertEquals("/undefined", view("board").eval("Object.keys(data.i.approve).join() + '/' + typeof controller.i.approve"));
        assertEquals("false,true,true,undefined", view("board").eval("['caption' in data.i.bump, 'type' in data.i.bump, 'noSort' in data.i.bump,"
                + " typeof controller.i.bump].join()"));
        assertEquals("Qty,false,undefined", view("board").eval("[data.i.qty.caption, 'qty' in row(" + js(items[1]) + "), typeof controller.i.qty].join()"));
        assertEquals("Comment,undefined", view("board").eval("[data.comment.caption, typeof controller.comment].join()"));
        assertEquals("function,function,function", view("board").eval("[typeof controller.form.exec, typeof controller.form.change, typeof controller.batch].join()"));

        assertEquals("Note,false,object,0,function", view("side").eval("[data.i.sideNote.caption, 'list' in data.i, typeof controller.i,"
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
        view("board").waitFor("names() === '" + String.join(",", items) + "'");
        view("board").call("controller.i.incQty.exec(row(" + js(items[1]) + "))");
        view("board").waitFor("row(" + js(items[1]) + ").qtyEcho.value === 12");
        assertThat(boardRow(form, items[1]).locator(".ui-rb-qty")).hasText("12");

        view("board").call("controller.i.change(row(" + js(items[2]) + "))");
        view("board").waitFor("current() === " + js(items[2]));
        form.locator(".ui-rb-approve button").click();
        view("board").waitFor("row(" + js(items[2]) + ").approvals.value === 1");
        boardRow(form, items[0]).locator(".ui-rb-bump button").click();
        view("board").waitFor("row(" + js(items[0]) + ").bumps.value === 1");
        view("board").waitFor("row(" + js(items[2]) + ").bumps.value === null && row(" + js(items[2]) + ").approvals.value === 1");

        Locator approve = form.locator(".ui-rb-approve > .lsf-platform");
        view("board").call("controller.toggleApprove.exec()");
        view("board").waitFor("data.i.approve.hidden === true");
        assertThat(approve).isHidden();
        view("board").call("controller.toggleApprove.exec()");
        view("board").waitFor("!('hidden' in data.i.approve)");
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
        Locator form = open(FOLDER, "UiReactBatch.uiRbShape");
        view("board").waitFor("names() === '" + String.join(",", items) + "'");
        view("board").call("controller.i.change(row(" + js(items[0]) + "))");
        view("board").waitFor("current() === " + js(items[0]));
        List<String> requests = requests(request -> request.method().equals("POST"));
        view("board").call("controller.batch(() => { controller.i.change(row(" + js(items[1]) + ")); controller.i.orders.change({property: 'sum', desc: true});"
                + " controller.i.note.change(" + js(note) + "); controller.i.incQty.exec(row(" + js(items[2]) + ")); })");
        view("board").waitFor("names() === '" + items[1] + "," + items[2] + "," + items[0] + "' && current() === " + js(items[1])
                + " && row(" + js(items[1]) + ").note.value === " + js(note) + " && row(" + js(items[2]) + ").qtyEcho.value === 13");
        assertEquals("[{\"property\":\"sum\",\"desc\":true}]", view("board").eval("JSON.stringify(data.i.orders)"));
        assertEquals("null", view("board").eval("row(" + js(items[0]) + ").note.value"));
        assertEquals(1, count(requests, "ExecuteBatch"));
        assertEquals(0, requests.stream().filter(request -> !request.contains(signature("ExecuteBatch")) && (request.contains(signature("ChangeGroupObject"))
                || request.contains(signature("SetPropertyOrders")) || request.contains(signature("ExecuteEventAction")))).count());
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
        view("board").waitFor("names() === '" + String.join(",", items) + "'");
        view("board").call("controller.i.change(row(" + js(items[1]) + "))");
        view("board").waitFor("current() === " + js(items[1]));
        List<String> requests = requests(request -> request.method().equals("POST"));
        assertEquals("stop", view("board").refusal("controller.batch(() => { controller.i.note.change(" + js(note) + "); throw new Error('stop'); })"));
        view("board").waitFor("row(" + js(items[1]) + ").note.value === " + js(note) + " && !row(" + js(items[1]) + ").note.loading");
        assertEquals(1, count(requests, "ExecuteBatch"));
        saveAndCheckNotes(form, items, "-", note, "-");
        close(form);
    }

    // a batch carries changes only: a server call in it is refused - its Promise rejected with the
    // reason -, so is a lookup, neither sent, the batch going on with the change made before them, in one request;
    // another view's batch begun in it is a part of it; outside a batch the server call is made (#1875)
    @Test
    public void aBatchCarriesChangesOnly() throws Exception {
        String[] items = seed();
        String note = "y" + System.nanoTime();
        Locator form = open(FOLDER, "UiReactBatch.uiRbShape");
        view("board").waitFor("names() === '" + String.join(",", items) + "'");
        view("board").call("controller.i.change(row(" + js(items[1]) + "))");
        view("board").waitFor("current() === " + js(items[1]));
        List<String> requests = requests(request -> request.method().equals("POST"));
        view("board").call("window.uiRbRefused = window.uiRbLookup = 'pending'; controller.batch(() => { controller.i.note.change('outer');"
                + " window.uiRbViews.side.controller.batch(() => controller.i.note.change(" + js(note) + "));"
                + " controller.form.exec('uiRbPing').then(() => window.uiRbRefused = 'made', e => window.uiRbRefused = e.message);"
                + " controller.i.note.getValues('').then(() => window.uiRbLookup = 'made', e => window.uiRbLookup = e.message); })");
        view("board").waitFor("window.uiRbRefused !== 'pending' && window.uiRbLookup !== 'pending'");
        assertEquals("controller.form.exec(): " + REFUSAL, view("board").eval("window.uiRbRefused"));
        assertEquals("controller.i.note.getValues(): " + REFUSAL, view("board").eval("window.uiRbLookup"));
        view("board").waitFor("row(" + js(items[1]) + ").note.value === " + js(note) + " && !row(" + js(items[1]) + ").note.loading");
        assertEquals(0, requests.stream().filter(request -> request.contains(signature("ControllerExecAction")) || request.contains(signature("GetAsyncValues"))
                || request.contains(signature("GetPriorityAsyncValues"))).count());
        assertEquals(1, count(requests, "ExecuteBatch"));

        assertEquals("ok", view("board").eval("controller.form.exec('uiRbPing').then(() => 'ok')"));
        saveAndCheckNotes(form, items, "-", note, "-");
        close(form);
    }

    // a property's change and exec, and a batch, return the Promise of their request: resolved when it is over - an
    // action's once the message it shows is closed -, a change made in a batch returning the batch's own Promise; an
    // action the server fails rejects it with the reason, which the platform shows too, so a Promise no one waits for
    // reports nothing more
    @Test
    public void aCallIsThePromiseOfItsRequest() throws Exception {
        String[] items = seed();
        Locator form = open(FOLDER, "UiReactBatch.uiRbShape");
        view("board").waitFor("names() === '" + String.join(",", items) + "'");
        assertEquals("resolved", view("board").eval("controller.i.note.change('p1', row(" + js(items[0]) + ")).then(() => 'resolved')"));
        assertEquals("resolved", view("board").eval("controller.i.incQty.exec(row(" + js(items[0]) + ")).then(() => 'resolved')"));
        assertEquals("resolved", view("board").eval("(() => { let made; const batch = controller.batch(() => { made = controller.i.note.change('p2', row("
                + js(items[1]) + ")); }); return made === batch ? batch.then(() => 'resolved') : 'another Promise'; })()"));

        view("board").call("window.uiRbTold = 'pending'; controller.i.tell.exec(row(" + js(items[2]) + ")).then(() => window.uiRbTold = 'over')");
        assertThat(dialog("told " + items[2])).isVisible();
        assertEquals("pending", view("board").eval("window.uiRbTold"));
        closeDialog("told " + items[2]);
        view("board").waitFor("window.uiRbTold === 'over'");

        String failed = view("board").eval("controller.crash.exec().then(() => 'resolved', e => e.message)");
        assertTrue(failed, failed.contains("uiRbCrash: the action failed"));
        closeDialog("uiRbCrash: the action failed");
        view("board").call("controller.crash.exec()"); // no one waits for its Promise
        closeDialog("uiRbCrash: the action failed");
        saveAndCheckNotes(form, items, "p1", "p2", "-");
        close(form);
    }

    // a classic view's changeProperty and batch return the Promise of their request too
    @Test
    public void aClassicViewsCallIsThePromiseOfItsRequest() throws Exception {
        String[] items = seed();
        Locator form = open(FOLDER, "UiReactBatch.uiRbClassic");
        classic.waitFor("list.length === 3");
        assertEquals("resolved", classic.eval("controller.changeProperty('note', row(" + js(items[0]) + "), 'c1').then(() => 'resolved')"));
        assertEquals("resolved", classic.eval("controller.batch(() => controller.changeProperty('note', row(" + js(items[1]) + "), 'c2'))"
                + ".then(() => 'resolved')"));
        classic.waitFor("row(" + js(items[0]) + ").note === 'c1' && row(" + js(items[1]) + ").note === 'c2'");
        saveAndCheckNotes(form, items, "c1", "c2", "-");
        close(form);
    }

    // a member's getValues: the first lookup after a change the form holds gives the database's answer, with the Promise
    // of the session's in its `more`; the next lookup gives the session's at once, with no `more` - and the form's
    // controller's lookup by the property's name is the same
    @Test
    public void aLookupGivesTheDatabasesAnswerThenTheSessions() throws Exception {
        String[] items = seed();
        String stamp = String.valueOf(System.nanoTime());
        Locator form = open(FOLDER, "UiReactBatch.uiRbShape");
        view("board").waitFor("names() === '" + String.join(",", items) + "'");
        view("board").call("controller.i.note.change('Apple" + stamp + "', row(" + js(items[0]) + "))");
        view("board").waitFor("row(" + js(items[0]) + ").note.value === 'Apple" + stamp + "' && !row(" + js(items[0]) + ").note.loading");
        saveAndCheckNotes(form, items, "Apple" + stamp, "-", "-");
        view("board").call("controller.i.note.change('Apricot" + stamp + "', row(" + js(items[0]) + "))");
        view("board").waitFor("row(" + js(items[0]) + ").note.value === 'Apricot" + stamp + "' && !row(" + js(items[0]) + ").note.loading");
        String lookup = "controller.i.note.getValues('Ap').then(async answer => { const names = data => data.map(value => value.rawString).join();"
                + " return names(answer.data) + '|' + (answer.more ? names((await answer.more).data) : 'final'); })";
        assertEquals("Apple" + stamp + "|Apricot" + stamp, view("board").eval(lookup));
        assertEquals("Apricot" + stamp + "|final", view("board").eval(lookup));
        assertEquals("Apricot" + stamp + "|final", view("board").eval(lookup.replace("controller.i.note.getValues('Ap')", "controller.form.getValues('i.note', 'Ap')")));
        saveAndCheckNotes(form, items, "Apricot" + stamp, "-", "-");
        close(form);
    }

    // a sorting names a property of the group wherever it is drawn: a panel property the platform draws, a column the
    // platform draws per row, which has no member; the sortings list them by their names; an unknown name is refused
    @Test
    public void aSortingNamesAPropertyWhereverItIsDrawn() throws Exception {
        String[] items = seed();
        Locator form = open(FOLDER, "UiReactBatch.uiRbShape");
        view("board").waitFor("names() === '" + String.join(",", items) + "'");
        view("board").call("controller.i.orders.change([{property: 'weight', desc: true}])");
        view("board").waitFor("names() === '" + items[0] + "," + items[2] + "," + items[1] + "' && JSON.stringify(data.i.orders) === '[{\"property\":\"weight\",\"desc\":true}]'");
        view("board").call("controller.i.orders.change([{property: 'qty', desc: false}])");
        view("board").waitFor("names() === '" + String.join(",", items) + "' && JSON.stringify(data.i.orders) === '[{\"property\":\"qty\",\"desc\":false}]'");
        assertEquals("controller.i.orders.change(): unknown property 'nosuch' of object group 'i'",
                view("board").refusal("controller.i.orders.change({property: 'nosuch', desc: true})"));
        view("board").call("controller.i.orders.change([])");
        view("board").waitFor("data.i.orders.length === 0");
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
        view("board").waitFor("names() === '" + String.join(",", items) + "'");
        view("board").call("controller.i.note.change(" + js(before) + ", row(" + js(items[2]) + "))");
        view("board").waitFor("row(" + js(items[2]) + ").note.value === " + js(before) + " && !row(" + js(items[2]) + ").note.loading");
        view("board").call("controller.toggleFail.exec()"); // a request before the batch's
        // the batch's request held until the view shows its change
        List<Route> held = new CopyOnWriteArrayList<>();
        page.route("**/main/dispatch", route -> {
            if (held.isEmpty() && route.request().postData() != null && route.request().postData().contains(signature("ExecuteBatch")))
                held.add(route);
            else
                route.resume();
        });
        view("board").call("controller.batch(() => { controller.i.orders.change({property: 'sum', desc: true});"
                + " controller.i.note.change('f1', row(" + js(items[2]) + ")); controller.i.note.change('f2', row(" + js(items[2]) + ")); })");
        view("board").waitFor("row(" + js(items[2]) + ").note.value === 'f2' && row(" + js(items[2]) + ").note.loading === true");
        page.waitForCondition(() -> !held.isEmpty());
        held.get(0).resume();
        page.unroute("**/main/dispatch");
        assertThat(dialog("uiRbFail: a sorting failed")).isVisible();
        view("board").waitFor("row(" + js(items[2]) + ").note.value === " + js(before) + " && !row(" + js(items[2]) + ").note.loading");
        closeDialog("uiRbFail: a sorting failed");
        view("board").call("controller.toggleFail.exec()"); // the next request, which the sorting fails no more
        view("board").waitFor("row(" + js(items[2]) + ").note.value === 'f2' && !row(" + js(items[2]) + ").note.loading");
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
        classic.waitFor("list.length === 3");
        List<String> requests = requests(request -> request.method().equals("POST"));
        classic.call("controller.batch(() => { controller.changeProperty('note', row(" + js(items[0]) + "), 'a" + stamp + "');"
                + " controller.changeProperty('note', row(" + js(items[1]) + "), 'b" + stamp + "'); })");
        classic.waitFor("row(" + js(items[0]) + ").note === 'a" + stamp + "' && row(" + js(items[1]) + ").note === 'b" + stamp + "'");
        assertEquals(1, count(requests, "ExecuteBatch"));
        assertEquals(0, requests.stream().filter(request -> !request.contains(signature("ExecuteBatch")) && request.contains(signature("ExecuteEventAction"))).count());
        saveAndCheckNotes(form, items, "a" + stamp, "b" + stamp, "-");

        requests = requests(request -> request.method().equals("POST"));
        classic.call("controller.changeProperties(['note', 'note', 'nosuch'], [row(" + js(items[1]) + "), row(" + js(items[2]) + "), row(" + js(items[2]) + ")],"
                + " ['c" + stamp + "', 'd" + stamp + "', 'x'])");
        classic.waitFor("row(" + js(items[1]) + ").note === 'c" + stamp + "' && row(" + js(items[2]) + ").note === 'd" + stamp + "'");
        assertEquals(1, count(requests, "ExecuteBatch"));
        saveAndCheckNotes(form, items, "a" + stamp, "c" + stamp, "d" + stamp);

        requests = requests(request -> request.method().equals("POST"));
        classic.call("controller.batch(() => { controller.changeProperty('note', row(" + js(items[2]) + "), 'n1" + stamp + "');"
                + " controller.changeProperties(['note'], [row(" + js(items[2]) + ")], ['n2" + stamp + "']); })");
        classic.waitFor("row(" + js(items[2]) + ").note === 'n2" + stamp + "'");
        assertEquals(1, count(requests, "ExecuteBatch"));
        saveAndCheckNotes(form, items, "a" + stamp, "c" + stamp, "n2" + stamp);

        requests = requests(request -> request.method().equals("POST"));
        classic.call("window.uiRbLookup = 'pending'; controller.batch(() => { controller.changeProperty('note', row(" + js(items[1]) + "), 'g" + stamp + "');"
                + " controller.getPropertyValues('note', '', () => window.uiRbLookup = 'made', () => window.uiRbLookup = 'failed'); })");
        classic.waitFor("row(" + js(items[1]) + ").note === 'g" + stamp + "'");
        assertEquals("failed", page.evaluate("() => window.uiRbLookup"));
        assertEquals(0, requests.stream().filter(request -> request.contains(signature("GetAsyncValues")) || request.contains(signature("GetPriorityAsyncValues"))).count());
        assertEquals(1, count(requests, "ExecuteBatch"));
        saveAndCheckNotes(form, items, "a" + stamp, "g" + stamp, "n2" + stamp);

        requests = requests(request -> request.method().equals("POST"));
        String promise = (String) page.evaluate("() => { const controller = window.uiRbClassic.controller, row = " + ROW + ";"
                + " try { controller.batch(() => { controller.changeProperty('note', row(" + js(items[0]) + "), 'p" + stamp + "'); return {then: () => {}}; });"
                + " return 'not refused'; } catch (e) { return e.message; } }");
        assertEquals("controller.batch(): the function returned a Promise: a batch is sent when it returns, so what follows an await in it would be"
                + " made outside it. Await before the batch", promise);
        classic.waitFor("row(" + js(items[0]) + ").note === 'p" + stamp + "'");
        assertEquals(1, count(requests, "ExecuteBatch"));
        saveAndCheckNotes(form, items, "p" + stamp, "g" + stamp, "n2" + stamp);

        requests = requests(request -> request.method().equals("POST"));
        classic.call("controller.changeProperty('note', row(" + js(items[0]) + "), 'e" + stamp + "')");
        classic.waitFor("row(" + js(items[0]) + ").note === 'e" + stamp + "'");
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
        classic.waitFor("list.length === 3");
        List<String> requests = requests(request -> request.method().equals("POST"));
        classic.call("controller.batch(() => { controller.changeProperty('note', row(" + js(items[2]) + "), 'u" + stamp + "');"
                + " controller.setBooleanViewFilter('name', 1000); })");
        assertThat(dialog("SetViewFilters is no request a batch carries")).isVisible();
        assertEquals(1, requests.stream().filter(request -> request.contains(signature("ExecuteBatch")) && request.contains(signature("SetViewFilters"))).count());
        assertEquals(0, requests.stream().filter(request -> !request.contains(signature("ExecuteBatch")) && (request.contains(signature("SetViewFilters"))
                || request.contains(signature("ExecuteEventAction")))).count());
        closeDialog("SetViewFilters is no request a batch carries");
        classic.call("controller.changeProperty('note', row(" + js(items[0]) + "), 'v" + stamp + "')");
        classic.waitFor("row(" + js(items[0]) + ").note === 'v" + stamp + "' && row(" + js(items[2]) + ").note === 'u" + stamp + "' && list.length === 3");
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

    // a dialog the platform shows, saying the text - a message, an error -, and it closed by its Close button
    private Locator dialog(String text) {
        return page.locator(".modal-content").filter(new Locator.FilterOptions().setHasText(text));
    }
    private void closeDialog(String text) {
        dialog(text).getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Close").setExact(true)).click();
        assertThat(dialog(text)).hasCount(0);
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

    private View view(String name) {
        return view("window.uiRbViews && window.uiRbViews." + name,
                " const row = name => data.i.list.find(r => r.name.value === name);"
                + " const names = () => data.i.list.map(r => r.name.value).join();"
                + " const current = () => (data.i.list.find(r => r.isCurrent) || {name: {value: null}}).name.value;");
    }

    // the classic view's rows, a row by its name
    private static final String ROW = "name => window.uiRbClassic.list.find(r => r.name === name)";
    private final View classic = new View("const list = window.uiRbClassic && window.uiRbClassic.list,"
            + " controller = window.uiRbClassic && window.uiRbClassic.controller, row = " + ROW + ";",
            "!!list", "the classic view has ", "window.uiRbClassic && window.uiRbClassic.list");
}
