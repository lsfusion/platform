package lsfusion.tests;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.PlaywrightException;
import com.microsoft.playwright.options.WaitForSelectorState;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Pattern;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

// a grid, a toolbar, filter controls, a filter group and a tree's grid drawn by a React component of their own - custom
// on the component (UiComponentView.lsf, its views UiComponentView.js) -: each view is given the part of its group or
// tree alone, stands where its component stands, and is hidden by the component's SHOWIF; one inside what a react
// container draws is placed by that container (#1893)
public class UiComponentViewIT extends UiTest {

    private static final String FOLDER = "UiComponentView.uiCv";
    // every load logs these two: chrome has no push api in an incognito context, which a test's is
    private static final Pattern PUSH_API = Pattern.compile("Push API|Registration failed - permission denied");

    // a grid's own view draws its rows where the grid stands, given the group's node alone - no entry of the grid
    // itself -, the platform's toolbar over its rows, counting them; it places a column the platform draws in each row,
    // makes a row current - the only one then -, is hidden and shown by the grid's SHOWIF; a class the design computes
    // for the grid is nothing to it, and no error (#1893)
    @Test
    public void aGridsOwnView() throws Exception {
        String[] items = seed();
        List<String> errors = errors();
        Locator form = open(FOLDER, "UiComponentView.uiCvGrid");
        Locator names = form.locator(".ui-cv-name");
        assertThat(names).hasText(items);
        assertEquals("i", eval("grid", "Object.keys(data).join()"));
        assertEquals("true", eval("grid", "['list', 'byKey', 'keys', 'orders', 'name', 'price'].every(k => k in data.i)"));
        assertThat(form.locator("[lsfusion-container='GRID(i)'] .ui-cv-grid")).isVisible();
        assertThat(form.locator("[lsfusion-container='TOOLBARSYSTEM(i)']")).isVisible();
        form.getByTitle("Number of Entries", new Locator.GetByTitleOptions().setExact(true)).click();
        popup("Number of Entries: 2");
        assertThat(form.locator(".ui-cv-price")).hasText(new String[] {"1.5", "0.75"});
        names.nth(1).click();
        assertThat(form.locator(".ui-cv-current .ui-cv-name")).hasText(new String[] {items[1]});

        action(form, "uiCvToggle").click();
        assertThat(form.locator(".ui-cv-grid")).isHidden();
        action(form, "uiCvToggle").click();
        assertThat(form.locator(".ui-cv-grid")).isVisible();
        assertThat(names).hasText(items);

        action(form, "uiCvRestyle").click();
        assertThat(value(form, "uiCvClass")).hasText("ui-cv-a");
        action(form, "uiCvRestyle").click();
        assertThat(value(form, "uiCvClass")).hasText("ui-cv-b");
        assertEquals(Collections.emptyList(), errors);
        close(form);
    }

    // a grid's own view is given the base size the platform's grid of the same columns has: the view is made before the
    // group's controllers, and the size is of the group's columns all the same (#1893)
    @Test
    public void aGridsOwnViewHasTheBaseSizeOfThePlatformsGrid() throws Exception {
        String[] items = seed();
        Locator form = open(FOLDER, "UiComponentView.uiCvGrid");
        assertThat(form.locator(".ui-cv-name")).hasText(items);
        List<String> own = size(form);
        close(form);
        form = open(FOLDER, "UiComponentView.uiCvToolbar");
        assertThat(cell(form, "name", items[0])).isVisible();
        assertEquals(size(form), own);
        assertTrue(own.toString(), own.get(0).startsWith("calc("));
        close(form);
    }

    // a toolbar's own view, over the platform's grid, stands where the toolbar does, given the group's node alone with
    // the toolbar's state and its members on the group's member; its count is of the platform's grid's rows (#1893)
    @Test
    public void aToolbarsOwnView() throws Exception {
        String[] items = seed();
        Locator form = open(FOLDER, "UiComponentView.uiCvToolbar");
        assertThat(form.locator("td[lsfusion-container='PROPERTY(name)']")).hasText(items);
        assertThat(form.locator("[lsfusion-container='TOOLBARSYSTEM(i)'] .ui-cv-toolbar")).isVisible();
        assertEquals("i", eval("toolbar", "Object.keys(data).join()"));
        assertEquals("true,true", eval("toolbar", "[['manual', 'filterControlsVisible', 'listView', 'listViews', 'updatePending'].every(k => k in data.i),"
                + " ['getCount', 'getSum', 'export', 'manual', 'update'].every(k => k in controller.i)].join()"));
        assertEquals("2", answer("toolbar", "controller.i.getCount()"));
        close(form);
    }

    // a filter group's own view, the rows a react container draws: given the group's node alone with the filter group's
    // entry, it selects a filter and none, the entry following, and the filter's key selects it; hidden by its SHOWIF,
    // the key is not taken - shown again, the rows are all there -, and shown, it is (#1893)
    @Test
    public void aFilterGroupsOwnView() throws Exception {
        String[] items = seed();
        Locator form = open(FOLDER, "UiComponentView.uiCvFilterGroup");
        Locator rows = form.locator(".ui-cv-rows"), view = form.locator(".ui-cv-filter-group");
        String all = items[0] + ", " + items[1];
        assertThat(rows).hasText(all);
        assertEquals("i", eval("filterGroup", "Object.keys(data).join()"));
        assertEquals(Arrays.asList(null, Arrays.asList("Dear", "Cheap"), true), page.evaluate("() => { " + scope("filterGroup")
                + " return [data.i.uiCvKind.value, data.i.uiCvKind.filters, data.i.uiCvKind.canClear]; }"));
        call("filterGroup", "controller.i.uiCvKind.change(0)");
        assertThat(rows).hasText(items[0]);
        assertThat(view).hasText("kind: 0");
        call("filterGroup", "controller.i.uiCvKind.change(null)");
        assertThat(rows).hasText(all);
        assertThat(view).hasText("kind: null");
        key(view, "F10");
        assertThat(rows).hasText(items[0]);
        assertThat(view).hasText("kind: 0");
        call("filterGroup", "controller.i.uiCvKind.change(null)");
        assertThat(rows).hasText(all);

        action(form, "uiCvToggle").click();
        assertThat(view).isHidden();
        key(rows, "F10");
        action(form, "uiCvToggle").click();
        assertThat(view).isVisible();
        assertThat(rows).hasText(all);
        assertThat(view).hasText("kind: null");
        key(view, "F10");
        assertThat(rows).hasText(items[0]);
        assertThat(view).hasText("kind: 0");
        close(form);
    }

    // filter controls' own view, over the platform's filters and grid: given the group's node alone with the controls'
    // state and their members, its add starts a condition in the platform's panel on the grid's current cell, which
    // Enter applies, and its reset takes it off (#1893)
    @Test
    public void filterControlsOwnView() throws Exception {
        String[] items = seed();
        Locator form = open(FOLDER, "UiComponentView.uiCvControls");
        Locator names = form.locator("td[lsfusion-container='PROPERTY(name)']"), conditions = form.locator(".filter:visible");
        assertThat(names).hasText(items);
        assertEquals("i", eval("controls", "Object.keys(data).join()"));
        assertEquals("false,false,addFilter,applyFilters,resetFilters",
                eval("controls", "[data.i.filtersPending, data.i.filtersManual, Object.keys(controller.i).sort()].join()"));
        cell(form, "name", items[1]).click();
        call("controls", "controller.i.addFilter()");
        assertThat(conditions).hasCount(1);
        idle();
        conditions.locator("input").press("Enter");
        assertThat(names).hasText(new String[] {items[1]});
        call("controls", "controller.i.resetFilters()");
        assertThat(names).hasText(items);
        close(form);
    }

    // a toolbar with a view of its own inside what a react container draws: the container's data has its entry and the
    // rows, not the toolbar's state, which is in the toolbar's own view's data; the container places the view, and once
    // it does not, the view leaves the page - placed again, it is back (#1893)
    @Test
    public void aToolbarsOwnViewPlacedByAContainer() throws Exception {
        String[] items = seed();
        Locator form = open(FOLDER, "UiComponentView.uiCvPlaced");
        assertThat(form.locator(".ui-cv-board-rows")).hasText(items[0] + ", " + items[1]);
        assertEquals("true,false", eval("board", "['TOOLBARSYSTEM(i)' in data, 'manual' in data.i].join()"));
        assertEquals("i/true", eval("placedToolbar", "Object.keys(data).join() + '/' + ('manual' in data.i)"));
        Locator placed = form.locator(".ui-cv-board .ui-cv-board-host .ui-cv-placed-toolbar");
        assertThat(placed).isVisible();
        form.locator(".ui-cv-place").click();
        assertThat(form.locator(".ui-cv-place")).hasText("Place");
        assertThat(form.locator(".ui-cv-placed-toolbar")).isHidden();
        form.locator(".ui-cv-place").click();
        assertThat(placed).isVisible();
        close(form);
    }

    // a tree's own view is given the tree's node and its groups' nodes; a hierarchical caption the design computes for
    // the tree is nothing to it, and no error (#1893)
    @Test
    public void aTreesOwnView() throws Exception {
        seed();
        List<String> errors = errors();
        Locator form = open(FOLDER, "UiComponentView.uiCvTree");
        assertThat(form.locator(".ui-cv-tree")).hasText(category);
        assertEquals("c,cats,i", eval("tree", "Object.keys(data).sort().join()"));
        action(form, "uiCvRecaption").click();
        assertThat(value(form, "uiCvCaption")).hasText("Groups of things");
        assertEquals(Collections.emptyList(), errors);
        close(form);
    }

    // the category of the test's two items
    private String category;

    // a category with two items in it, under names no other attempt has - what a failed attempt saved stays in the
    // database for its rerun -, the dearer item first by name; returns the items
    private String[] seed() throws Exception {
        long id = System.nanoTime();
        String[] items = {"Apple" + id, "Banana" + id};
        category = "Fruit" + id;
        exec("UiComponentView.uiCvSeed", category, items[0], items[1]);
        return items;
    }

    // what the popup of the count shows; the popup let go then - shown, it takes the focus, and hides as it loses it,
    // as to a click on nothing
    private void popup(String text) {
        Locator popup = page.locator("[data-tippy-root]");
        assertThat(popup).hasText(text);
        page.waitForFunction("() => !!document.activeElement && !!document.activeElement.closest('[data-tippy-root]')");
        page.evaluate("() => document.activeElement.blur()");
        assertThat(popup).hasCount(0);
    }

    // the errors the page logs from now on
    private List<String> errors() {
        List<String> errors = new CopyOnWriteArrayList<>();
        page.onConsoleMessage(message -> {
            if (message.type().equals("error") && !PUSH_API.matcher(message.text()).find())
                errors.add(message.text());
        });
        page.onPageError(error -> {
            if (!PUSH_API.matcher(error).find())
                errors.add(error);
        });
        return errors;
    }

    // the base size the layout gives the grid, or the grid's own view, as its style says
    @SuppressWarnings("unchecked")
    private static List<String> size(Locator form) {
        return (List<String>) form.locator("[lsfusion-container='GRID(i)']").evaluate("grid => [grid.style.width, grid.style.height]");
    }

    // a button of the form's own action, and the value of a property of the form
    private static Locator action(Locator form, String action) {
        return form.locator("button[lsfusion-container='PROPERTY(" + action + "())']");
    }

    private static Locator value(Locator form, String property) {
        return form.locator("[lsfusion-container='PROPERTY(" + property + "())'] .panel-renderer-value");
    }

    // a key pressed in an element once no request is in flight: the client drops the keys typed meanwhile
    private void key(Locator element, String key) {
        element.click();
        idle();
        page.keyboard().press(key);
    }

    private void idle() {
        page.locator(".lsf-blocking").waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.HIDDEN));
    }

    // what a view was last drawn with, its data and its controller, as a script names them
    private static String scope(String view) {
        return "const view = window.uiCvViews && window.uiCvViews." + view + ", data = view && view.data, controller = view && view.controller;";
    }

    // what an expression over what a view is drawn with comes to - the answer of a Promise -, as a string, once the
    // view is drawn
    private String eval(String view, String expression) {
        try {
            page.waitForFunction("() => !!(window.uiCvViews && window.uiCvViews." + view + ")", null, new Page.WaitForFunctionOptions().setTimeout(10_000));
        } catch (PlaywrightException e) {
            throw new AssertionError("the view " + view + " is never drawn", e);
        }
        return (String) page.evaluate("async () => { " + scope(view) + " return String(await (" + expression + ")); }");
    }

    // what a call of a view answers with a Promise of comes to, as a string - the Promise refused unless there is one
    private String answer(String view, String call) {
        eval(view, "true");
        return (String) page.evaluate("async () => { " + scope(view) + " const answer = " + call + ";"
                + " if (!answer || typeof answer.then !== 'function') return 'not a Promise: ' + answer; return String(await answer); }");
    }

    // a call of a view's controller, made as the view's own code makes it
    private void call(String view, String call) {
        page.evaluate("() => { " + scope(view) + " " + call + "; }");
    }
}
