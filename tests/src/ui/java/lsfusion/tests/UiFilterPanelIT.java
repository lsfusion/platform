package lsfusion.tests;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.WaitForSelectorState;
import org.junit.Test;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

// the platform's own filter panel over its grid (UiFilterPanel.lsf), no React view on it: its keys, its manual apply
// mode, a quick filter on another column, and the toolbars that turn the filters off
public class UiFilterPanelIT extends UiTest {

    private static final String FOLDER = "UiFilterPanel.uiFpn";

    // F3 starts a condition on the current cell, the cell's value in its editor, which a value typed and Enter apply;
    // Shift+F3 takes every condition off; F3 and Enter apply the cell's own value; Escape in the grid takes the
    // conditions off; a key typed in a read-only column starts a condition of its own, the key in its editor
    @Test
    public void thePanelsKeys() throws Exception {
        String[] items = seed();
        Locator form = open(FOLDER, "UiFilterPanel.uiFpnItems");
        Locator editor = form.locator(".filter:visible input");
        assertThat(names(form)).hasText(items);
        key(cell(form, "name", items[1]), "F3");
        assertThat(editor).hasValue(items[1]);
        assertThat(editor).isFocused();
        editor.fill(items[0]);
        press(editor, "Enter");
        assertThat(names(form)).hasText(new String[] {items[0]});
        key(cell(form, "name", items[0]), "Shift+F3");
        assertThat(names(form)).hasText(items);

        key(cell(form, "name", items[1]), "F3");
        assertThat(editor).hasValue(items[1]);
        assertThat(editor).isFocused();
        press(editor, "Enter");
        assertThat(names(form)).hasText(new String[] {items[1]});
        key(cell(form, "name", items[1]), "Escape");
        assertThat(names(form)).hasText(items);

        key(cell(form, "name", items[1]), "A");
        assertThat(editor).hasValue("A");
        assertThat(editor).isFocused();
        editor.fill(items[0]);
        press(editor, "Enter");
        assertThat(names(form)).hasText(new String[] {items[0]});
        key(cell(form, "name", items[0]), "Shift+F3");
        assertThat(names(form)).hasText(items);
        close(form);
    }

    // in the manual apply mode (userFiltersManualApplyMode) the panel holds a condition committed back while its
    // controls are shown, lighting its apply button, which applies it; with the controls hidden, a change of the
    // condition applies as it is made
    @Test
    public void thePanelsManualApplyMode() throws Exception {
        String[] items = seed();
        exec("UiFilterPanel.uiFpnManualApply", "true");
        try {
            reload(); // a client is told the mode as it starts
            Locator form = open(FOLDER, "UiFilterPanel.uiFpnItems");
            Locator editor = form.locator(".filter:visible input"), apply = button(form, "Apply filters");
            key(cell(form, "name", items[1]), "F3");
            editor.fill(items[0]);
            press(editor, "Enter");
            assertThat(apply).isEnabled();
            // asked after the Enter, the count would be of the condition applied
            button(form, "Number of Entries").click();
            popup("Number of Entries: 2");
            assertThat(names(form)).hasText(items);
            apply.click();
            assertThat(names(form)).hasText(new String[] {items[0]});

            button(form, "Hide filter controls").click();
            assertThat(apply).isHidden();
            edit(form.locator(".filter:visible .filter-data-property-value"), items[1]);
            assertThat(names(form)).hasText(new String[] {items[1]});
            button(form, "Show filter controls").click();
            button(form, "Reset all filters").click();
            assertThat(names(form)).hasText(items);
            close(form);
        } finally {
            exec("UiFilterPanel.uiFpnManualApply", "false");
        }
    }

    // a key typed in a cell of a column with a QUICKFILTER starts a condition on the column it names - typed in a price
    // cell, on the name -, the key in its editor; Enter applies it, and Escape in the grid takes it off
    @Test
    public void aQuickFilterOnAnotherColumn() throws Exception {
        String[] items = seed();
        Locator form = open(FOLDER, "UiFilterPanel.uiFpnQuick");
        Locator condition = form.locator(".filter:visible"), editor = condition.locator("input");
        key(cell(form, "price", "0.75"), "B");
        assertThat(condition).hasCount(1);
        assertThat(condition.locator(".filter-label").first()).hasText("Name");
        assertThat(editor).hasValue("B");
        assertThat(editor).isFocused();
        editor.fill("Banana");
        press(editor, "Enter");
        assertThat(names(form)).hasText(new String[] {items[1]});
        key(cell(form, "price", "0.75"), "Escape");
        assertThat(condition).hasCount(0);
        assertThat(names(form)).hasText(items);
        close(form);
    }

    // a condition the server states (FILTER) shows on the panel, filtering the grid; the filter button shows the
    // panel's controls over it, starting no other, and hides them leaving it shown and applied
    @Test
    public void theFilterButtonOverAConditionTheServerStates() throws Exception {
        String[] items = seed();
        Locator form = open(FOLDER, "UiFilterPanel.uiFpnItems");
        Locator conditions = form.locator(".filter:visible");
        assertThat(names(form)).hasText(items);
        form.locator("button[lsfusion-container='PROPERTY(uiFpnFilterDear())']").click();
        assertThat(names(form)).hasText(new String[] {items[0]});
        assertThat(conditions).hasCount(1);
        button(form, "Show filter controls").click();
        assertThat(button(form, "Add filter")).isVisible();
        assertThat(conditions).hasCount(1);
        button(form, "Hide filter controls").click();
        assertThat(button(form, "Add filter")).isHidden();
        // asked after the controls hide, the count would be of the conditions they sent
        button(form, "Number of Entries").click();
        popup("Number of Entries: 1");
        assertThat(conditions).hasCount(1);
        assertThat(names(form)).hasText(new String[] {items[0]});
        close(form);
    }

    // the filter button over an empty panel shows its controls with a condition to fill in - on the grid's first
    // property, its editor focused -, and hides them, the condition untouched, filtering nothing
    @Test
    public void theFilterButtonOverAnEmptyPanel() throws Exception {
        String[] items = seed();
        Locator form = open(FOLDER, "UiFilterPanel.uiFpnItems");
        Locator conditions = form.locator(".filter:visible");
        assertThat(names(form)).hasText(items);
        assertThat(conditions).hasCount(0);
        button(form, "Show filter controls").click();
        assertThat(button(form, "Add filter")).isVisible();
        assertThat(conditions).hasCount(1);
        assertThat(conditions.locator(".filter-label").first()).hasText("Name");
        assertThat(conditions.locator("input")).isFocused();
        button(form, "Hide filter controls").click();
        assertThat(button(form, "Add filter")).isHidden();
        // asked after the controls hide, the count would be of a condition they applied
        button(form, "Number of Entries").click();
        popup("Number of Entries: 2");
        assertThat(names(form)).hasText(items);
        close(form);
    }

    // a QUICKFILTER of a property of a group in PANEL view: a key typed in a name cell starts a condition on it, which
    // no list of the grid's takes - left out, said on the console -, the rows staying - a value no category has, which
    // sent would leave none -, the condition on the panel; Escape in the grid takes it off
    @Test
    public void aQuickFilterOnAPanelGroupsProperty() throws Exception {
        String[] items = seed();
        Locator form = open(FOLDER, "UiFilterPanel.uiFpnQuick");
        Locator condition = form.locator(".filter:visible"), editor = condition.locator("input");
        key(cell(form, "name", items[0]), "F");
        assertThat(condition).hasCount(1);
        assertThat(editor).isFocused();
        assertThat(editor).hasValue("F");
        editor.fill("NoMatch" + System.nanoTime());
        idle();
        page.waitForConsoleMessage(new Page.WaitForConsoleMessageOptions().setPredicate(message -> message.text().equals(
                "the filters of 'i': the condition on 'PROPERTY(catName)' is on a property of group 'c', which is none of theirs, so it is not sent")),
                () -> editor.press("Enter"));
        button(form, "Number of Entries").click(); // asked after the Enter, the count would be of the condition sent
        popup("Number of Entries: 2");
        assertThat(names(form)).hasText(items);
        assertThat(condition).hasCount(1);
        key(cell(form, "name", items[0]), "Escape");
        assertThat(condition).hasCount(0);
        close(form);
    }

    // a grid's and a tree's toolbars that turn the filters off (showFilters = FALSE) have no filter button, the tree's
    // that turns its export off (showPrintGroupXls = FALSE) no export, as the grid's toolbar does (#1885); and F3 in
    // the grid starts no condition
    @Test
    public void toolbarsThatTurnTheFiltersOff() throws Exception {
        String[] items = seed();
        Locator form = open(FOLDER, "UiFilterPanel.uiFpnNoFilters");
        Locator tree = form.locator("[lsfusion-container='TOOLBARSYSTEM(TREE cats)']");
        assertThat(button(form, "Number of Entries")).isVisible();
        assertThat(button(tree, "Expand whole tree")).isVisible();
        assertThat(button(form, "Show filter controls")).hasCount(0);
        assertThat(button(tree, "Export")).hasCount(0);
        assertThat(button(form, "Export")).hasCount(1);
        key(cell(form, "name", items[1]), "F3");
        button(form, "Number of Entries").click(); // asked after F3, the count would be of a condition it started
        popup("Number of Entries: 2");
        assertThat(form.locator(".filter:visible")).hasCount(0);
        assertThat(names(form)).hasText(items);
        close(form);
    }

    // a category with two items in it, under names no other attempt has - what a failed attempt saved stays in the
    // database for its rerun -, the dearer item first by name; returns the items
    private static String[] seed() throws Exception {
        long id = System.nanoTime();
        String[] items = {"Apple" + id, "Banana" + id};
        exec("UiFilterPanel.uiFpnSeed", "Fruit" + id, items[0], items[1]);
        return items;
    }

    // the names in the platform's grid
    private static Locator names(Locator form) {
        return form.locator("td[lsfusion-container='PROPERTY(name)']");
    }


    // what the popup of the count or of the sum shows; the popup let go then - shown, it takes the focus, and hides as
    // it loses it, as to a click on nothing
    private void popup(String text) {
        Locator popup = page.locator("[data-tippy-root]");
        assertThat(popup).hasText(text);
        page.waitForFunction("() => !!document.activeElement && !!document.activeElement.closest('[data-tippy-root]')");
        page.evaluate("() => document.activeElement.blur()");
        assertThat(popup).hasCount(0);
    }
    // a button of the platform's toolbar or filter controls, by its title
    private static Locator button(Locator scope, String title) {
        return scope.getByTitle(title, new Locator.GetByTitleOptions().setExact(true));
    }

    // a key pressed in a cell, or into an editor, once no request is in flight: the client drops the keys typed
    // meanwhile, and a click on a cell of another row sends one
    private void key(Locator cell, String key) {
        if (Boolean.TRUE.equals(cell.evaluate("element => !!element.closest('tr') && !element.closest('tr').classList.contains('table-active')")))
            page.waitForRequest(request -> request.postData() != null && request.postData().contains("ChangeGroupObject"),
                    () -> cell.click());
        else
            cell.click();
        idle();
        page.keyboard().press(key);
    }

    private void press(Locator editor, String key) {
        idle();
        editor.press(key);
    }

    private void idle() {
        page.locator(".lsf-blocking").waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.HIDDEN));
    }

    private void reload() {
        page.reload();
        page.locator("[lsfusion-container='System.root']").waitFor();
    }
}
