package lsfusion.tests;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Route;
import com.microsoft.playwright.options.KeyboardModifier;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.Assert.assertEquals;

// trees in the browser (UiReactTree.lsf, its React views in UiReactTree.js): one a React view draws - its rows and
// their `expanded` as the server opens and closes its nodes, the view's own verbs, what a node shows before the answer,
// the rows a node does not touch kept as they were, its sortings -, a recursive one a view draws, and the platform's
// own - its nodes opened by a press and by the server, its one header sorting the group of the column, the conditions
// of all its groups a view draws
public class UiReactTreeIT extends UiTest {

    // the server's EXPAND and COLLAPSE - DOWN, ALL, ALL TOP - of a tree a React view draws: the rows the view gets and
    // their `expanded` follow from the keys, each item hanging under its category
    @Test
    public void theServerOpensAndClosesTheNodesOfATreeAViewDraws() throws Exception {
        String p = seed();
        Locator form = open("UiReactTree.uiRtr", "UiReactTree.uiRtrDrawn");
        Locator categories = form.locator(".ui-rtr-category"), items = form.locator(".ui-rtr-item");
        assertThat(categories).hasText(new String[] {p + " Fruit closed", p + " Veg closed", p + " Empty closed empty"});
        assertThat(items).hasCount(0);

        button(form, "uiRtrExpandFruit").click();
        assertThat(categories).hasText(new String[] {p + " Fruit open", p + " Veg closed", p + " Empty closed empty"});
        assertThat(items).hasText(fruits(p));
        button(form, "uiRtrCollapseAllFruit").click();
        assertThat(categories).hasText(new String[] {p + " Fruit closed", p + " Veg closed", p + " Empty closed empty"});
        assertThat(items).hasCount(0);
        button(form, "uiRtrExpandAllTop").click();
        assertThat(categories).hasText(new String[] {p + " Fruit open", p + " Veg open", p + " Empty closed empty"});
        assertThat(items).hasText(all(p));
        button(form, "uiRtrCollapseAllTop").click();
        assertThat(categories).hasText(new String[] {p + " Fruit closed", p + " Veg closed", p + " Empty closed empty"});
        assertThat(items).hasCount(0);
        button(form, "uiRtrExpandAllFruit").click();
        assertThat(categories).hasText(new String[] {p + " Fruit open", p + " Veg closed", p + " Empty closed empty"});
        assertThat(items).hasText(fruits(p));
        button(form, "uiRtrCollapseFruit").click();
        assertThat(categories).hasText(new String[] {p + " Fruit closed", p + " Veg closed", p + " Empty closed empty"});
        assertThat(items).hasCount(0);
        close(form);
    }

    // the verbs of a tree a React view draws: a node opened and closed through its group's member - by the row, by the
    // key the view was given, by a key out of `keys`, which is a string -, a key that names no row refused, every node
    // of the tree opened and closed through its top group; a row of an item made current by its key out of `byKey` -
    // the whole path down to it -, as the server reads it too, and that key refused once the row is gone, the tree then
    // having no current row; a category made current, whose node the tree's own member opens
    @Test
    public void aViewOpensAndClosesTheNodesOfATreeItDraws() throws Exception {
        String p = seed();
        Locator form = open("UiReactTree.uiRtr", "UiReactTree.uiRtrDrawn");
        Locator categories = form.locator(".ui-rtr-category"), items = form.locator(".ui-rtr-item"),
                current = form.locator(".ui-rtr-current"), server = form.locator(".ui-rtr-server-current");
        assertThat(categories).hasCount(3);

        drawn("controller.c.toggle(node('c', '" + p + " Fruit'))");
        assertThat(categories).hasText(new String[] {p + " Fruit open", p + " Veg closed", p + " Empty closed empty"});
        assertThat(items).hasText(fruits(p));
        drawn("controller.c.collapse(data.c.keys[data.c.list.indexOf(node('c', '" + p + " Fruit'))])");
        assertThat(categories).hasText(new String[] {p + " Fruit closed", p + " Veg closed", p + " Empty closed empty"});
        assertThat(items).hasCount(0);
        drawn("controller.c.expand(node('c', '" + p + " Fruit').key)");
        assertThat(items).hasText(fruits(p));
        assertEquals("controller.c.expand(): expects a row of 'c', its objects handle, or its key; that is none of them,"
                + " or names no row the group has now", refusal("controller.c.expand('no such key')"));

        drawn("controller.c.collapseAll()");
        assertThat(categories).hasText(new String[] {p + " Fruit closed", p + " Veg closed", p + " Empty closed empty"});
        assertThat(items).hasCount(0);
        drawn("controller.c.expandAll()");
        assertThat(categories).hasText(new String[] {p + " Fruit open", p + " Veg open", p + " Empty closed empty"});
        assertThat(items).hasText(all(p));

        String apple = (String) page.evaluate("() => { const byKey = window.uiRtrDrawnState.data.i.byKey;"
                + " return Object.keys(byKey).find(key => byKey[key].iName.value === 'Apple'); }");
        drawn("controller.i.change('" + apple + "')");
        assertThat(current).hasText("Apple");
        assertThat(server).hasText("category " + p + " Fruit, item Apple");
        drawn("controller.c.collapseAll()"); // the current row goes with the rows it was among
        assertThat(items).hasCount(0);
        assertThat(current).hasText("none");
        assertEquals("the current key is cleared", true,
                page.evaluate("() => window.uiRtrDrawnState.data.t.current === null"));
        assertEquals("controller.i.change(): expects a row of 'i', its objects handle, or its key; that is none of them,"
                + " or names no row the group has now", refusal("controller.i.change('" + apple + "')"));

        drawn("controller.c.change(node('c', '" + p + " Veg'))");
        assertThat(current).hasText(p + " Veg");
        assertThat(server).hasText("category " + p + " Veg, item none");
        drawn("controller.t.expandCurrent()");
        assertThat(categories).hasText(new String[] {p + " Fruit closed", p + " Veg open", p + " Empty closed empty"});
        assertThat(items).hasText(new String[] {"Potato 20 in " + p + " Veg"});
        close(form);
    }

    // what a node of a tree a React view draws shows before the answer to its member's call - the answers are held back
    // here until it is seen: opened, it says so and that its children are loading, which arrive with the answer;
    // closed, it says so while its children stay until the answer takes them away; toggled twice it is closed, and
    // stays so when the answers come; a node with no children is not opened; all the nodes opened through the top
    // group are open at once, each with children loading them - one open already too -, and closed so are closed at
    // once, their children staying until the answer
    @Test
    public void aNodeIsShownAsAskedBeforeTheAnswer() throws Exception {
        String p = seed();
        Locator form = open("UiReactTree.uiRtr", "UiReactTree.uiRtrDrawn");
        Locator categories = form.locator(".ui-rtr-category"), items = form.locator(".ui-rtr-item");
        String fruit = "node('c', '" + p + " Fruit')";
        assertThat(categories).hasCount(3);

        hold();
        drawn("controller.c.toggle(" + fruit + ")");
        assertThat(categories).hasText(new String[] {p + " Fruit open loading", p + " Veg closed", p + " Empty closed empty"});
        assertThat(items).hasCount(0);
        release();
        assertThat(categories).hasText(new String[] {p + " Fruit open", p + " Veg closed", p + " Empty closed empty"});
        assertThat(items).hasText(fruits(p));

        hold();
        drawn("controller.c.toggle(" + fruit + ")");
        assertThat(categories).hasText(new String[] {p + " Fruit closed", p + " Veg closed", p + " Empty closed empty"});
        assertThat(items).hasText(fruits(p));
        release();
        assertThat(items).hasCount(0);

        hold();
        drawn("controller.c.toggle(" + fruit + "); controller.c.toggle(" + fruit + ")");
        assertThat(categories).hasText(new String[] {p + " Fruit closed", p + " Veg closed", p + " Empty closed empty"});
        drawn("controller.c.toggle(node('c', '" + p + " Empty'))");
        assertThat(categories).hasText(new String[] {p + " Fruit closed", p + " Veg closed", p + " Empty closed empty"});
        release();
        // the vegetables opened after it all: what the answers before theirs did shows by then
        drawn("controller.c.toggle(node('c', '" + p + " Veg'))");
        assertThat(categories).hasText(new String[] {p + " Fruit closed", p + " Veg open", p + " Empty closed empty"});
        assertThat(items).hasText(new String[] {"Potato 20 in " + p + " Veg"});

        hold();
        drawn("controller.c.expandAll()");
        assertThat(categories).hasText(new String[] {p + " Fruit open loading", p + " Veg open loading", p + " Empty closed empty"});
        release();
        assertThat(items).hasText(all(p));
        assertThat(categories).hasText(new String[] {p + " Fruit open", p + " Veg open", p + " Empty closed empty"});
        hold();
        drawn("controller.c.collapseAll()");
        assertThat(categories).hasText(new String[] {p + " Fruit closed", p + " Veg closed", p + " Empty closed empty"});
        assertThat(items).hasText(all(p));
        release();
        assertThat(items).hasCount(0);
        close(form);
    }

    // a node of a tree a React view draws, opened and then closed, changes its own row alone: the rows it does not
    // touch - the other categories, the items under them - keep their identity across both answers, the same objects
    // the view was given before, as the projection keeps a row that did not change
    @Test
    public void theRowsANodeDoesNotTouchKeepTheirIdentity() throws Exception {
        String p = seed();
        Locator form = open("UiReactTree.uiRtr", "UiReactTree.uiRtrDrawn");
        Locator categories = form.locator(".ui-rtr-category"), items = form.locator(".ui-rtr-item");
        assertThat(categories).hasText(new String[] {p + " Fruit closed", p + " Veg closed", p + " Empty closed empty"});
        drawn("controller.c.expand(node('c', '" + p + " Fruit'))");
        assertThat(items).hasText(fruits(p));
        page.evaluate("() => { const data = window.uiRtrDrawnState.data; window.uiRtrRows = []"
                + ".concat(data.c.list.map(row => ['c', row, row.cName.value]), data.i.list.map(row => ['i', row, row.iName.value])); }");

        drawn("controller.c.expand(node('c', '" + p + " Veg'))");
        assertThat(categories).hasText(new String[] {p + " Fruit open", p + " Veg open", p + " Empty closed empty"});
        assertThat(items).hasText(all(p));
        assertEquals("the rows the vegetables' node opened changed", p + " Veg", changedRows());
        drawn("controller.c.collapse(node('c', '" + p + " Veg'))");
        assertThat(categories).hasText(new String[] {p + " Fruit open", p + " Veg closed", p + " Empty closed empty"});
        assertThat(items).hasText(fruits(p));
        assertEquals("the rows the vegetables' node closed changed", p + " Veg", changedRows());
        close(form);
    }

    // a filter of the categories of a tree a React view draws takes a category away while its items stay - the server
    // goes on sending them -, their parent a row the view does not have; the filter taken off brings the category back
    // with its items under it
    @Test
    public void theItemsOfACategoryFilteredOutStayWithNoParent() throws Exception {
        String p = seed();
        Locator form = open("UiReactTree.uiRtr", "UiReactTree.uiRtrDrawn");
        Locator categories = form.locator(".ui-rtr-category"), items = form.locator(".ui-rtr-item"),
                orphans = form.locator(".ui-rtr-orphan");
        assertThat(categories).hasText(new String[] {p + " Fruit closed", p + " Veg closed", p + " Empty closed empty"});
        drawn("controller.c.expandAll()");
        assertThat(items).hasText(all(p));
        assertThat(orphans).hasCount(0);
        Object children = page.evaluate("() => window.uiRtrDrawnState.data.i.list.map(row => ({"
                + " key: row.key, parent: row.parent, name: row.iName.value, qty: row.qty.value }))");
        assertEquals("integer properties are projected as numbers", "number,number,number,number",
                page.evaluate("() => window.uiRtrDrawnState.data.i.list.map(row => typeof row.qty.value).join()"));

        drawn("controller.t.filters.change({property: 'c.cName', compare: '=', value: '" + p + " Veg'})");
        assertThat(categories).hasText(new String[] {p + " Veg open"});
        assertThat(items).hasText(new String[] {"Potato 20 in " + p + " Veg"});
        assertThat(orphans).hasText(new String[] {"Banana 9", "Apple 5", "Cherry 1"});
        assertEquals("all children retain their keys and parent keys after their ancestor is filtered out", children,
                page.evaluate("() => window.uiRtrDrawnState.data.i.list.map(row => ({"
                        + " key: row.key, parent: row.parent, name: row.iName.value, qty: row.qty.value }))"));
        drawn("controller.t.filters.change([])");
        assertThat(categories).hasText(new String[] {p + " Fruit open", p + " Veg open", p + " Empty closed empty"});
        assertThat(items).hasText(all(p));
        assertThat(orphans).hasCount(0);
        close(form);
    }

    // a recursive tree a React view draws - folders in folders: a folder opened brings the folders in it into the same
    // group, each hanging under the folder it is in, and closing all of them leaves the folders at the top
    @Test
    public void aRecursiveTreeHangsItsRowsOnRowsOfItsOwnGroup() throws Exception {
        long id = System.nanoTime();
        String p = "F" + id;
        exec("UiReactTree.uiRtrSeedFolders", p);
        Locator form = open("UiReactTree.uiRtr", "UiReactTree.uiRtrFolders");
        Locator folders = form.locator(".ui-rtr-folder");
        assertThat(folders).hasText(new String[] {p + " Documents at the top closed", p + " Archive at the top empty"});

        folders("controller.f.expand(node('" + p + " Documents'))");
        assertThat(folders).hasText(new String[] {p + " Documents at the top open", p + " Invoices in " + p + " Documents closed",
                p + " Archive at the top empty"});
        folders("controller.f.expand(node('" + p + " Invoices'))");
        assertThat(folders).hasText(new String[] {p + " Documents at the top open", p + " Invoices in " + p + " Documents open",
                p + " 2026 in " + p + " Invoices empty", p + " Archive at the top empty"});
        folders("controller.f.collapseAll()");
        assertThat(folders).hasText(new String[] {p + " Documents at the top closed", p + " Archive at the top empty"});
        close(form);
    }

    // the sortings of a tree a React view draws are the tree's, of all its groups, each property named with its
    // group: one sorting changes its group's - appended, or in its place -, leaving the other group's alone; one the
    // design says not to sort by and an unknown name are refused, changing nothing; an array states the whole list,
    // its order the priority, including the secondary sorting of rows whose primary values are equal. The
    // server's ORDER of a group shows there too, its ORDERS read a view's sorting, which an ORDER of what was read
    // states again - the rows following each, as the answer brings them
    @Test
    public void theSortingsOfATreeAViewDrawsAreTheTrees() throws Exception {
        String p = seed();
        Locator form = open("UiReactTree.uiRtr", "UiReactTree.uiRtrDrawn");
        Locator categories = form.locator(".ui-rtr-category"), items = form.locator(".ui-rtr-item"),
                orders = form.locator(".ui-rtr-orders");
        button(form, "uiRtrExpandAllTop").click();
        assertThat(items).hasText(all(p));
        assertThat(orders).hasText("");

        drawn("controller.t.orders.change({property: 'c.cName', desc: true})");
        assertThat(categories).hasText(new String[] {p + " Veg open", p + " Fruit open", p + " Empty closed empty"});
        assertThat(orders).hasText("c.cName desc");
        drawn("controller.t.orders.change({property: 'i.qty', desc: false})");
        assertThat(items).hasText(new String[] {"Potato 20 in " + p + " Veg", "Cherry 1 in " + p + " Fruit",
                "Apple 5 in " + p + " Fruit", "Banana 9 in " + p + " Fruit"});
        assertThat(orders).hasText("c.cName desc, i.qty asc");
        drawn("controller.t.orders.change({property: 'i.qty', desc: true})");
        assertThat(items).hasText(new String[] {"Potato 20 in " + p + " Veg", "Banana 9 in " + p + " Fruit",
                "Apple 5 in " + p + " Fruit", "Cherry 1 in " + p + " Fruit"});
        assertThat(orders).hasText("c.cName desc, i.qty desc");

        assertEquals("controller.t.orders.change(): 'i.code' cannot be sorted by",
                refusal("controller.t.orders.change({property: 'i.code', desc: true})"));
        assertThat(orders).hasText("c.cName desc, i.qty desc");
        assertThat(items).hasText(new String[] {"Potato 20 in " + p + " Veg", "Banana 9 in " + p + " Fruit",
                "Apple 5 in " + p + " Fruit", "Cherry 1 in " + p + " Fruit"});
        assertEquals("controller.t.orders.change(): unknown property 'nope' of object group 'i'",
                refusal("controller.t.orders.change({property: 'i.nope', desc: true})"));
        assertThat(orders).hasText("c.cName desc, i.qty desc");
        assertThat(items).hasText(new String[] {"Potato 20 in " + p + " Veg", "Banana 9 in " + p + " Fruit",
                "Apple 5 in " + p + " Fruit", "Cherry 1 in " + p + " Fruit"});
        drawn("controller.t.orders.change({property: 'i.iName', desc: false})");
        assertThat(orders).hasText("c.cName desc, i.qty desc, i.iName asc");

        drawn("controller.t.orders.change([{property: 'i.qty', desc: true}, {property: 'i.iName', desc: false}])");
        assertThat(categories).hasText(new String[] {p + " Fruit open", p + " Veg open", p + " Empty closed empty"});
        assertThat(orders).hasText("i.qty desc, i.iName asc");
        drawn("controller.t.orders.change([{property: 'i.iName', desc: false}, {property: 'i.qty', desc: true}])");
        assertThat(items).hasText(new String[] {"Apple 5 in " + p + " Fruit", "Banana 9 in " + p + " Fruit",
                "Cherry 1 in " + p + " Fruit", "Potato 20 in " + p + " Veg"});
        assertThat(orders).hasText("i.iName asc, i.qty desc");
        button(form, "uiRtrOrderItems").click();
        assertThat(items).hasText(new String[] {"Banana 9 in " + p + " Fruit", "Apple 5 in " + p + " Fruit",
                "Cherry 1 in " + p + " Fruit", "Potato 20 in " + p + " Veg"});
        assertThat(orders).hasText("i.qty desc");

        drawn("controller.t.orders.change([{property: 'i.qty', desc: false}])");
        assertThat(items).hasText(new String[] {"Cherry 1 in " + p + " Fruit", "Apple 5 in " + p + " Fruit",
                "Banana 9 in " + p + " Fruit", "Potato 20 in " + p + " Veg"});
        button(form, "uiRtrSaveOrders").click();
        drawn("controller.t.orders.change([])");
        assertThat(items).hasText(all(p));
        assertThat(orders).hasText("");
        button(form, "uiRtrRestoreOrders").click();
        assertThat(items).hasText(new String[] {"Cherry 1 in " + p + " Fruit", "Apple 5 in " + p + " Fruit",
                "Banana 9 in " + p + " Fruit", "Potato 20 in " + p + " Veg"});
        assertThat(orders).hasText("i.qty asc");

        button(form, "uiRtrTieQuantities").click();
        assertThat(items).hasText(new String[] {"Cherry 1 in " + p + " Fruit", "Banana 5 in " + p + " Fruit",
                "Apple 5 in " + p + " Fruit", "Potato 20 in " + p + " Veg"});
        drawn("controller.t.orders.change([{property: 'i.qty', desc: false}, {property: 'i.iName', desc: false}])");
        assertThat(items).hasText(new String[] {"Cherry 1 in " + p + " Fruit", "Apple 5 in " + p + " Fruit",
                "Banana 5 in " + p + " Fruit", "Potato 20 in " + p + " Veg"});
        assertThat(orders).hasText("i.qty asc, i.iName asc");
        drawn("controller.t.orders.change([{property: 'i.iName', desc: false}, {property: 'i.qty', desc: false}])");
        assertThat(items).hasText(new String[] {"Apple 5 in " + p + " Fruit", "Banana 5 in " + p + " Fruit",
                "Cherry 1 in " + p + " Fruit", "Potato 20 in " + p + " Veg"});
        assertThat(orders).hasText("i.iName asc, i.qty asc");
        form.locator("button[lsfusion-container='PROPERTY(formApply())']").click();
        close(form);
    }

    // the platform's own tree by the mouse: a node with children shows closed, one with none a leaf; a press on the
    // glyph of the current row's node opens it and closes it again; before the answer, which is held back here, the
    // glyph turns at once, a row standing for each of the node's children, which goes when the answer brings them, and
    // a node closed loses its rows at once; two presses leave it closed when the answers come; a press on the glyph of
    // another row only selects it, and a second press opens it, but not a leaf
    @Test
    public void thePlatformsTreeOpensANodeByAPressOnItsGlyph() throws Exception {
        String p = seed();
        Locator form = open("UiReactTree.uiRtr", "UiReactTree.uiRtrPlatform");
        Locator fruit = glyph(form, p + " Fruit"), veg = glyph(form, p + " Veg"), empty = glyph(form, p + " Empty"),
                items = itemCells(form), nodes = form.locator("[__tree_node]"),
                loading = form.locator("[__tree_node].loading-async-image");
        assertNode(fruit, "collapsed");
        assertNode(veg, "collapsed");
        assertNode(empty, "leaf");

        fruit.click(); // the first row is the current one
        assertNode(fruit, "expanded");
        assertThat(items).hasText(new String[] {"Banana", "Apple", "Cherry"});
        fruit.click();
        assertNode(fruit, "collapsed");
        assertThat(items).hasCount(0);

        hold();
        fruit.click();
        assertNode(fruit, "expanded");
        assertThat(loading).hasCount(3);
        assertThat(nodes).hasCount(6);
        release();
        // the rows that stood for the children are the children now - their glyphs keep the class they had as such
        assertThat(items).hasText(new String[] {"Banana", "Apple", "Cherry"});
        assertThat(nodes).hasCount(6);
        hold();
        fruit.click();
        assertNode(fruit, "collapsed");
        assertThat(nodes).hasCount(3);
        assertThat(items).hasCount(0);
        release();

        hold();
        fruit.click();
        assertNode(fruit, "expanded");
        fruit.click();
        assertNode(fruit, "collapsed");
        release();

        empty.click(); // another row's: selects it
        empty.click();
        veg.click();
        veg.click();
        // the vegetables opened after it all: what the answers before theirs did shows by then
        assertNode(veg, "expanded");
        assertThat(items).hasText(new String[] {"Potato"});
        assertNode(fruit, "collapsed");
        assertNode(empty, "leaf");
        close(form);
    }

    // the server's EXPAND and COLLAPSE of the platform's own tree: the glyphs and the rows under them follow
    @Test
    public void theServerOpensAndClosesTheNodesOfThePlatformsTree() throws Exception {
        String p = seed();
        Locator form = open("UiReactTree.uiRtr", "UiReactTree.uiRtrPlatform");
        Locator fruit = glyph(form, p + " Fruit"), veg = glyph(form, p + " Veg"), items = itemCells(form);
        assertNode(fruit, "collapsed");

        button(form, "uiRtrPlatformExpandAllTop").click();
        assertNode(fruit, "expanded");
        assertNode(veg, "expanded");
        assertThat(items).hasText(new String[] {"Banana", "Apple", "Cherry", "Potato"});
        button(form, "uiRtrPlatformCollapseAllTop").click();
        assertNode(fruit, "collapsed");
        assertNode(veg, "collapsed");
        assertThat(items).hasCount(0);
        button(form, "uiRtrPlatformExpandFruit").click();
        assertNode(fruit, "expanded");
        assertNode(veg, "collapsed");
        assertThat(items).hasText(new String[] {"Banana", "Apple", "Cherry"});
        button(form, "uiRtrPlatformCollapseAllFruit").click();
        assertNode(fruit, "collapsed");
        assertThat(items).hasCount(0);
        close(form);
    }

    // the platform's tree has one header, of its last group's columns: a double click on one its design says not to
    // sort by (noSort) shows and asks for nothing, on another column it sorts the items by it - one request each
    // time -, and the same with ctrl turns it round
    @Test
    public void aTreesHeaderDoesNotSortByANoSortColumn() throws Exception {
        String p = seed();
        List<String> sortings = sortings();
        Locator form = open("UiReactTree.uiRtr", "UiReactTree.uiRtrPlatform");
        button(form, "uiRtrPlatformExpandFruit").click();
        Locator items = itemCells(form);
        assertThat(items).hasText(new String[] {"Banana", "Apple", "Cherry"});
        assertArrows(form);

        header(form, "qty").dblclick();
        assertArrows(form);
        assertThat(items).hasText(new String[] {"Banana", "Apple", "Cherry"});
        header(form, "iName").dblclick();
        assertThat(items).hasText(new String[] {"Apple", "Banana", "Cherry"});
        assertArrows(form, "iName asc");
        assertEquals("the header's requests", 1, sortings.size());

        header(form, "qty").dblclick();
        assertArrows(form, "iName asc");
        assertThat(items).hasText(new String[] {"Apple", "Banana", "Cherry"});
        header(form, "iName").dblclick(new Locator.DblclickOptions().setModifiers(Collections.singletonList(KeyboardModifier.CONTROL)));
        assertThat(items).hasText(new String[] {"Cherry", "Banana", "Apple"});
        assertArrows(form, "iName desc");
        assertEquals("the header's requests", 2, sortings.size());
        close(form);
    }

    // a tree's own sortings, given by the form: the arrow of the items' one is in the header, though a double click
    // on it - a noSort column - shows and asks for nothing, and the categories' one has no column there; a click on an
    // items' column sorts the items by it, and the categories keep their sorting
    @Test
    public void aTreesHeaderSortsTheGroupOfItsColumnAlone() throws Exception {
        String p = seed();
        List<String> sortings = sortings();
        Locator form = open("UiReactTree.uiRtr", "UiReactTree.uiRtrOrdered");
        button(form, "uiRtrOrderedExpandAllTop").click();
        Locator categories = form.locator("td[lsfusion-container='PROPERTY(cName)']"), items = itemCells(form);
        assertThat(categories).hasText(new String[] {p + " Veg", p + " Fruit", p + " Empty"});
        assertThat(items).hasText(new String[] {"Potato", "Cherry", "Apple", "Banana"});
        assertArrows(form, "qty asc");

        header(form, "qty").dblclick();
        assertArrows(form, "qty asc");
        assertThat(items).hasText(new String[] {"Potato", "Cherry", "Apple", "Banana"});
        header(form, "iName").dblclick();
        assertThat(items).hasText(new String[] {"Potato", "Apple", "Banana", "Cherry"});
        assertThat(categories).hasText(new String[] {p + " Veg", p + " Fruit", p + " Empty"});
        assertArrows(form, "iName asc");
        assertEquals("the header's requests", 1, sortings.size());
        close(form);
    }

    // a React view drawing the user filters of the platform's tree - one box for all its groups - is given the
    // conditions of all of them, each property named with its group, its value of the property's type: one condition
    // changes its property's, leaving the other group's alone - put in, put in the place of the first one on the
    // property, taken off with no value -, one on an unknown property is refused, changing nothing, an array states
    // them all, those of both groups or of one, a negated one and one joined to the next by OR; the rows the platform
    // draws follow
    @Test
    public void aViewOfATreesFiltersChangesOneConditionOfAGroup() throws Exception {
        String p = seed();
        Locator form = open("UiReactTree.uiRtr", "UiReactTree.uiRtrFiltered");
        button(form, "uiRtrFilteredExpandAllTop").click();
        Locator filters = form.locator(".ui-rtr-filters"), items = itemCells(form),
                categories = form.locator("td[lsfusion-container='PROPERTY(cName)']");
        assertThat(items).hasText(new String[] {"Banana", "Apple", "Cherry", "Potato"});
        assertThat(filters).hasText("no conditions");

        filtered("{property: 'c.cName', compare: '=', value: '" + p + " Fruit'}");
        assertThat(categories).hasText(new String[] {p + " Fruit"});
        assertThat(items).hasText(new String[] {"Banana", "Apple", "Cherry"});
        assertThat(filters).hasText("c.cName = " + p + " Fruit (string)");
        filtered("{property: 'i.iName', compare: '_', value: 'an'}");
        assertThat(items).hasText(new String[] {"Banana"});
        assertThat(filters).hasText("c.cName = " + p + " Fruit (string); i.iName _ an (string)");
        filtered("{property: 'i.iName', compare: '_', value: 'pp'}");
        assertThat(items).hasText(new String[] {"Apple"});
        assertThat(filters).hasText("c.cName = " + p + " Fruit (string); i.iName _ pp (string)");
        assertEquals("controller.t.filters.change(): unknown property 'nope' of object group 'i'",
                filterRefusal("{property: 'i.nope', compare: '=', value: 1}"));
        assertThat(filters).hasText("c.cName = " + p + " Fruit (string); i.iName _ pp (string)");
        assertThat(items).hasText(new String[] {"Apple"});
        filtered("{property: 'i.iName'}");
        assertThat(items).hasText(new String[] {"Banana", "Apple", "Cherry"});
        assertThat(filters).hasText("c.cName = " + p + " Fruit (string)");

        filtered("[{property: 'c.cName', compare: '=', value: '" + p + " Fruit'}, {property: 'i.iName', compare: '_', value: 'an'}]");
        assertThat(items).hasText(new String[] {"Banana"});
        assertThat(filters).hasText("c.cName = " + p + " Fruit (string); i.iName _ an (string)");
        filtered("[{property: 'i.qty', compare: '>', value: 2}]");
        assertThat(categories).hasText(new String[] {p + " Fruit", p + " Veg", p + " Empty"});
        assertThat(items).hasText(new String[] {"Banana", "Apple", "Potato"});
        assertThat(filters).hasText("i.qty > 2 (number)");
        assertEquals("the complete numeric condition",
                "[{\"property\":\"i.qty\",\"compare\":\">\",\"value\":2,\"negation\":false,\"or\":false}]",
                page.evaluate("() => JSON.stringify(window.uiRtrFiltersState.data.t.filters)"));
        filtered("[{property: 'i.qty', compare: '>', value: 2, negation: true}]");
        assertThat(items).hasText(new String[] {"Cherry"});
        assertThat(filters).hasText("i.qty > 2 (number) not");
        assertEquals("the complete negated condition",
                "[{\"property\":\"i.qty\",\"compare\":\">\",\"value\":2,\"negation\":true,\"or\":false}]",
                page.evaluate("() => JSON.stringify(window.uiRtrFiltersState.data.t.filters)"));
        filtered("[{property: 'i.qty', compare: '>', value: 8, or: true}, {property: 'i.qty', compare: '<', value: 2}]");
        assertThat(items).hasText(new String[] {"Banana", "Cherry", "Potato"});
        assertThat(filters).hasText("i.qty > 8 (number) or; i.qty < 2 (number)");
        assertEquals("both complete conditions and their OR junction",
                "[{\"property\":\"i.qty\",\"compare\":\">\",\"value\":8,\"negation\":false,\"or\":true},"
                        + "{\"property\":\"i.qty\",\"compare\":\"<\",\"value\":2,\"negation\":false,\"or\":false}]",
                page.evaluate("() => JSON.stringify(window.uiRtrFiltersState.data.t.filters)"));
        filtered("[]");
        assertThat(items).hasText(new String[] {"Banana", "Apple", "Cherry", "Potato"});
        assertThat(filters).hasText("no conditions");
        close(form);
    }

    // a FILTER of a group of the platform's tree from an action is among the conditions the view of the tree's filters
    // is given, and a FILTER of the other group then keeps it; a condition of the view's is what the group's FILTERS
    // reads, which a FILTER of what was read states again
    @Test
    public void aFilterOfOneGroupOfATreeKeepsTheOthers() throws Exception {
        String p = seed();
        Locator form = open("UiReactTree.uiRtr", "UiReactTree.uiRtrFiltered");
        button(form, "uiRtrFilteredExpandAllTop").click();
        Locator filters = form.locator(".ui-rtr-filters"), items = itemCells(form),
                categories = form.locator("td[lsfusion-container='PROPERTY(cName)']");
        assertThat(items).hasText(new String[] {"Banana", "Apple", "Cherry", "Potato"});

        button(form, "uiRtrFilterItems").click();
        assertThat(items).hasText(new String[] {"Banana", "Apple", "Potato"});
        assertThat(filters).hasText("i.qty > 2 (number)");
        button(form, "uiRtrFilterCategories").click();
        assertThat(categories).hasText(new String[] {p + " Fruit"});
        assertThat(items).hasText(new String[] {"Banana", "Apple"});
        assertThat(filters).hasText("c.cName = " + p + " Fruit (string); i.qty > 2 (number)");

        filtered("[{property: 'i.iName', compare: '_', value: 'an'}]");
        assertThat(categories).hasText(new String[] {p + " Fruit", p + " Veg", p + " Empty"});
        assertThat(items).hasText(new String[] {"Banana"});
        button(form, "uiRtrSaveFilters").click();
        filtered("[]");
        assertThat(items).hasText(new String[] {"Banana", "Apple", "Cherry", "Potato"});
        assertThat(filters).hasText("no conditions");
        button(form, "uiRtrRestoreFilters").click();
        assertThat(items).hasText(new String[] {"Banana"});
        assertThat(filters).hasText("i.iName _ an (string)");
        close(form);
    }

    // the categories of a tree, named after a prefix no other attempt has - what a failed attempt saved stays in the
    // database for its rerun -, shown from now on: Fruit with Banana, Apple and Cherry, Veg with Potato, Empty
    private static String seed() throws Exception {
        String prefix = "T" + System.nanoTime();
        exec("UiReactTree.uiRtrSeed", prefix);
        return prefix;
    }

    // the lines of the drawn tree's items: the fruits alone, and all of them
    private static String[] fruits(String p) {
        return new String[] {"Banana 9 in " + p + " Fruit", "Apple 5 in " + p + " Fruit", "Cherry 1 in " + p + " Fruit"};
    }

    private static String[] all(String p) {
        return new String[] {"Banana 9 in " + p + " Fruit", "Apple 5 in " + p + " Fruit", "Cherry 1 in " + p + " Fruit",
                "Potato 20 in " + p + " Veg"};
    }

    // calls of the members of the drawn tree's view, with its data, `node(group, name)` finding a row by its name -
    // failing with what they throw
    private void drawn(String calls) {
        String refusal = refusal(calls);
        if (refusal != null)
            throw new AssertionError(calls + ": " + refusal);
    }

    // ... what they throw, null if nothing
    private String refusal(String calls) {
        return (String) page.evaluate("() => { const {data, controller} = window.uiRtrDrawnState;"
                + " const node = (g, name) => data[g].list.find(r => (g === 'c' ? r.cName : r.iName).value === name);"
                + " try { " + calls + "; return null; } catch (e) { return e.message; } }");
    }

    // ... the names of the rows saved in uiRtrRows that the view now has other objects for, or none
    private String changedRows() {
        return (String) page.evaluate("() => { const data = window.uiRtrDrawnState.data;"
                + " return window.uiRtrRows.filter(([group, row]) => data[group].byKey[row.key] !== row)"
                + ".map(([group, row, name]) => name).join(', '); }");
    }

    // ... of the folders' view, `node(name)` finding a folder
    private void folders(String call) {
        page.evaluate("() => { const {data, controller} = window.uiRtrFoldersState;"
                + " const node = name => data.f.list.find(r => r.fName.value === name); " + call + "; }");
    }

    // ... a change of the conditions of the filtered tree through its view - failing with what it throws
    private void filtered(String conditions) {
        String refusal = filterRefusal(conditions);
        if (refusal != null)
            throw new AssertionError(conditions + ": " + refusal);
    }

    // ... what it throws, null if nothing
    private String filterRefusal(String conditions) {
        return (String) page.evaluate("() => { try { window.uiRtrFiltersState.controller.t.filters.change(" + conditions + ");"
                + " return null; } catch (e) { return e.message; } }");
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

    // the requests the page sends from now on that set the sortings of a group (GWT-RPC names the action's class)
    private List<String> sortings() {
        List<String> sent = Collections.synchronizedList(new ArrayList<>());
        page.onRequest(request -> {
            String body = request.postData();
            if (body != null && body.contains(".SetPropertyOrders/"))
                sent.add(body);
        });
        return sent;
    }

    // the glyph of a category's node in the platform's tree
    private Locator glyph(Locator form, String category) {
        return row(form, "cName", category).locator("[__tree_node]");
    }

    // a node's glyph says it: expanded, collapsed, or a leaf
    private static void assertNode(Locator glyph, String state) {
        assertThat(glyph).hasClass(Pattern.compile("\\b" + state + "-image\\b"));
        for (String other : new String[] {"expanded", "collapsed", "leaf"})
            if (!other.equals(state))
                assertThat(glyph).not().hasClass(Pattern.compile("\\b" + other + "-image\\b"));
    }

    // the cells of the platform's tree that show the items' names, in the order the tree shows them
    private static Locator itemCells(Locator form) {
        return form.locator("td[lsfusion-container='PROPERTY(iName)']");
    }

    // a column's header as the table shows it: the table keeps a copy of its header out of sight
    private static Locator header(Locator form, String property) {
        return form.locator("th[lsfusion-container='PROPERTY(" + property + ")']:visible");
    }

    // the arrows the header shows, each "<property> asc|desc" in the order of the columns - one reading of all of them;
    // an image of neither direction as its classes, so that it matches no arrow
    private static final String ARROWS = "form => Array.from(form.querySelectorAll('th')).filter(th => {"
            + " const box = th.getBoundingClientRect(); return box.width > 0 && box.height > 0"
            + " && getComputedStyle(th).visibility !== 'hidden' && th.querySelector('.sort-img'); })"
            + ".map(th => { const image = th.querySelector('.sort-img').className,"
            + " up = image.includes('arrow-up'), down = image.includes('arrow-down');"
            + " return th.getAttribute('lsfusion-container').replace(/^PROPERTY\\((.*)\\)$/, '$1')"
            + " + (up !== down ? (up ? ' asc' : ' desc') : ' [' + image + ']'); }).join(', ')";

    // ... until they are exactly these, and no others, failing with what they are then
    private void assertArrows(Locator form, String... arrows) {
        String expected = String.join(", ", arrows);
        long deadline = System.currentTimeMillis() + 10_000;
        while (true) {
            Object shown = form.evaluate(ARROWS);
            if (expected.equals(shown))
                return;
            if (System.currentTimeMillis() > deadline)
                throw new AssertionError("the arrows are '" + shown + "', not '" + expected + "'");
            page.waitForTimeout(100);
        }
    }

    // a button the platform draws, by its action
    private static Locator button(Locator form, String action) {
        return form.locator("button[lsfusion-container='PROPERTY(" + action + "())']");
    }
}
