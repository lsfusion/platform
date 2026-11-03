package lsfusion.tests;

import com.microsoft.playwright.Locator;
import org.junit.Before;
import org.junit.Test;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.Assert.assertEquals;

// a group's user filters a React view draws (UiReactFilters.lsf, its view UiReactFilters.js): the conditions and the
// member that changes them - over the platform's grid, whose filter keys, filter controls, FILTER actions and pivot
// state the same list, over the rows the view draws and on a tree's node
public class UiReactFiltersIT extends UiTest {

    private static final String FOLDER = "UiReactFilters.uiRfl";

    // a view drawing a group's conditions over the platform's grid (FILTERS(i)) has them as a part of the group - the
    // conditions, what one can be on, and the one member that changes them -: a change of one condition takes the place
    // of the first on its property or is appended, one with no value - or an empty string - takes that first one off,
    // and an array states the whole list; the grid follows
    @Test
    public void theConditionsAViewDraws() throws Exception {
        String[] items = seed();
        Locator form = open(FOLDER, "UiReactFilters.uiRflConditions");
        view.waitFor("data.i.filters.length === 0");
        assertEquals("filterProperties,filters,properties", view.drawn().eval("Object.keys(data.i).sort().join()"));
        assertEquals("filters/change", view.drawn().eval("Object.keys(controller.i).join() + '/' + Object.keys(controller.i.filters).join()"));
        assertThat(names(form)).hasText(items);

        view.drawn().call("controller.i.filters.change({property: 'name', compare: '_', value: 'pp'})");
        view.waitFor("conditions() === 'name _ pp'");
        assertEquals("false,false", view.drawn().eval("[data.i.filters[0].negation, data.i.filters[0].or].join()"));
        assertThat(names(form)).hasText(new String[] {items[0]});
        view.drawn().call("controller.i.filters.change({property: 'price', compare: '>', value: 1})");
        view.waitFor("conditions() === 'name _ pp; price > 1'");
        view.drawn().call("controller.i.filters.change({property: 'name', compare: '_', value: 'an'})");
        view.waitFor("conditions() === 'name _ an; price > 1'");
        assertThat(names(form)).hasCount(0);
        view.drawn().call("controller.i.filters.change({property: 'price'})");
        view.waitFor("conditions() === 'name _ an'");
        assertThat(names(form)).hasText(new String[] {items[1]});
        view.drawn().call("controller.i.filters.change({property: 'price', compare: '>', value: 1})");
        assertThat(names(form)).hasCount(0);
        view.waitFor("conditions() === 'name _ an; price > 1'");
        view.drawn().call("controller.i.filters.change({property: 'name', value: ''})");
        assertThat(names(form)).hasText(new String[] {items[0]});
        view.waitFor("conditions() === 'price > 1'");
        view.drawn().call("controller.i.filters.change([{property: 'price', compare: '<', value: 1}])");
        view.waitFor("conditions() === 'price < 1'");
        assertThat(names(form)).hasText(new String[] {items[1]});
        view.drawn().call("controller.i.filters.change([])");
        view.waitFor("conditions() === ''");
        assertThat(names(form)).hasText(items);
        close(form);
    }

    // a condition's negation and its junction reach the server: a negated one leaves the rows it does not hold for, or
    // on the first of two joins it to the next one - the pair a disjunction, a conjunction without it -; a value comes
    // back of its own type, a string's a string, a number's a number
    @Test
    public void theNegationAndTheJunctionOfConditions() throws Exception {
        String[] items = seed();
        Locator form = open(FOLDER, "UiReactFilters.uiRflConditions");
        view.drawn().call("controller.i.filters.change([{property: 'name', compare: '_', value: 'an', negation: true}])");
        view.waitFor("conditions() === 'name _ an' && data.i.filters[0].negation === true");
        assertThat(names(form)).hasText(new String[] {items[0]});
        view.drawn().call("controller.i.filters.change([{property: 'name', compare: '=', value: '" + items[0] + "', or: true},"
                + " {property: 'name', compare: '=', value: '" + items[1] + "'}])");
        view.waitFor("data.i.filters.length === 2 && data.i.filters[0].or === true && data.i.filters[1].or === false");
        assertThat(names(form)).hasText(items);
        view.drawn().call("controller.i.filters.change([{property: 'name', compare: '=', value: '" + items[0] + "'},"
                + " {property: 'name', compare: '=', value: '" + items[1] + "'}])");
        view.waitFor("data.i.filters.length === 2 && data.i.filters[0].or === false");
        assertThat(names(form)).hasCount(0);
        view.drawn().call("controller.i.filters.change([{property: 'name', compare: '_', value: 'an'}, {property: 'price', compare: '>=', value: 0.75}])");
        assertThat(names(form)).hasText(new String[] {items[1]});
        view.waitFor("conditions() === 'name _ an; price >= 0.75'");
        assertEquals("[\"an\",0.75]", view.drawn().eval("JSON.stringify(data.i.filters.map(f => f.value))"));
        close(form);
    }

    // a mistake in a condition is refused, saying what it is, and the list stays as it was - a change after them is
    // made on it: an unknown property or comparison, IN ARRAY, an unknown field, a negation or a junction that is not a
    // boolean, a condition on an action or on a property grouped in columns, an array with one such condition, one with
    // no property, one that is no object
    @Test
    public void aMistakeInAConditionIsRefused() throws Exception {
        seed();
        Locator form = open(FOLDER, "UiReactFilters.uiRflConditions");
        view.drawn().call("controller.i.filters.change({property: 'name', compare: '_', value: 'pp'})");
        view.waitFor("conditions() === 'name _ pp'");
        String change = "controller.i.filters.change(): ";
        String[][] mistakes = {
                {"{property: 'nope', value: 'x'}", "unknown property 'nope' of object group 'i'"},
                {"{property: 'name', compare: '~~', value: 'x'}", "unknown compare '~~'"},
                {"{property: 'name', compare: 'IN ARRAY', value: 'x'}", "'IN ARRAY' is no comparison of a filter condition"},
                {"{property: 'name', value: 'x', foo: 1}", "unknown field 'foo'; a condition has property, compare, value, negation, or"},
                {"{property: 'name', value: 'x', negation: 'yes'}", "'negation' must be true or false"},
                {"{property: 'name', value: 'x', or: 1}", "'or' must be true or false"},
                {"{property: 'uiRflAct', value: 'x'}", "'uiRflAct' cannot be filtered by here: an action, which the server is never asked to filter by"},
                {"{property: 'uiRflPriceIn', value: 1}", "'uiRflPriceIn' cannot be filtered by here: it is grouped in columns, and a condition names no column"},
                {"[{property: 'name', value: 'x'}, {property: 'nope', value: 1}]", "condition 1: unknown property 'nope' of object group 'i'"},
                {"{value: 'x'}", "a condition has no 'property'"},
                {"7", "a condition must be an object {property, compare, value, negation, or}"},
                {"['name']", "condition 0: a condition must be an object {property, compare, value, negation, or}"}};
        for (String[] mistake : mistakes)
            assertEquals(mistake[0], change + mistake[1], view.drawn().refusal("controller.i.filters.change(" + mistake[0] + ")"));
        view.drawn().call("controller.i.filters.change({property: 'price', compare: '>', value: 1})");
        view.waitFor("conditions() === 'name _ pp; price > 1'");
        close(form);
    }

    // the conditions the form states and reads are the view's list: a FILTER action shows in it, a FILTER of none
    // clears it, and FILTERS reads back what the view stated
    @Test
    public void theFormsOperatorsOverTheConditionsAViewDraws() throws Exception {
        String[] items = seed();
        Locator form = open(FOLDER, "UiReactFilters.uiRflConditions");
        action(form, "uiRflFilterPrice").click();
        view.waitFor("conditions() === 'price > 1'");
        assertThat(names(form)).hasText(new String[] {items[0]});
        action(form, "uiRflFilterNone").click();
        view.waitFor("conditions() === ''");
        assertThat(names(form)).hasText(items);

        view.drawn().call("controller.i.filters.change({property: 'name', compare: '_', value: 'an'})");
        view.waitFor("conditions() === 'name _ an'");
        assertThat(names(form)).hasText(new String[] {items[1]});
        action(form, "uiRflReadFilters").click();
        check("UiReactFilters.uiRflCheckRead", "[{\"property\": \"name\", \"compare\": \"_\", \"negation\": false, \"value\": \"an\"}]");
        close(form);
    }

    // a form opened with a condition of its own - a FILTER of ON INIT - shows it in the view's list from the start
    @Test
    public void aFormOpenedWithACondition() throws Exception {
        String[] items = seed();
        Locator form = open(FOLDER, "UiReactFilters.uiRflOpened");
        view.waitFor("conditions() === 'name _ pp'");
        assertThat(names(form)).hasText(new String[] {items[0]});
        close(form);
    }

    // the platform's filter keys in the grid state the view's list at once, with no condition row of the platform's: F3
    // gives the current cell's value to its property - in the place of the first condition on it, its comparison kept,
    // or at the end, compared the default way -, Alt+F3 makes a new one the only one, Escape and Shift+F3 clear the
    // list; typing in a column starts no condition
    @Test
    public void thePlatformsKeysOverTheConditionsAViewDraws() throws Exception {
        String[] items = seed();
        Locator form = open(FOLDER, "UiReactFilters.uiRflConditions");
        view.drawn().call("controller.i.filters.change({property: 'name', compare: '_', value: 'an'})");
        view.waitFor("conditions() === 'name _ an'");
        assertThat(names(form)).hasText(new String[] {items[1]});
        key(cell(form, "name", items[1]), "F3");
        view.waitFor("conditions() === 'name _ " + items[1] + "'"); // the comparison of the one there
        assertThat(form.locator(".filter:visible")).hasCount(0);
        key(cell(form, "price", "0.75"), "F3");
        view.waitFor("conditions() === 'name _ " + items[1] + "; price = 0.75'");
        key(cell(form, "price", "0.75"), "Alt+F3");
        view.waitFor("conditions() === 'price = 0.75'");
        key(cell(form, "price", "0.75"), "Escape");
        view.waitFor("conditions() === ''");
        assertThat(names(form)).hasText(items);
        key(cell(form, "name", items[1]), "F3");
        view.waitFor("conditions() === 'name = " + items[1] + "'");
        key(cell(form, "name", items[1]), "Shift+F3");
        view.waitFor("conditions() === ''");
        assertThat(names(form)).hasText(items);

        // the dearer item's name typed into starts no condition - the count asked after it is of both rows -, so the
        // condition F3 then puts in on its price is the list
        click(cell(form, "name", items[0]));
        page.keyboard().type("pp");
        button(form, "Number of Entries").click();
        popup("Number of Entries: 2");
        key(cell(form, "price", "1.5"), "F3");
        view.waitFor("conditions() === 'price = 1.5'");
        assertThat(names(form)).hasText(new String[] {items[0]});
        close(form);
    }

    // the platform's filter controls over the conditions a view draws: the toolbar's filter button shows an add and a
    // reset - no apply, a change being sent at once - and no condition row of the platform's; the add puts in the
    // condition of the current cell, as F3 does, the reset clears the list, and the button hides them again
    @Test
    public void thePlatformsControlsOverTheConditionsAViewDraws() throws Exception {
        String[] items = seed();
        Locator form = open(FOLDER, "UiReactFilters.uiRflConditions");
        for (String title : new String[] {"Show filter controls", "Number of Entries", "Calculate Sum", "Export"})
            assertThat(button(form, title)).isVisible();
        Locator add = button(form, "Add filter");
        assertThat(add).isHidden();
        button(form, "Show filter controls").click();
        assertThat(add).isVisible();
        assertThat(button(form, "Reset all filters")).isVisible();
        assertThat(button(form, "Apply filters")).isHidden();
        button(form, "Number of Entries").click(); // asked after the show, the count would be of a condition it started
        popup("Number of Entries: 2");
        assertThat(form.locator(".filter:visible")).hasCount(0);
        view.waitFor("conditions() === ''");

        cell(form, "name", items[0]).click();
        add.click();
        view.waitFor("conditions() === 'name = " + items[0] + "'");
        assertThat(names(form)).hasText(new String[] {items[0]});
        button(form, "Reset all filters").click();
        view.waitFor("conditions() === ''");
        assertThat(names(form)).hasText(items);
        button(form, "Hide filter controls").click();
        assertThat(add).isHidden();
        assertThat(button(form, "Show filter controls")).isVisible();
        close(form);
    }

    // two changes in a row: the answer to the first does not take the second back - the view, drawn render by render,
    // is not drawn with the first again once it is drawn with the second
    @Test
    public void twoChangesInARow() throws Exception {
        String[] items = seed();
        Locator form = open(FOLDER, "UiReactFilters.uiRflConditions");
        view.waitFor("conditions() === ''");
        view.drawn().call("window.uiRflDrawn = []; controller.i.filters.change({property: 'name', compare: '_', value: 'pp'});"
                + " controller.i.filters.change({property: 'name', compare: '_', value: 'an'})");
        assertThat(names(form)).hasText(new String[] {items[1]});
        view.waitFor("conditions() === 'name _ an'");
        assertEquals("true", view.drawn().eval("window.uiRflDrawn.slice(window.uiRflDrawn.indexOf('name _ an')).every(drawn => drawn === 'name _ an')"));
        close(form);
    }

    // in the manual apply mode (userFiltersManualApplyMode), the platform's filter controls shown - their apply button
    // with them -, a view's change waits for the apply as every writer's does: `filters` shows it, the rows are
    // filtered once the apply sends it
    @Test
    public void aViewsChangeInTheManualApplyMode() throws Exception {
        String[] items = seed();
        exec("UiReactFilters.uiRflManualApply", "true");
        try {
            reload(); // a client is told the mode as it starts
            Locator form = open(FOLDER, "UiReactFilters.uiRflConditions");
            button(form, "Show filter controls").click();
            assertThat(button(form, "Add filter")).isVisible();
            assertThat(button(form, "Apply filters")).isVisible();
            view.drawn().call("controller.i.filters.change({property: 'name', compare: '_', value: 'pp'})");
            view.waitFor("conditions() === 'name _ pp'");
            assertThat(names(form)).hasText(items);
            button(form, "Apply filters").click();
            assertThat(names(form)).hasText(new String[] {items[0]});
            close(form);
        } finally {
            exec("UiReactFilters.uiRflManualApply", "false");
        }
    }

    // a view drawing the whole group has its conditions on the group's node beside its rows, with what a condition can
    // be on - the group's properties in the form's order, their types and comparisons -, which its entries do not
    // carry; its change and a FILTER action state the list its rows follow, which keeps its identity across renders
    // that state no condition, as one of a change of the current row
    @Test
    public void theConditionsOfTheWholeGroup() throws Exception {
        String[] items = seed();
        Locator form = open(FOLDER, "UiReactFilters.uiRflBox");
        view.waitFor("data.i.list.length === 2");
        assertEquals("true", view.drawn().eval("['list', 'byKey', 'keys', 'orders', 'properties', 'filters', 'filterProperties'].every(k => k in data.i)"
                + " && ['change', 'orders', 'filters', 'name', 'price', 'sku'].every(k => k in controller.i)"));
        assertEquals("name string,price number,sku string", view.drawn().eval("data.i.filterProperties.map(p => p.property + ' ' + p.type).join()"));
        assertEquals("true,false", view.drawn().eval("[data.i.filterProperties.every(p => p.compares.length > 0 && !!p.defaultCompare), 'compares' in data.i.name].join()"));

        view.drawn().call("window.uiRflListsDrawn = 0; controller.i.change(data.i.list[1])");
        view.waitFor("data.i.list[1].isCurrent");
        assertEquals("2", view.drawn().answer("controller.i.getCount()")); // the answer to the change has come before the count's
        assertEquals("0", view.drawn().eval("window.uiRflListsDrawn"));

        view.drawn().call("controller.i.filters.change({property: 'price', compare: '>', value: 1})");
        view.waitFor("conditions() === 'price > 1'");
        assertThat(rows(form)).hasText(new String[] {items[0]});
        view.drawn().call("controller.i.filters.change([])");
        assertThat(rows(form)).hasText(items);
        action(form, "uiRflBoxFilterPrice").click();
        view.waitFor("conditions() === 'price > 1'");
        assertThat(rows(form)).hasText(new String[] {items[0]});
        close(form);
    }

    // a view drawing a tree has the conditions of all its groups on the tree's node, each property named with its
    // group, and none on the groups' nodes: a condition named so filters its group's rows, one with no group in its
    // name is refused, and a FILTER action on a group of the tree shows in the tree's list
    @Test
    public void theConditionsOfATreeAViewDraws() throws Exception {
        String[] items = seed();
        Locator form = open(FOLDER, "UiReactFilters.uiRflTree");
        view.waitFor("data.cats.filters.length === 0 && data.c.list.length === 1");
        assertEquals("false,false,true,false,false", view.drawn().eval("['filters' in data.c, 'filters' in data.i, 'filters' in controller.cats,"
                + " 'filters' in controller.c, 'filters' in controller.i].join()"));
        view.drawn().call("controller.c.expandAll()");
        view.waitFor("data.i.list.length === 2");
        view.drawn().call("controller.cats.filters.change({property: 'i.price', compare: '>', value: 1})");
        assertThat(rows(form)).hasText(new String[] {items[0]});
        view.waitFor("conditions() === 'i.price > 1'");
        assertEquals("controller.cats.filters.change(): 'price' names no property of a group of tree 'cats': a property is named by"
                + " its group's name and its own, <group>.<property>", view.drawn().refusal("controller.cats.filters.change({property: 'price', compare: '>', value: 1})"));
        view.drawn().call("controller.cats.filters.change([])");
        assertThat(rows(form)).hasText(items);
        view.waitFor("conditions() === ''");
        action(form, "uiRflTreeFilterPrice").click();
        assertThat(rows(form)).hasText(new String[] {items[0]});
        view.waitFor("conditions() === 'i.price > 1'");
        close(form);
    }

    // a pivot's drill-down - a double click on a value cell, then its menu - states the group's conditions: under the
    // conditions a view draws they show in its list, and the pivot is narrowed to the cell; so it is with no filters in
    // the design
    @Test
    public void aPivotsDrillDownStatesTheConditions() throws Exception {
        String[] items = seed();
        Locator form = open(FOLDER, "UiReactFilters.uiRflPivot");
        drillDown(form, items);
        view.waitFor("data.i.filters.length > 0 && data.i.filters.every(f => f.property === 'name')");
        close(form);

        form = open(FOLDER, "UiReactFilters.uiRflPivotPlain");
        drillDown(form, items);
        close(form);
    }

    // the pivot shown, the dearer item's price drilled down to: the pivot's rows narrowed to it
    private void drillDown(Locator form, String[] items) {
        button(form, "Pivot").click();
        Locator labels = form.locator("th.pvtRowLabel");
        assertThat(labels.filter(new Locator.FilterOptions().setHasText(items[1]))).hasCount(1);
        form.locator("td.pvtVal").filter(new Locator.FilterOptions().setHasText("1.5")).first().dblclick();
        page.locator(".gwt-MenuItem:visible").first().click();
        assertThat(labels.filter(new Locator.FilterOptions().setHasText(items[1]))).hasCount(0);
        assertThat(labels.filter(new Locator.FilterOptions().setHasText(items[0]))).hasCount(1);
    }

    // a category with two items in it, under names no other attempt has - what a failed attempt saved stays in the
    // database for its rerun -, the dearer item first by name; returns the items
    private static String[] seed() throws Exception {
        long id = System.nanoTime();
        String[] items = {"Apple" + id, "Banana" + id};
        exec("UiReactFilters.uiRflSeed", "Fruit" + id, items[0], items[1]);
        return items;
    }

    // the names in the platform's grid, and in the rows the view draws
    private static Locator names(Locator form) {
        return form.locator("td[lsfusion-container='PROPERTY(name)']");
    }

    private static Locator rows(Locator form) {
        return form.locator(".ui-rfl-row");
    }

    @Before
    public void countRequests() {
        trackRequests();
    }

    private void reload() {
        page.reload();
        page.locator("[lsfusion-container='System.root']").waitFor();
    }

    // what the view was last drawn with, its data and its controller, as a script names them, and the conditions it is
    // given - a group's or a tree's -, each as `property compare value`
    private final View view = view("window.uiRflView",
            " const conditions = () => (data.cats || data.i).filters.map(f => f.property + ' ' + f.compare + ' ' + f.value).join('; ');");
}
