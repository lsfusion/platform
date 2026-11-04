package lsfusion.tests;

import com.microsoft.playwright.APIResponse;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Route;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

// a table's toolbar a React view draws (UiReactToolbar.lsf, its view UiReactToolbar.js): a part of the group - what its
// buttons show on the group's node, each button a member of the group -, over the platform's grid, over the rows the
// view draws itself and over a group started in manual update; no toolbar over a group with no rows
public class UiReactToolbarIT extends UiTest {

    private static final String FOLDER = "UiReactToolbar.uiRtb";
    private static final String[] PLATFORM_TOOLBAR = {"Table", "Pivot", "Show filter controls", "Number of Entries", "Calculate Sum", "Export", "Manual update"};

    // a view drawing the toolbar over the platform's grid - TOOLBARSYSTEM moved into it - has its buttons as members of
    // the group and what they show on its node, and the platform draws no toolbar of its own; the count and a column's
    // sum are the server's, as Promises, a sum of what is not a number property refused
    @Test
    public void aToolbarOverThePlatformsGrid() throws Exception {
        String[] items = seed();
        Locator form = open(FOLDER, "UiReactToolbar.uiRtbHeader");
        view.waitFor("data.i.listView === 'grid'");
        assertEquals("export,filterControlsVisible,getCount,getSum,listView,manual,update", view.eval("Object.keys(controller.i).sort().join()"));
        assertEquals("false,false,false,grid,grid,pivot",
                view.eval("[data.i.manual, data.i.updatePending, data.i.filterControlsVisible, data.i.listView, data.i.listViews].join()"));
        assertThat(cell(form, "name", items[0])).isVisible(); // the grid stays the platform's
        assertThat(cell(form, "name", items[1])).isVisible();
        for (String button : PLATFORM_TOOLBAR)
            assertThat(button(form, button)).hasCount(0);

        assertEquals("2", view.answer("controller.i.getCount()"));
        assertEquals("2.25", view.answer("controller.i.getSum('price')"));
        assertEquals("controller.i.getSum(): unknown property 'nope' of object group 'i'", view.refusal("controller.i.getSum('nope')"));
        assertEquals("controller.i.getSum(): 'name' is not a number", view.refusal("controller.i.getSum('name')"));

        // the export: the server's report of the rows, an Excel workbook, which the client opens in a window of its own
        // - the workbook as the server answered it, before the browser gets it
        List<byte[]> reports = new CopyOnWriteArrayList<>();
        page.context().route(Pattern.compile(".*/lsfReport\\.xlsx.*"), route -> {
            APIResponse report = route.fetch();
            if (report.ok())
                reports.add(report.body());
            route.fulfill(new Route.FulfillOptions().setResponse(report));
        });
        view.call("controller.i['export']()");
        page.waitForCondition(() -> !reports.isEmpty());
        String workbook = workbook(reports.get(0));
        assertTrue(workbook.contains(items[0]) && workbook.contains(items[1]));
        close(form);
    }

    // the filter button's member shows and hides the platform's filter controls - on the empty panel, with a condition
    // to fill in, which stays while they are hidden -, and what the toolbar shows follows the platform's own change
    // too: the panel's reset, which hides the controls of an empty panel
    @Test
    public void itsFilterButtonShowsThePlatformsControls() throws Exception {
        seed();
        Locator form = open(FOLDER, "UiReactToolbar.uiRtbHeader");
        view.waitFor("data.i.filterControlsVisible === false");
        Locator add = button(form, "Add filter"), conditions = form.locator(".filter:visible");
        assertThat(add).isHidden();

        view.call("controller.i.filterControlsVisible.change(true)");
        assertThat(add).isVisible();
        assertThat(button(form, "Reset all filters")).isVisible();
        assertThat(conditions).hasCount(1);
        view.waitFor("data.i.filterControlsVisible === true");

        view.call("controller.i.filterControlsVisible.change(false)");
        assertThat(add).isHidden();
        view.waitFor("data.i.filterControlsVisible === false");

        view.call("controller.i.filterControlsVisible.change(true)");
        assertThat(conditions).hasCount(1); // the one it started, no other
        button(form, "Reset all filters").click();
        assertThat(conditions).hasCount(0);
        assertThat(add).isHidden();
        view.waitFor("data.i.filterControlsVisible === false");
        close(form);
    }

    // the manual update's member turns it on: a change made then is held back - what the toolbar shows says so - until
    // the update's member brings it; the member takes a boolean only
    @Test
    public void itsManualUpdate() throws Exception {
        String[] items = seed();
        Locator form = open(FOLDER, "UiReactToolbar.uiRtbHeader");
        view.waitFor("data.i.manual === false");
        view.call("controller.i.manual.change(true)");
        view.waitFor("data.i.manual === true && data.i.updatePending === false");
        rename(form);
        view.waitFor("data.i.updatePending === true");
        assertThat(names(form)).hasText(new String[] {"", ""}); // the server sends what it holds back as nothing
        view.call("controller.i.update()");
        assertThat(names(form)).hasText(new String[] {items[0] + "!", items[1]});
        view.waitFor("data.i.updatePending === false");

        assertEquals("controller.i.manual.change(): takes true or false", view.refusal("controller.i.manual.change('yes')"));
        view.call("controller.i.manual.change(false)");
        view.waitFor("data.i.manual === false");
        close(form);
    }

    // the view switch's member shows the platform's table as a pivot and back, as the platform's buttons do, and says
    // which views the design gives; another view is refused
    @Test
    public void itsViewSwitch() throws Exception {
        String[] items = seed();
        Locator form = open(FOLDER, "UiReactToolbar.uiRtbHeader");
        view.waitFor("data.i.listView === 'grid'");
        view.call("controller.i.listView.change('pivot')");
        assertThat(form.locator(".pvtTable.bodytable")).isVisible();
        view.waitFor("data.i.listView === 'pivot'");
        view.call("controller.i.listView.change('grid')");
        assertThat(names(form)).hasText(items);
        view.waitFor("data.i.listView === 'grid'");
        assertEquals("controller.i.listView.change(): takes the name of a view of the table: grid, pivot",
                view.refusal("controller.i.listView.change('map')"));
        close(form);
    }

    // a pivot switched to and away again in one batch, replaced before it took the user's update mode back, leaves the
    // group in the user's mode: a change made then is shown at once, nothing held back
    @Test
    public void aPivotSwitchedToAndAwayInABatchLeavesTheUsersUpdateMode() throws Exception {
        String[] items = seed();
        Locator form = open(FOLDER, "UiReactToolbar.uiRtbHeader");
        view.waitFor("data.i.manual === false && data.i.listView === 'grid'");
        view.call("controller.batch(() => { controller.i.listView.change('pivot'); controller.i.listView.change('grid'); })");
        view.waitFor("data.i.listView === 'grid'");
        assertThat(names(form)).hasText(items);
        rename(form);
        assertThat(names(form)).hasText(new String[] {items[0] + "!", items[1]});
        view.waitFor("data.i.manual === false && data.i.updatePending === false");
        close(form);
    }

    // a view drawing the whole group has the toolbar's members and states all the same, with nothing of the platform's
    // to show or to switch - its filter controls' member and the view switch refused -; the count and the manual update
    // work over the rows it draws
    @Test
    public void aToolbarOverTheRowsTheViewDraws() throws Exception {
        String[] items = seed();
        Locator form = open(FOLDER, "UiReactToolbar.uiRtbBox");
        view.waitFor("data.i.list.length === 2");
        assertEquals("true", view.eval("['getCount', 'getSum', 'export', 'manual', 'update', 'filterControlsVisible', 'listView'].every(m => m in controller.i)"));
        assertEquals("false,false,false,null,0",
                view.eval("[data.i.manual, data.i.updatePending, data.i.filterControlsVisible, String(data.i.listView), data.i.listViews.length].join()"));
        for (String button : PLATFORM_TOOLBAR)
            assertThat(button(form, button)).hasCount(0);

        assertEquals("controller.i.filterControlsVisible.change(): has no filter controls of the platform's to show: the platform "
                + "draws neither the conditions nor FILTERCONTROLS here, or its toolbar turns its filters off (showFilters)",
                view.refusal("controller.i.filterControlsVisible.change(true)"));
        assertEquals("controller.i.listView.change(): has no view to switch to: no table of the platform's draws the rows",
                view.refusal("controller.i.listView.change('pivot')"));
        assertEquals("2", view.answer("controller.i.getCount()"));

        view.call("controller.i.manual.change(true)");
        view.waitFor("data.i.manual === true");
        rename(form);
        view.waitFor("data.i.updatePending === true && data.i.list.length === 2 && data.i.list.every(row => row.name.value === null)");
        view.call("controller.i.update()");
        view.waitFor("data.i.updatePending === false && names() === '" + items[0] + "!," + items[1] + "'");
        close(form);
    }

    // over a group started in manual update (enableManualUpdate) whose switch the design hides on the platform's
    // toolbar, a view's toolbar has its members and states all the same: the data held back from the start, brought by
    // update(); the pivot shown keeps the manual update, its data held back - its totals empty -, and so does the grid
    // again - no rows - until update() brings them (#1885); manual.change(false) brings what is held back at once
    @Test
    public void aToolbarOverAGroupStartedInManualUpdate() throws Exception {
        String[] items = seed();
        Locator form = open(FOLDER, "UiReactToolbar.uiRtbManual");
        view.waitFor("data.i.manual === true && data.i.updatePending === true && data.i.listView === 'grid'");
        assertThat(names(form)).hasCount(0);
        view.call("controller.i.update()");
        assertThat(names(form)).hasText(items);
        view.waitFor("data.i.updatePending === false");

        rename(form);
        view.waitFor("data.i.updatePending === true");
        assertThat(names(form)).hasText(new String[] {"", ""}); // the server sends what it holds back as nothing
        view.call("controller.i.update()");
        assertThat(names(form)).hasText(new String[] {items[0] + "!", items[1]});
        view.waitFor("data.i.updatePending === false");

        view.call("controller.i.listView.change('pivot')");
        assertThat(form.locator(".pvtTable.bodytable")).isVisible();
        view.waitFor("data.i.listView === 'pivot' && data.i.manual === true && data.i.updatePending === true");
        // the totals all empty
        page.waitForFunction("() => { const values = document.querySelectorAll(\"[lsfusion-form='UiReactToolbar.uiRtbManual'] td.pvtVal\");"
                + " return values.length > 0 && Array.from(values).every(value => !value.textContent.trim()); }");
        view.call("controller.i.listView.change('grid')");
        view.waitFor("data.i.listView === 'grid' && data.i.manual === true && data.i.updatePending === true");
        assertThat(form.locator("table.lsf-table").first()).isVisible();
        assertThat(names(form)).hasCount(0);
        view.call("controller.i.update()");
        assertThat(names(form)).hasText(new String[] {items[0] + "!", items[1]});

        rename(form);
        view.waitFor("data.i.updatePending === true");
        assertThat(names(form)).hasText(new String[] {"", ""});
        view.call("controller.i.manual.change(false)");
        assertThat(names(form)).hasText(new String[] {items[0] + "!!", items[1]});
        view.waitFor("data.i.manual === false && data.i.updatePending === false");
        close(form);
    }

    // a group in PANEL view has no rows: a view's container holding its grid gets no toolbar over them, as the
    // platform's grid would not
    @Test
    public void noToolbarOverAPanelGroup() throws Exception {
        seed();
        Locator form = open(FOLDER, "UiReactToolbar.uiRtbPanel");
        assertThat(form.locator(".ui-rtb-view")).isVisible();
        for (String button : PLATFORM_TOOLBAR)
            assertThat(button(form, button)).hasCount(0);
        close(form);
    }

    // two items under names no other attempt has - what a failed attempt saved stays in the database for its rerun -,
    // the dearer one first by name
    private static String[] seed() throws Exception {
        long id = System.nanoTime();
        String[] items = {"Apple" + id, "Banana" + id};
        exec("UiReactToolbar.uiRtbSeed", items);
        return items;
    }

    // the names in the platform's grid
    private static Locator names(Locator form) {
        return form.locator("td[lsfusion-container='PROPERTY(name)']");
    }

    // what a workbook holds, the text of every part of it
    private static String workbook(byte[] workbook) throws IOException {
        StringBuilder text = new StringBuilder();
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(workbook))) {
            for (ZipEntry entry = zip.getNextEntry(); entry != null; entry = zip.getNextEntry())
                text.append(new String(zip.readAllBytes(), StandardCharsets.UTF_8));
        }
        return text.toString();
    }

    // the form's own action: the dearer item renamed and the change applied, which a manual update holds back
    private static void rename(Locator form) {
        form.locator("button[lsfusion-container='PROPERTY(uiRtbRename())']").click();
    }

    // what the view was last drawn with, its data and its controller, as a script names them, and the names of the rows
    // it draws
    private final View view = view("window.uiRtbView",
            " const names = () => data.i.list.map(row => row.name.value).join();");
}
