package lsfusion.tests;

import com.microsoft.playwright.Locator;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.Assert.assertEquals;

// the members of a table's chrome a React view draws, called in controller.batch (UiReactBatchChrome.lsf, its view
// UiReactBatchChrome.js): a toolbar's calls that ask the server refused, the client's own made at once, the condition
// they start on a property the form follows the focus of started after the batch; a tree's members and a filter group's
// going with the batch (#1884)
public class UiReactBatchChromeIT extends UiTest {

    private static final String FOLDER = "UiReactBatchChrome.uiRbc";
    private static final String REFUSAL = "a batch is running, which carries changes only: make this call before the batch or after it";

    // a toolbar's calls that ask the server, in a batch: the count's and a sum's Promises rejected with the reason, the
    // export, the manual update, the update and the view switch refused, the reason on the console, nothing of them
    // made; after the batch the count, the sum, the view switch and the manual update are made - the export and the
    // update outside a batch are UiReactToolbarIT's (#1884)
    @Test
    public void theToolbarsServerCallsInABatch() throws Exception {
        String[] items = items(seed());
        Locator form = open(FOLDER, "UiReactBatchChrome.uiRbcHeader");
        view.waitFor("data.i.listView === 'grid'");
        assertThat(names(form)).hasText(items);
        assertEquals("controller.i.getCount(): " + REFUSAL, view.drawn().rejection("(() => { let count; controller.batch(() => { count = controller.i.getCount(); }); return count; })()"));
        assertEquals("controller.i.getSum(): " + REFUSAL, view.drawn().rejection("(() => { let sum; controller.batch(() => { sum = controller.i.getSum('price'); }); return sum; })()"));
        assertEquals("2", view.drawn().answer("controller.i.getCount()"));

        List<String> console = console();
        view.drawn().call("controller.batch(() => { controller.i.manual.change(true); controller.i.update(); controller.i['export'](); controller.i.listView.change('pivot'); })");
        assertEquals(Arrays.asList("manual.change()", "update()", "export()", "listView.change()").stream()
                .map(call -> "controller.i." + call + ": " + REFUSAL).collect(Collectors.toList()), console);
        assertEquals("2", view.drawn().answer("controller.i.getCount()"));
        assertEquals("false,grid", view.drawn().eval("[data.i.manual, data.i.listView].join()"));
        assertThat(names(form)).hasText(items);
        assertEquals("2.25", view.drawn().answer("controller.i.getSum('price')"));
        view.drawn().call("controller.i.listView.change('pivot')");
        assertThat(form.locator(".pvtTable.bodytable")).isVisible();
        view.waitFor("data.i.listView === 'pivot'");
        view.drawn().call("controller.i.listView.change('grid')");
        assertThat(names(form)).hasText(items);
        view.waitFor("data.i.listView === 'grid'");
        view.drawn().call("controller.i.manual.change(true)");
        view.waitFor("data.i.manual === true");
        assertThat(names(form)).hasText(items);
        close(form);
    }

    // the filter button's member in a batch is the client's alone, made at once: the name - the form follows its focus
    // - current, the condition it starts on it takes the focus once the batch is sent, so the batch fails on no request
    // of the focus, and a value typed into it filters the grid; the panel's reset takes it off, hiding the controls
    // again (#1884)
    @Test
    public void theFilterButtonInABatchOverAPropertyTheFormFollowsTheFocusOf() throws Exception {
        String[] items = items(seed());
        Locator form = open(FOLDER, "UiReactBatchChrome.uiRbcHeader");
        view.waitFor("data.i.filterControlsVisible === false");
        cell(form, "name", items[0]).click();
        idle();
        view.drawn().call("controller.batch(() => controller.i.filterControlsVisible.change(true))");
        view.waitFor("data.i.filterControlsVisible === true");
        assertThat(button(form, "Add filter")).isVisible();
        assertThat(form.locator(".filter:visible")).hasCount(1);
        assertThat(form.locator(".filter:visible input")).isFocused();
        assertThat(form.locator(".filter:visible input")).hasValue(items[0]);
        form.locator(".filter:visible input").fill(items[1]);
        idle();
        page.keyboard().press("Enter");
        assertThat(names(form)).hasText(new String[] {items[1]});
        button(form, "Reset all filters").click();
        view.waitFor("data.i.filterControlsVisible === false");
        assertThat(names(form)).hasText(items);
        assertThat(page.locator(".modal-content")).hasCount(0);
        close(form);
    }

    // ... over a number drawn in a div, the form following the focus of its cell: the condition's editor, an input in
    // the div's place, starts once the batch is sent, holding the focus, the batch failing on no request of the focus,
    // and a value typed into it filters the grid (#1884)
    @Test
    public void theFilterButtonInABatchOverANumberInADiv() throws Exception {
        String[] items = items(seed());
        Locator form = open(FOLDER, "UiReactBatchChrome.uiRbcHeaderNumber");
        view.waitFor("data.i.filterControlsVisible === false");
        assertThat(names(form)).hasText(items);
        form.locator("td[lsfusion-container='PROPERTY(price)']").first().click();
        idle();
        view.drawn().call("controller.batch(() => controller.i.filterControlsVisible.change(true))");
        view.waitFor("data.i.filterControlsVisible === true");
        assertThat(form.locator(".filter:visible input")).isFocused();
        form.locator(".filter:visible input").fill("0.75");
        idle();
        page.keyboard().press("Enter");
        assertThat(names(form)).hasText(new String[] {items[1]});
        button(form, "Reset all filters").click();
        view.waitFor("data.i.filterControlsVisible === false");
        assertThat(names(form)).hasText(items);
        assertThat(page.locator(".modal-content")).hasCount(0);
        close(form);
    }

    // a tree's members in a batch, where a view draws the tree: its expandCurrent opens the node a change earlier in
    // the batch made current, and it alone; its add and its reset of the conditions and the top group's expandAll -
    // every category with children opened - go with the batch; its export is refused, the reason on the console; its
    // filter button's member is refused, with nothing of the platform's to show (#1884)
    @Test
    public void aTreesMembersInABatch() throws Exception {
        String[] names = seed();
        Locator form = open(FOLDER, "UiReactBatchChrome.uiRbcTreeBox");
        view.waitFor("data.c.list.length === 3");
        view.drawn().call("controller.c.change(data.c.list[2])");
        view.waitFor("data.c.list[2].isCurrent");
        view.drawn().call("controller.batch(() => { controller.c.change(data.c.list[0]); controller.cats.expandCurrent(); })");
        view.waitFor("opened() === '" + names[0] + "' && rows() === '" + names[2] + "," + names[3] + "' && loaded()");
        view.drawn().call("controller.batch(() => controller.cats.addFilter())");
        view.waitFor("conditions() === 'c.catName = " + names[0] + "' && data.c.list.map(r => r.catName.value).join() === '" + names[0] + "'");
        view.drawn().call("controller.batch(() => controller.cats.resetFilters())");
        view.waitFor("conditions() === '' && data.c.list.length === 3");
        view.drawn().call("controller.c.collapseAll()");
        view.waitFor("opened() === '' && rows() === ''");
        view.drawn().call("controller.batch(() => controller.c.expandAll())");
        view.waitFor("opened() === '" + names[0] + "," + names[4] + "' && rows() === '" + names[2] + "," + names[3] + "," + names[5] + "' && loaded()");

        List<String> console = console();
        view.drawn().call("controller.batch(() => controller.cats['export']())");
        assertEquals(Arrays.asList("controller.cats.export(): " + REFUSAL), console);
        assertEquals("controller.cats.filterControlsVisible.change(): has no filter controls of the platform's to show: the platform "
                + "draws neither the conditions nor FILTERCONTROLS here, or its toolbar turns its filters off (showFilters)",
                view.drawn().refusal("controller.batch(() => controller.cats.filterControlsVisible.change(true))"));
        assertEquals("false", view.drawn().eval("data.cats.filterControlsVisible"));
        close(form);
    }

    // the platform's tree's toolbar and filter controls a view draws, in a batch: expandCurrent opens the selected node
    // alone and expandTree the whole tree, the filter button's member shows the panel's editors with a condition
    // started, the reset takes it off, the tree as it was (#1884)
    @Test
    public void thePlatformsTreeToolbarInABatch() throws Exception {
        String[] names = seed();
        Locator form = open(FOLDER, "UiReactBatchChrome.uiRbcTreeHeader");
        String[] closed = {names[0], names[4], names[1]}, open = {names[0], names[2], names[3], names[4], names[5], names[1]};
        assertThat(tree(form)).hasText(closed);
        select(form.locator("td[lsfusion-container='PROPERTY(catName)']").filter(new Locator.FilterOptions().setHasText(names[4])));
        view.drawn().call("controller.batch(() => controller.cats.expandCurrent())");
        assertThat(tree(form)).hasText(new String[] {names[0], names[4], names[5], names[1]});
        toggle(form, names[4]);
        assertThat(tree(form)).hasText(closed);
        view.drawn().call("controller.batch(() => controller.cats.expandTree())");
        assertThat(tree(form)).hasText(open);
        view.drawn().call("controller.batch(() => controller.cats.filterControlsVisible.change(true))");
        assertThat(form.locator(".filter:visible")).hasCount(1);
        view.drawn().call("controller.batch(() => controller.cats.resetFilters())");
        assertThat(form.locator(".filter:visible")).hasCount(0);
        assertThat(tree(form)).hasText(open);
        close(form);
    }

    // a filter group's member in a batch selects its filter, another one, none (#1884)
    @Test
    public void aFilterGroupInABatch() throws Exception {
        String[] items = items(seed());
        Locator form = open(FOLDER, "UiReactBatchChrome.uiRbcFilterGroup");
        view.waitFor("data.i.list.length === 2");
        view.drawn().call("controller.batch(() => controller.i.uiRbcKind.change(0))");
        view.waitFor("data.i.uiRbcKind.value === 0 && rows() === '" + items[0] + "'");
        view.drawn().call("controller.batch(() => controller.i.uiRbcKind.change(1))");
        view.waitFor("data.i.uiRbcKind.value === 1 && rows() === '" + items[1] + "'");
        view.drawn().call("controller.batch(() => controller.i.uiRbcKind.change(null))");
        view.waitFor("data.i.uiRbcKind.value === null && rows() === '" + items[0] + "," + items[1] + "'");
        close(form);
    }

    // three categories under names no other attempt has - what a failed attempt saved stays in the database for its
    // rerun -, by name the full one with two items in it, the dearer first by name, another with one item, which only a
    // tree shows, and the empty one: the full category, the empty one, the dearer item, the cheaper one, the other
    // category, its item
    private static String[] seed() throws Exception {
        long id = System.nanoTime();
        String[] names = {"Fruit" + id, "Veg" + id, "Apple" + id, "Banana" + id, "Nuts" + id, "Walnut" + id};
        exec("UiReactBatchChrome.uiRbcSeed", names);
        return names;
    }

    // ... the items alone
    private static String[] items(String[] names) {
        return new String[] {names[2], names[3]};
    }

    // the names in the platform's grid
    private static Locator names(Locator form) {
        return form.locator("td[lsfusion-container='PROPERTY(name)']");
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
        Locator icon = form.locator("tr").filter(new Locator.FilterOptions().setHas(page.locator("td[lsfusion-container='PROPERTY(catName)']")
                .filter(new Locator.FilterOptions().setHasText(category)))).locator("[__tree_node]");
        idle();
        icon.dblclick();
    }

    // the errors the page logs on the console from now on
    private List<String> console() {
        List<String> console = new CopyOnWriteArrayList<>();
        page.onConsoleMessage(message -> {
            if (message.type().equals("error") && message.text().contains(REFUSAL))
                console.add(message.text());
        });
        return console;
    }

    // what the view was last drawn with, its data and its controller, as a script names them; the categories it is
    // given open, the tree's conditions, each as `property compare value`, the names of the rows of a group it draws,
    // and whether no category's children are being loaded
    private final View view = view("window.uiRbcView",
            " const opened = () => data.c.list.filter(r => r.expanded).map(r => r.catName.value).join();"
            + " const conditions = () => data.cats.filters.map(f => f.property + ' ' + f.compare + ' ' + f.value).join('; ');"
            + " const rows = () => data.i.list.map(r => r.name.value).join();"
            + " const loaded = () => data.c.list.every(r => !r.loading);");
}
