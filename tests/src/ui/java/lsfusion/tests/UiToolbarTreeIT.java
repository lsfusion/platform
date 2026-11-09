package lsfusion.tests;

import com.microsoft.playwright.Locator;
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
        view.waitFor("data.c.list.length === 3");
        assertEquals(TREE_TOOLBAR, titles(toolbar));
        assertEquals("true,false", view.drawn().eval("['expandAll' in controller.c, 'expandTree' in controller.c].join()"));

        button(toolbar, "Expand whole tree").click();
        view.waitFor("opened() === '" + names[0] + "," + names[4] + "' && items() === '" + all + "' && loaded()");
        view.drawn().call("controller.c.collapseAll()");
        view.waitFor("opened() === '' && items() === ''");
        view.drawn().call("controller.c.change(data.c.list[1])");
        view.waitFor("data.c.list[1].current");
        button(toolbar, "Expand current object").click();
        view.waitFor("opened() === '" + names[4] + "' && items() === '" + names[5] + "' && loaded()");
        view.drawn().call("controller.i.change(data.i.list[0])");
        view.waitFor("data.i.list[0].current");
        button(toolbar, "Expand current object").click();
        settled();
        assertEquals(names[4] + "/" + names[5], view.drawn().eval("opened() + '/' + items()"));

        view.drawn().call("controller.c.change(data.c.list[0])");
        view.waitFor("data.c.list[0].current");
        button(toolbar, "Expand whole tree").click();
        view.waitFor("opened() === '" + names[0] + "," + names[4] + "' && items() === '" + all + "' && loaded()");
        view.drawn().call("controller.i.change(data.i.list[1])");
        view.waitFor("data.i.list[1].current");
        button(toolbar, "Show filter controls").click();
        Locator conditions = form.locator(".filter:visible"), editor = conditions.locator("input");
        assertThat(conditions).hasCount(1);
        assertThat(editor).hasValue(names[3]);
        press(editor, "Enter");
        view.waitFor("items() === '" + names[3] + "'");
        button(form, "Reset all filters").click();
        view.waitFor("items() === '" + all + "'");
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
        view.waitFor("data.c.list.length === 3");
        assertEquals("true,false,false", view.drawn().eval("[" + members + ".every(m => m in controller.cats), 'expandTree' in controller.cats,"
                + " " + members + ".concat(['expandTree']).some(m => m in controller.c || m in controller.i)].join()"));
        assertEquals("false,false,false,0", view.drawn().eval("[data.cats.filterControlsVisible, data.cats.filtersManual, data.cats.filtersPending,"
                + " data.cats.filters.length].join()"));
        assertThat(form.getByTitle("Expand whole tree", new Locator.GetByTitleOptions().setExact(true))).hasCount(0);

        view.drawn().call("controller.c.expandAll()");
        view.waitFor("opened() === '" + names[0] + "," + names[4] + "' && items() === '" + names[2] + "," + names[3] + "," + names[5] + "' && loaded()");
        view.drawn().call("controller.c.collapseAll()");
        view.waitFor("opened() === '' && items() === ''");
        view.drawn().call("controller.c.change(data.c.list[1])");
        view.waitFor("data.c.list[1].current");
        view.drawn().call("controller.cats.expandCurrent()");
        view.waitFor("opened() === '" + names[4] + "' && items() === '" + names[5] + "' && loaded()");

        view.drawn().call("controller.c.collapseAll()");
        view.waitFor("opened() === '' && items() === ''");
        view.drawn().call("controller.c.change(data.c.list[0])");
        view.waitFor("data.c.list[0].current");
        view.drawn().call("controller.cats.expandCurrent()");
        view.waitFor("opened() === '" + names[0] + "' && items() === '" + names[2] + "," + names[3] + "' && loaded()");
        view.drawn().call("controller.cats.addFilter()");
        view.waitFor("conditions() === 'c.catName = " + names[0] + "'");
        view.drawn().call("controller.cats.filters.change({property: 'i.price', compare: '>', value: 1})");
        view.waitFor("conditions() === 'c.catName = " + names[0] + "; i.price > 1' && items() === '" + names[2] + "'");
        view.drawn().call("controller.cats.resetFilters()");
        view.waitFor("conditions() === '' && items() === '" + names[2] + "," + names[3] + "'");
        assertEquals("controller.cats.filterControlsVisible.change(): has no filter controls of the platform's to show: the platform "
                + "draws neither the conditions nor FILTERCONTROLS here, or its toolbar turns its filters off (showFilters)",
                view.drawn().refusal("controller.cats.filterControlsVisible.change(true)"));
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
        view.waitFor("!!data.cats");
        assertEquals("true,false,false", view.drawn().eval("[['filterControlsVisible', 'expandCurrent', 'expandTree', 'export', 'addFilter', 'applyFilters',"
                + " 'resetFilters'].every(m => m in controller.cats), 'c' in data, 'i' in data].join()"));
        assertThat(tree(form)).hasText(closed);
        assertThat(form.getByTitle("Expand whole tree", new Locator.GetByTitleOptions().setExact(true))).hasCount(0);

        click(cell(form, "catName", names[4]));
        view.drawn().call("controller.cats.expandCurrent()");
        assertThat(tree(form)).hasText(nuts);
        toggle(form, names[4]);
        assertThat(tree(form)).hasText(closed);
        view.drawn().call("controller.cats.expandTree()");
        assertThat(tree(form)).hasText(new String[] {names[0], names[2], names[3], names[4], names[5], names[1]});
        view.drawn().call("controller.cats.filterControlsVisible.change(true)");
        Locator conditions = form.locator(".filter:visible");
        assertThat(conditions).hasCount(1);
        view.waitFor("data.cats.filterControlsVisible === true");
        view.drawn().call("controller.cats.resetFilters()");
        assertThat(conditions).hasCount(0);
        view.drawn().call("controller.cats.addFilter()");
        assertThat(conditions).hasCount(1);
        view.drawn().call("controller.cats.resetFilters()");
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

        click(cell(form, "catName", names[1]));
        button(toolbar, "Expand current object").click();
        settled();
        assertThat(tree(form)).hasText(closed);
        button(toolbar, "Expand whole tree").click();
        assertThat(tree(form)).hasText(new String[] {names[0], names[2], names[3], names[4], names[5], names[1]});
        toggle(form, names[0]);
        toggle(form, names[4]);
        assertThat(tree(form)).hasText(closed);
        click(cell(form, "catName", names[0]));
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

    @Before
    public void countRequests() {
        trackRequests();
    }

    // whether an element is in a row of the platform's tree that is not the current one
    private static boolean notCurrent(Locator element) {
        return Boolean.TRUE.equals(element.evaluate("element => !element.closest('tr').classList.contains('table-active')"));
    }

    // what the view was last drawn with, its data and its controller, as a script names them; the categories it is
    // given open, the items it is given, by name, whether no category's children are being loaded, and the tree's
    // conditions, each as `property compare value`
    private final View view = view("window.uiTtrView",
            " const opened = () => data.c.list.filter(r => r.expanded).map(r => r.catName.value).join();"
            + " const items = () => data.i.list.map(r => r.name.value).join();"
            + " const loaded = () => data.c.list.every(r => !r.loading);"
            + " const conditions = () => data.cats.filters.map(f => f.property + ' ' + f.compare + ' ' + f.value).join('; ');");
}
