package lsfusion.tests;

import com.microsoft.playwright.Locator;
import org.junit.Test;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.Assert.assertEquals;

// a group's filter controls a React view draws (UiReactFilterControls.lsf, its views UiReactFilterControls.js): a part
// of the group - the add, the apply and the reset as members, what the apply button shows on the group's node - the
// same whoever draws the conditions, the view or the platform's panel placed back, over the platform's grid and over
// the rows the view draws; the platform's whole filter box placed back over the rows a view draws; and the platform's
// filter box and panel moved into a view's container with custom = NULL
public class UiReactFilterControlsIT extends UiTest {

    private static final String FOLDER = "UiReactFilterControls.uiRfc";

    // over the conditions the view draws too, its controls' add puts in the condition of the grid's current cell at
    // once, the reset clears the list, and the apply is refused - each change is sent at once, nothing waits for it -;
    // the platform's toolbar has no filter button, having no controls of its own to show
    @Test
    public void controlsAViewDrawsOverTheConditionsItDraws() throws Exception {
        String[] items = seed();
        Locator form = open(FOLDER, "UiReactFilterControls.uiRfcConditions");
        view.waitFor("data.i.filters.length === 0");
        assertEquals("addFilter,applyFilters,filters,resetFilters", view.drawn().eval("Object.keys(controller.i).sort().join()"));
        assertEquals("false,false", view.drawn().eval("[data.i.filtersManual, data.i.filtersPending].join()"));
        assertThat(button(form, "Show filter controls")).hasCount(0);
        assertThat(button(form, "Hide filter controls")).hasCount(0);

        cell(form, "name", items[1]).click();
        view.drawn().call("controller.i.addFilter()");
        view.waitFor("conditions() === 'name = " + items[1] + "'");
        assertThat(names(form)).hasText(new String[] {items[1]});
        view.drawn().call("controller.i.applyFilters()"); // nothing waits: nothing sent, nothing refused
        assertThat(names(form)).hasText(new String[] {items[1]});
        view.waitFor("conditions() === 'name = " + items[1] + "' && data.i.filtersManual === false && data.i.filtersPending === false");
        view.drawn().call("controller.i.resetFilters()");
        view.waitFor("conditions() === ''");
        assertThat(names(form)).hasText(items);
        cell(form, "price", "1.5").click();
        view.drawn().call("controller.i.addFilter()");
        view.waitFor("conditions() === 'price = 1.5'");
        assertThat(names(form)).hasText(new String[] {items[0]});
        view.drawn().call("controller.i.resetFilters()");
        view.waitFor("conditions() === ''");
        close(form);
    }

    // over the platform's panel, placed back with custom = NULL, the view's controls do what the panel's buttons do:
    // the add starts a condition on the current cell, its editor open, which Enter applies at once - the manual apply
    // mode being off -, and the reset takes the panel's conditions off; the platform's toolbar has its filter button,
    // which shows the panel's editors
    @Test
    public void controlsAViewDrawsOverThePlatformsPanel() throws Exception {
        String[] items = seed();
        Locator form = open(FOLDER, "UiReactFilterControls.uiRfcPanel");
        Locator panel = host(form, "FILTERS(i)"), conditions = panel.locator(".filter:visible");
        view.waitFor("data.i.filtersManual === false");
        assertEquals("addFilter,applyFilters,resetFilters/false/false", view.drawn().eval("Object.keys(controller.i).sort().join() + '/' + ('filters' in data.i)"
                + " + '/' + data.i.filtersPending"));
        assertThat(button(form, "Show filter controls")).isVisible();

        cell(form, "name", items[1]).click();
        view.drawn().call("controller.i.addFilter()");
        assertThat(conditions).hasCount(1);
        press(conditions.locator("input"), "Enter");
        assertThat(names(form)).hasText(new String[] {items[1]});
        view.drawn().call("controller.i.resetFilters()");
        assertThat(conditions).hasCount(0);
        assertThat(names(form)).hasText(items);
        close(form);
    }

    // in the manual apply mode (userFiltersManualApplyMode) the view's controls - their apply always there - say so
    // from the start; the platform's editors shown, they start a condition on the empty panel, which waits - a
    // condition committed waits for the apply -, and the apply applies it; the reset takes it off at once. The apply
    // and the reset are the Promise of their request - resolved at once with nothing to send, the batch's in a batch
    @Test
    public void controlsAViewDrawsInTheManualApplyMode() throws Exception {
        String[] items = seed();
        exec("UiReactFilterControls.uiRfcManualApply", "true");
        try {
            reload(); // a client is told the mode as it starts
            Locator form = open(FOLDER, "UiReactFilterControls.uiRfcPanel");
            Locator conditions = host(form, "FILTERS(i)").locator(".filter:visible");
            view.waitFor("data.i.filtersManual === true && data.i.filtersPending === false");
            cell(form, "name", items[1]).click();
            button(form, "Show filter controls").click();
            view.waitFor("data.i.filtersManual === true && data.i.filtersPending === true");
            assertThat(conditions).hasCount(1);
            assertThat(conditions.locator("input")).isFocused();
            assertThat(conditions.locator("input")).hasValue(items[1]);
            press(conditions.locator("input"), "Enter");
            assertThat(conditions.locator("input")).hasCount(0);
            assertThat(conditions.locator(".filter-data-property-value")).hasText(items[1]);
            view.waitFor("data.i.filtersPending === true");
            // asked after the Enter, the count would be of the condition applied
            button(form, "Number of Entries").click();
            popup("Number of Entries: 2");
            assertThat(names(form)).hasText(items);
            assertEquals("resolved", view.drawn().eval("controller.i.applyFilters().then(() => 'resolved')"));
            assertThat(names(form)).hasText(new String[] {items[1]});
            view.waitFor("data.i.filtersPending === false");
            assertEquals("resolved", view.drawn().eval("controller.form.applyFilters('i').then(() => 'resolved')")); // nothing waits
            assertEquals("resolved", view.drawn().eval("(() => { let made; const batch = controller.batch(() => { controller.form.changeFilters('i', {property: 'price', value: 1});"
                    + " made = controller.i.applyFilters(); }); return made === batch ? batch.then(() => 'resolved') : 'another Promise'; })()"));
            view.waitFor("data.i.filtersPending === false");
            assertEquals("resolved", view.drawn().eval("controller.i.resetFilters().then(() => 'resolved')"));
            assertThat(names(form)).hasText(items);
            close(form);
        } finally {
            exec("UiReactFilterControls.uiRfcManualApply", "false");
        }
    }

    // over the rows the view draws, its controls' add puts in the condition of the first column, with the value of the
    // row the view made current, and the reset clears the list
    @Test
    public void controlsAViewDrawsOverTheRowsItDraws() throws Exception {
        String[] items = seed();
        Locator form = open(FOLDER, "UiReactFilterControls.uiRfcBox");
        view.waitFor("data.i.list.length === 2");
        assertEquals("true,false,false", view.drawn().eval("[['addFilter', 'applyFilters', 'resetFilters'].every(m => m in controller.i), data.i.filtersManual,"
                + " data.i.filtersPending].join()"));
        assertEquals("resolved", view.drawn().eval("controller.i.change(data.i.list[1]).then(() => 'resolved')")); // the Promise of its request
        view.waitFor("data.i.list[1].isCurrent");
        view.drawn().call("controller.i.addFilter()");
        view.waitFor("conditions() === 'name = " + items[1] + "'");
        assertThat(rows(form)).hasText(new String[] {items[1]});
        view.drawn().call("controller.i.resetFilters()");
        view.waitFor("conditions() === ''");
        assertThat(rows(form)).hasText(items);
        close(form);
    }

    // a view drawing the rows and the filter controls, the platform's panel of the conditions placed back: the view
    // gets no conditions, and the panel in its host none of the platform's controls; the view's add starts a condition
    // there, which Enter applies to the view's rows, and its reset takes it off
    @Test
    public void controlsAViewDrawsOverThePlatformsPanelPlacedBack() throws Exception {
        String[] items = seed();
        Locator form = open(FOLDER, "UiReactFilterControls.uiRfcPlaced");
        Locator panel = host(form, "FILTERS(i)"), conditions = panel.locator(".filter:visible");
        view.waitFor("data.i.list.length === 2");
        assertEquals("false,false,true", view.drawn().eval("['filters' in data.i, 'filters' in controller.i, ['addFilter', 'applyFilters', 'resetFilters']"
                + ".every(m => m in controller.i)].join()"));
        assertThat(panel).isAttached();
        assertThat(button(panel, "Add filter")).hasCount(0);

        view.drawn().call("controller.i.addFilter()");
        assertThat(conditions).hasCount(1);
        press(conditions.locator("input"), "Enter");
        assertThat(rows(form)).hasText(new String[] {items[0]});
        view.drawn().call("controller.i.resetFilters()");
        assertThat(rows(form)).hasText(items);
        close(form);
    }

    // a view drawing the rows and the toolbar, the platform's whole filter box placed back: the view gets neither the
    // conditions nor the controls' members, the platform's controls are in the box's host, and the view's filter button
    // shows them, a condition started on the current row's name, which Enter applies to the view's rows and the
    // platform's reset takes off
    @Test
    public void thePlatformsFilterBoxOverTheRowsAViewDraws() throws Exception {
        String[] items = seed();
        Locator form = open(FOLDER, "UiReactFilterControls.uiRfcPlatformBox");
        Locator box = host(form, "FILTERBOX(i)"), conditions = box.locator(".filter:visible");
        assertThat(rows(form)).hasText(items);
        assertEquals("false,false,false", view.drawn().eval("['filters' in data.i, 'filters' in controller.i, 'addFilter' in controller.i].join()"));
        assertThat(box.locator("button[title='Add filter']")).hasCount(1);
        view.drawn().call("controller.i.change(data.i.list[1])");
        view.waitFor("data.i.list[1].isCurrent");
        view.drawn().call("controller.i.filterControlsVisible.change(true)");
        assertThat(box).isVisible();
        assertThat(button(box, "Add filter")).isVisible();
        assertThat(button(box, "Reset all filters")).isVisible();
        assertThat(conditions).hasCount(1);
        assertThat(conditions.locator("input")).hasValue(items[1]);
        press(conditions.locator("input"), "Enter");
        assertThat(rows(form)).hasText(new String[] {items[1]});
        button(box, "Reset all filters").click();
        assertThat(rows(form)).hasText(items);
        assertThat(conditions).hasCount(0);
        close(form);
    }

    // the platform's filter box - and its panel of the conditions alone - moved into a view's container with custom =
    // NULL over the platform's grid: the platform builds it there and it works, the toolbar's filter button with it, F3
    // starting its condition in the view's host, a value typed into it filtering the grid, Shift+F3 taking it off
    @Test
    public void theFiltersMovedIntoAViewsContainer() throws Exception {
        String[] items = seed();
        for (String moved : new String[] {"uiRfcMovedBox", "uiRfcMovedFilters"}) {
            Locator form = open(FOLDER, "UiReactFilterControls." + moved);
            Locator host = form.locator(".ui-rfc-host .lsf-platform");
            assertThat(button(form, "Show filter controls")).isVisible();
            key(cell(form, "name", items[1]), "F3");
            Locator editor = host.locator(".filter:visible input");
            assertThat(editor).hasValue(items[1]);
            editor.fill(items[0]);
            press(editor, "Enter");
            assertThat(names(form)).hasText(new String[] {items[0]});
            key(cell(form, "name", items[0]), "Shift+F3");
            assertThat(names(form)).hasText(items);
            close(form);
        }
    }

    // two items under names no other attempt has - what a failed attempt saved stays in the database for its rerun -,
    // the dearer one first by name
    private static String[] seed() throws Exception {
        long id = System.nanoTime();
        String[] items = {"Apple" + id, "Banana" + id};
        exec("UiReactFilterControls.uiRfcSeed", items);
        return items;
    }

    // the names in the platform's grid, and in the rows the view draws
    private static Locator names(Locator form) {
        return form.locator("td[lsfusion-container='PROPERTY(name)']");
    }

    private static Locator rows(Locator form) {
        return form.locator(".ui-rfc-row");
    }

    // the host the view places a component the platform draws in
    private static Locator host(Locator form, String component) {
        return form.locator(".lsf-platform[data-lsf-sid='" + component + "']");
    }

    private void reload() {
        page.reload();
        page.locator("[lsfusion-container='System.root']").waitFor();
    }

    // what the view was last drawn with, its data and its controller, as a script names them, and the conditions it is
    // given, each as `property compare value`
    private final View view = view("window.uiRfcView",
            " const conditions = () => data.i.filters.map(f => f.property + ' ' + f.compare + ' ' + f.value).join('; ');");
}
