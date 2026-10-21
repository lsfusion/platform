package lsfusion.tests;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.PlaywrightException;
import com.microsoft.playwright.options.WaitForSelectorState;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.Assert.assertEquals;

// a tree's toolbar and filter controls (UiToolbarTree.lsf, its view UiToolbarTree.js), one for all the tree's groups:
// the platform's over the rows a React view draws, a view's - on the tree's node - over the rows it draws and over the
// platform's tree, and the platform's own over its own tree
public class UiToolbarTreeIT extends UiTest {

    private static final String FOLDER = "UiToolbarTree.uiTtr";
    private static final List<String> TREE_TOOLBAR = Arrays.asList("Show filter controls", "Expand current object", "Expand whole tree", "Export");

    // the platform's tree toolbar over the rows a view draws, which are the view's - their group has expandAll, the
    // toolbar's expandTree is not a member -: Expand whole tree opens every node with children, Expand current object
    // the category the view made current alone, and nothing with an item current; the filter button starts a condition
    // on the first column of the current node's group, which applied filters the view's rows, the reset taking it off
    @Test
    public void thePlatformsToolbarOverTheRowsAViewDraws() throws Exception {
        String[] names = seed();
        Locator form = open(FOLDER, "UiToolbarTree.uiTtrRows");
        Locator toolbar = form.locator("[lsfusion-container='TOOLBARSYSTEM(TREE cats)']");
        String all = names[2] + "," + names[3] + "," + names[5];
        waitFor("data.c.list.length === 3");
        assertEquals(TREE_TOOLBAR, titles(toolbar));
        assertEquals("true,false", eval("['expandAll' in controller.c, 'expandTree' in controller.c].join()"));

        button(toolbar, "Expand whole tree").click();
        waitFor("opened() === '" + names[0] + "," + names[4] + "' && items() === '" + all + "' && loaded()");
        call("controller.c.collapseAll()");
        waitFor("opened() === '' && items() === ''");
        call("controller.c.change(data.c.list[1])");
        waitFor("data.c.list[1].isCurrent");
        button(toolbar, "Expand current object").click();
        waitFor("opened() === '" + names[4] + "' && items() === '" + names[5] + "' && loaded()");
        call("controller.i.change(data.i.list[0])");
        waitFor("data.i.list[0].isCurrent");
        button(toolbar, "Expand current object").click();
        settled();
        assertEquals(names[4] + "/" + names[5], eval("opened() + '/' + items()"));

        call("controller.c.change(data.c.list[0])");
        waitFor("data.c.list[0].isCurrent");
        button(toolbar, "Expand whole tree").click();
        waitFor("opened() === '" + names[0] + "," + names[4] + "' && items() === '" + all + "' && loaded()");
        call("controller.i.change(data.i.list[1])");
        waitFor("data.i.list[1].isCurrent");
        button(toolbar, "Show filter controls").click();
        Locator conditions = form.locator(".filter:visible"), editor = conditions.locator("input");
        assertThat(conditions).hasCount(1);
        assertThat(editor).hasValue(names[3]);
        press(editor, "Enter");
        waitFor("items() === '" + names[3] + "'");
        button(form, "Reset all filters").click();
        waitFor("items() === '" + all + "'");
        close(form);
    }

    // a view drawing the whole tree has its toolbar, filter controls and conditions on the tree's node - none of them
    // on its groups' -, with the tree's own expandCurrent and no expandTree, the top group's expandAll opening it all,
    // and no toolbar of the platform's; expandCurrent opens the current category alone; the add puts in the condition
    // of the current node's first column, named with its group, a condition of another group merges into the tree's
    // list, the reset clears it; the filter button's member is refused, with nothing of the platform's to show
    @Test
    public void theWholeTreeAViewDraws() throws Exception {
        String[] names = seed();
        Locator form = open(FOLDER, "UiToolbarTree.uiTtrBox");
        String members = "['filterControlsVisible', 'expandCurrent', 'export', 'addFilter', 'applyFilters', 'resetFilters', 'filters']";
        waitFor("data.c.list.length === 3");
        assertEquals("true,false,false", eval("[" + members + ".every(m => m in controller.cats), 'expandTree' in controller.cats,"
                + " " + members + ".concat(['expandTree']).some(m => m in controller.c || m in controller.i)].join()"));
        assertEquals("false,false,false,0", eval("[data.cats.filterControlsVisible, data.cats.filtersManual, data.cats.filtersPending,"
                + " data.cats.filters.length].join()"));
        assertThat(form.getByTitle("Expand whole tree", new Locator.GetByTitleOptions().setExact(true))).hasCount(0);

        call("controller.c.expandAll()");
        waitFor("opened() === '" + names[0] + "," + names[4] + "' && items() === '" + names[2] + "," + names[3] + "," + names[5] + "' && loaded()");
        call("controller.c.collapseAll()");
        waitFor("opened() === '' && items() === ''");
        call("controller.c.change(data.c.list[1])");
        waitFor("data.c.list[1].isCurrent");
        call("controller.cats.expandCurrent()");
        waitFor("opened() === '" + names[4] + "' && items() === '" + names[5] + "' && loaded()");

        call("controller.c.collapseAll()");
        waitFor("opened() === '' && items() === ''");
        call("controller.c.change(data.c.list[0])");
        waitFor("data.c.list[0].isCurrent");
        call("controller.cats.expandCurrent()");
        waitFor("opened() === '" + names[0] + "' && items() === '" + names[2] + "," + names[3] + "' && loaded()");
        call("controller.cats.addFilter()");
        waitFor("conditions() === 'c.catName = " + names[0] + "'");
        call("controller.cats.filters.change({property: 'i.price', compare: '>', value: 1})");
        waitFor("conditions() === 'c.catName = " + names[0] + "; i.price > 1' && items() === '" + names[2] + "'");
        call("controller.cats.resetFilters()");
        waitFor("conditions() === '' && items() === '" + names[2] + "," + names[3] + "'");
        assertEquals("controller.cats.filterControlsVisible.change(): has no filter controls of the platform's to show: the platform "
                + "draws neither the conditions nor FILTERCONTROLS here, or its toolbar turns its filters off (showFilters)",
                refusal("controller.cats.filterControlsVisible.change(true)"));
        close(form);
    }

    // a view drawing the platform's tree's toolbar and filter controls has them on the tree's node, with no node of a
    // group, the platform drawing no toolbar of its own: expandCurrent() opens the tree's selected node alone,
    // expandTree() all of it from the selected node's group down; the filter button's member shows the panel's editors
    // with a condition started, the reset takes the panel's conditions off and the add starts one
    @Test
    public void aToolbarAViewDrawsOverThePlatformsTree() throws Exception {
        String[] names = seed();
        Locator form = open(FOLDER, "UiToolbarTree.uiTtrHeader");
        String[] closed = {names[0], names[4], names[1]}, nuts = {names[0], names[4], names[5], names[1]};
        waitFor("!!data.cats");
        assertEquals("true,false,false", eval("[['filterControlsVisible', 'expandCurrent', 'expandTree', 'export', 'addFilter', 'applyFilters',"
                + " 'resetFilters'].every(m => m in controller.cats), 'c' in data, 'i' in data].join()"));
        assertThat(tree(form)).hasText(closed);
        assertThat(form.getByTitle("Expand whole tree", new Locator.GetByTitleOptions().setExact(true))).hasCount(0);

        select(cell(form, "catName", names[4]));
        call("controller.cats.expandCurrent()");
        assertThat(tree(form)).hasText(nuts);
        toggle(form, names[4]);
        assertThat(tree(form)).hasText(closed);
        call("controller.cats.expandTree()");
        assertThat(tree(form)).hasText(new String[] {names[0], names[2], names[3], names[4], names[5], names[1]});
        call("controller.cats.filterControlsVisible.change(true)");
        Locator conditions = form.locator(".filter:visible");
        assertThat(conditions).hasCount(1);
        waitFor("data.cats.filterControlsVisible === true");
        call("controller.cats.resetFilters()");
        assertThat(conditions).hasCount(0);
        call("controller.cats.addFilter()");
        assertThat(conditions).hasCount(1);
        call("controller.cats.resetFilters()");
        assertThat(conditions).hasCount(0);
        close(form);
    }

    // the platform's own tree toolbar: Expand current object opens nothing on an empty category; Expand whole tree
    // opens the tree from the current node's group down - an empty category current, the categories' -, the nodes'
    // icons close them, and Expand current object opens the current category alone
    @Test
    public void thePlatformsTreeToolbar() throws Exception {
        String[] names = seed();
        Locator form = open(FOLDER, "UiToolbarTree.uiTtrPlatform");
        Locator toolbar = form.locator("[lsfusion-container='TOOLBARSYSTEM(TREE cats)']");
        String[] closed = {names[0], names[4], names[1]};
        assertThat(tree(form)).hasText(closed);
        assertEquals(TREE_TOOLBAR, titles(toolbar));
        assertThat(icon(form, names[0])).hasClass(Pattern.compile("collapsed-image"));

        select(cell(form, "catName", names[1]));
        button(toolbar, "Expand current object").click();
        settled();
        assertThat(tree(form)).hasText(closed);
        button(toolbar, "Expand whole tree").click();
        assertThat(tree(form)).hasText(new String[] {names[0], names[2], names[3], names[4], names[5], names[1]});
        toggle(form, names[0]);
        toggle(form, names[4]);
        assertThat(tree(form)).hasText(closed);
        select(cell(form, "catName", names[0]));
        button(toolbar, "Expand current object").click();
        assertThat(tree(form)).hasText(new String[] {names[0], names[2], names[3], names[4], names[1]});
        close(form);
    }

    // the platform's own tree filter panel: the filter button starts a condition on the current node's group - an
    // item's, its name -, a value typed replacing the cell's, which applied leaves that item alone under its category;
    // hidden, the controls leave it shown and applied, shown again they start none; the reset takes it off, hiding the
    // controls; a condition the server states (FILTER) is applied and shown on the panel - its property, comparison and
    // value -, and taken off again
    @Test
    public void thePlatformsTreeFilterPanel() throws Exception {
        String[] names = seed();
        Locator form = open(FOLDER, "UiToolbarTree.uiTtrPlatform");
        Locator toolbar = form.locator("[lsfusion-container='TOOLBARSYSTEM(TREE cats)']");
        Locator conditions = form.locator(".filter:visible"), editor = conditions.locator("input");
        String[] all = {names[0], names[2], names[3], names[4], names[5], names[1]}, banana = {names[0], names[3], names[4], names[1]},
                cheap = {names[0], names[3], names[4], names[5], names[1]};
        button(toolbar, "Expand whole tree").click();
        assertThat(tree(form)).hasText(all);

        cell(form, "name", names[2]).click();
        button(toolbar, "Show filter controls").click();
        assertThat(conditions).hasCount(1);
        assertThat(editor).isFocused();
        assertThat(editor).hasValue(names[2]);
        editor.fill(names[3]);
        press(editor, "Enter");
        assertThat(tree(form)).hasText(banana);
        button(toolbar, "Hide filter controls").click();
        assertThat(button(form, "Add filter")).isHidden();
        assertThat(conditions).hasCount(1);
        assertThat(tree(form)).hasText(banana);
        button(toolbar, "Show filter controls").click();
        assertThat(button(form, "Add filter")).isVisible();
        assertThat(conditions).hasCount(1);
        button(form, "Reset all filters").click();
        assertThat(tree(form)).hasText(all);
        assertThat(conditions).hasCount(0);
        assertThat(button(toolbar, "Show filter controls")).isVisible();

        form.locator("button[lsfusion-container='PROPERTY(uiTtrFilterCheap())']").click();
        assertThat(tree(form)).hasText(cheap);
        assertThat(conditions).hasCount(1);
        assertThat(conditions.locator(".filter-label:visible")).hasText(new String[] {"Price"});
        assertThat(conditions.locator(".filter-selector:visible")).hasText(new String[] {"<"});
        assertThat(conditions.locator(".filter-data-property-value")).hasText("1");
        form.locator("button[lsfusion-container='PROPERTY(uiTtrFilterNone())']").click();
        assertThat(tree(form)).hasText(all);
        assertThat(conditions).hasCount(0);
        close(form);
    }

    // three categories under names no other attempt has - what a failed attempt saved stays in the database for its
    // rerun -, by name the full one with two items in it, the dearer first by name, another with one cheap item, and
    // the empty one: the full category, the empty one, the dearer item, the cheaper one, the other category, its item
    private static String[] seed() throws Exception {
        long id = System.nanoTime();
        String[] names = {"Fruit" + id, "Veg" + id, "Apple" + id, "Banana" + id, "Nuts" + id, "Walnut" + id};
        exec("UiToolbarTree.uiTtrSeed", names);
        return names;
    }

    // the rows of the platform's tree, top down, each by its category's or its item's name
    private static Locator tree(Locator form) {
        return form.locator("[lsfusion-container='GRID(TREE cats)'] tbody tr td[lsfusion-container='PROPERTY(catName)'],"
                + " [lsfusion-container='GRID(TREE cats)'] tbody tr td[lsfusion-container='PROPERTY(name)']").filter(
                new Locator.FilterOptions().setHasText(Pattern.compile("\\S")));
    }

    // the platform's tree node of a category opened or closed by a double click on its icon: a press on the icon in the
    // selected cell toggles the node, the double click's second press then let go, and a press in another cell only
    // selects it, the second press toggling the node
    private void toggle(Locator form, String category) {
        Locator icon = icon(form, category);
        idle();
        answered(notCurrent(icon) ? 1 : 0, icon::dblclick);
    }

    // the expand icon of the platform's tree node showing a category
    private static Locator icon(Locator form, String category) {
        return form.locator("tr").filter(new Locator.FilterOptions().setHas(form.page().locator("td[lsfusion-container='PROPERTY(catName)']")
                .filter(new Locator.FilterOptions().setHasText(category)))).locator("[__tree_node]");
    }

    // the titles of a toolbar's buttons, in their order
    @SuppressWarnings("unchecked")
    private static List<String> titles(Locator toolbar) {
        return (List<String>) toolbar.locator("button:visible").evaluateAll("buttons => buttons.map(b => b.title)");
    }

    // a button of the platform's toolbar or filter controls, by its title
    private static Locator button(Locator scope, String title) {
        return scope.getByTitle(title, new Locator.GetByTitleOptions().setExact(true));
    }

    // a key into a condition's editor once no request is in flight: the client drops the keys typed meanwhile
    private void press(Locator editor, String key) {
        idle();
        editor.press(key);
    }

    private void idle() {
        page.locator(".lsf-blocking").waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.HIDDEN));
    }

    // the page's requests in flight, counted as they are sent and once the handlers of their answers have run - what an
    // answer brings is applied there -, and the changes of a current object among them (window.uiTtrRequests); the
    // glass the client puts over the page shows for some requests only
    private static final String REQUESTS = "(() => { if (window.uiTtrRequests) return; const requests = window.uiTtrRequests = {pending: 0, changes: 0};"
            + " const send = XMLHttpRequest.prototype.send; XMLHttpRequest.prototype.send = function (body) { requests.pending++;"
            + " if (String(body).includes('action.form.ChangeGroupObject/')) requests.changes++;"
            + " this.addEventListener('loadend', () => setTimeout(() => requests.pending--)); return send.apply(this, arguments); }; })()";

    @Before
    public void countRequests() {
        page.addInitScript(REQUESTS);
        page.evaluate(REQUESTS);
    }

    // until no request of the page is in flight, the answers applied
    private void settled() {
        answered(0, () -> { });
    }

    // a cell of the platform's tree clicked, and the change of the current object it makes on the server, a cell of a
    // row not current, answered
    private void select(Locator cell) {
        answered(notCurrent(cell) ? 1 : 0, cell::click);
    }

    // a step, the changes of a current object it makes - each sent a little after it, with no glass over the page
    // (GFormController.changeGroupObjectLater) - sent, and every request of the page then answered and applied
    private void answered(int changes, Runnable step) {
        int before = ((Number) page.evaluate("() => window.uiTtrRequests.changes")).intValue();
        step.run();
        page.waitForFunction("before => window.uiTtrRequests.changes >= before && window.uiTtrRequests.pending === 0", before + changes);
    }

    // whether an element is in a row of the platform's tree that is not the current one
    private static boolean notCurrent(Locator element) {
        return Boolean.TRUE.equals(element.evaluate("element => !element.closest('tr').classList.contains('table-active')"));
    }

    // what the view was last drawn with, its data and its controller, as a script names them; the categories it is
    // given open, the items it is given, by name, whether no category's children are being loaded, and the tree's
    // conditions, each as `property compare value`
    private static final String VIEW = "const view = window.uiTtrView, data = view && view.data, controller = view && view.controller;"
            + " const opened = () => data.c.list.filter(r => r.expanded).map(r => r.catName.value).join();"
            + " const items = () => data.i.list.map(r => r.name.value).join();"
            + " const loaded = () => data.c.list.every(r => !r.loading);"
            + " const conditions = () => data.cats.filters.map(f => f.property + ' ' + f.compare + ' ' + f.value).join('; ');";

    // until a condition holds of what the view is drawn with, failing with what it is drawn with then
    private void waitFor(String condition) {
        try {
            page.waitForFunction("() => { " + VIEW + " try { return !!data && !!(" + condition + "); } catch (e) { return false; } }",
                    null, new Page.WaitForFunctionOptions().setTimeout(10_000));
        } catch (PlaywrightException e) {
            throw new AssertionError("never " + condition + " - the view is drawn with " + page.evaluate("() => JSON.stringify(window.uiTtrView && window.uiTtrView.data)"), e);
        }
    }

    // the view drawn: what follows asks what it is drawn with
    private void drawn() {
        waitFor("true");
    }

    // what an expression over what the view is drawn with comes to, as a string
    private String eval(String expression) {
        drawn();
        return (String) page.evaluate("async () => { " + VIEW + " return String(await (" + expression + ")); }");
    }

    // a call of the view's controller, made as the view's own code makes it
    private void call(String call) {
        drawn();
        page.evaluate("() => { " + VIEW + " " + call + "; }");
    }

    // the reason a call of the controller is refused with
    private String refusal(String call) {
        drawn();
        return (String) page.evaluate("async () => { " + VIEW + " try { await (" + call + "); return 'not refused'; } catch (e) { return e.message; } }");
    }
}
