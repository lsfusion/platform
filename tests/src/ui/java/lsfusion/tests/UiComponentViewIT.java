package lsfusion.tests;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.PlaywrightException;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

// a grid, a toolbar, filter controls, a filter group and a tree's grid drawn by a React component of their own - custom
// on the component (UiComponentView.lsf, its views UiComponentView.js) -: each view is given the part of its group or
// tree alone, stands where its component stands, and is hidden by the component's SHOWIF; one inside what a react
// container draws is placed by that container (#1893)
public class UiComponentViewIT extends UiTest {

    private static final String FOLDER = "UiComponentView.uiCv";

    // a grid's own view draws its rows where the grid stands, given the group's node alone - no entry of the grid
    // itself -, the platform's toolbar over its rows, counting them; it places a column the platform draws in each row,
    // makes a row current - the only one then -, is hidden and shown by the grid's SHOWIF; a class the design computes
    // for the grid is nothing to it, and no error (#1893)
    @Test
    public void aGridsOwnView() throws Exception {
        String[] items = seed();
        List<String> errors = consoleErrors();
        Locator form = open(FOLDER, "UiComponentView.uiCvGrid");
        Locator names = form.locator(".ui-cv-name");
        assertThat(names).hasText(items);
        assertEquals("i", drawnView("grid").eval("Object.keys(data).join()"));
        assertEquals("true", drawnView("grid").eval("['list', 'byKey', 'keys', 'orders', 'name', 'price'].every(k => k in data.i)"));
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
        assertEquals("i", drawnView("toolbar").eval("Object.keys(data).join()"));
        assertEquals("true,true", drawnView("toolbar").eval("[['manual', 'filterControlsVisible', 'listView', 'listViews', 'updatePending'].every(k => k in data.i),"
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
        assertEquals("i", drawnView("filterGroup").eval("Object.keys(data).join()"));
        assertEquals(Arrays.asList(null, Arrays.asList("Dear", "Cheap"), true), view("filterGroup")
                .value("[data.i.uiCvKind.value, data.i.uiCvKind.filters, data.i.uiCvKind.canClear]"));
        view("filterGroup").call("controller.i.uiCvKind.change(0)");
        assertThat(rows).hasText(items[0]);
        assertThat(view).hasText("kind: 0");
        view("filterGroup").call("controller.i.uiCvKind.change(null)");
        assertThat(rows).hasText(all);
        assertThat(view).hasText("kind: null");
        focusAndPress(view, "F10");
        assertThat(rows).hasText(items[0]);
        assertThat(view).hasText("kind: 0");
        view("filterGroup").call("controller.i.uiCvKind.change(null)");
        assertThat(rows).hasText(all);

        action(form, "uiCvToggle").click();
        assertThat(view).isHidden();
        focusAndPress(rows, "F10");
        action(form, "uiCvToggle").click();
        assertThat(view).isVisible();
        assertThat(rows).hasText(all);
        assertThat(view).hasText("kind: null");
        focusAndPress(view, "F10");
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
        assertEquals("i", drawnView("controls").eval("Object.keys(data).join()"));
        assertEquals("false,false,addFilter,applyFilters,resetFilters",
                drawnView("controls").eval("[data.i.filtersPending, data.i.filtersManual, Object.keys(controller.i).sort()].join()"));
        cell(form, "name", items[1]).click();
        view("controls").call("controller.i.addFilter()");
        assertThat(conditions).hasCount(1);
        idle();
        conditions.locator("input").press("Enter");
        assertThat(names).hasText(new String[] {items[1]});
        view("controls").call("controller.i.resetFilters()");
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
        assertEquals("true,false", drawnView("board").eval("['TOOLBARSYSTEM(i)' in data, 'manual' in data.i].join()"));
        assertEquals("i/true", drawnView("placedToolbar").eval("Object.keys(data).join() + '/' + ('manual' in data.i)"));
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
        List<String> errors = consoleErrors();
        Locator form = open(FOLDER, "UiComponentView.uiCvTree");
        assertThat(form.locator(".ui-cv-tree")).hasText(category);
        assertEquals("c,cats,i", drawnView("tree").eval("Object.keys(data).sort().join()"));
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

    // the base size the layout gives the grid, or the grid's own view, as its style says
    @SuppressWarnings("unchecked")
    private static List<String> size(Locator form) {
        return (List<String>) form.locator("[lsfusion-container='GRID(i)']").evaluate("grid => [grid.style.width, grid.style.height]");
    }

    private static Locator value(Locator form, String property) {
        return form.locator("[lsfusion-container='PROPERTY(" + property + "())'] .panel-renderer-value");
    }

    private View view(String name) {
        return view("window.uiCvViews && window.uiCvViews." + name, "");
    }

    private View drawnView(String name) {
        try {
            page.waitForFunction("() => !!(window.uiCvViews && window.uiCvViews." + name + ")", null, new Page.WaitForFunctionOptions().setTimeout(10_000));
        } catch (PlaywrightException e) {
            throw new AssertionError("the view " + name + " is never drawn", e);
        }
        return view(name);
    }

    private String answer(String name, String call) {
        drawnView(name).eval("true");
        return view(name).answer(call);
    }
}
