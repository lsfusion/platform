package lsfusion.tests;

import com.microsoft.playwright.Locator;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.Assert.assertEquals;

// the platform's own toolbar of a grid (UiToolbar.lsf), no React view on it: its buttons and its views, the manual
// update across a pivot, a group shown as a pivot or in manual update from the start, and a manual update whose switch
// the design hides
public class UiToolbarIT extends UiTest {

    private static final String FOLDER = "UiToolbar.uiTbr";
    private static final Pattern PRESSED = Pattern.compile("\\bactive\\b");

    // the toolbar's buttons in their order - the views, the filter button, the count, the export, the manual update -,
    // and the view buttons switching the table to a pivot and back
    @Test
    public void theToolbarsButtonsAndViews() throws Exception {
        String[] items = seed();
        Locator form = open(FOLDER, "UiToolbar.uiTbrItems");
        assertThat(names(form)).hasText(items);
        List<String> order = Arrays.asList("Table", "Pivot", "Show filter controls", "Number of Entries", "Export", "Manual update");
        @SuppressWarnings("unchecked")
        List<String> titles = (List<String>) form.locator("[lsfusion-container='TOOLBARSYSTEM(i)'] button").evaluateAll("buttons => buttons.map(b => b.title)");
        assertEquals(order, titles.stream().filter(order::contains).collect(Collectors.toList()));
        assertThat(button(form, "Table")).hasClass(PRESSED);

        button(form, "Pivot").click();
        assertThat(pivot(form)).isVisible();
        assertThat(button(form, "Pivot")).hasClass(PRESSED);
        button(form, "Table").click();
        assertThat(names(form)).hasText(items);
        assertThat(button(form, "Table")).hasClass(PRESSED);
        close(form);
    }

    // the pivot sets the group's update mode as it loads and then restores the group's own: automatic - a change made
    // in the table again comes at once -, or manual - in the table again too, its rows held back, a change lighting the
    // update button, until the forced update brings them; back in automatic update, a change comes at once again
    @Test
    public void theUpdateModeAcrossAPivot() throws Exception {
        String[] items = seed();
        Locator form = open(FOLDER, "UiToolbar.uiTbrItems");
        Locator update = button(form, "Update");
        assertThat(names(form)).hasText(items);
        button(form, "Pivot").click();
        assertThat(pivot(form)).isVisible();
        button(form, "Table").click();
        assertThat(names(form)).hasText(items);
        rename(form, "1");
        assertThat(names(form)).hasText(new String[] {items[0] + "!", items[1]});
        assertThat(update).isHidden();

        button(form, "Manual update").click();
        assertThat(update).isDisabled();
        button(form, "Pivot").click();
        assertThat(pivot(form)).isVisible();
        assertThat(button(form, "Manual update")).hasClass(PRESSED);
        button(form, "Table").click();
        assertThat(update).isVisible();
        rename(form, "2");
        assertThat(update).isEnabled();
        assertThat(grid(form)).hasCSS("filter", "opacity(0.5)");
        assertThat(names(form)).hasCount(0);
        update.click();
        assertThat(names(form)).hasText(new String[] {items[0] + "!!", items[1]});
        button(form, "Manual update").click();
        assertThat(update).isHidden();
        rename(form, "3");
        assertThat(names(form)).hasText(new String[] {items[0] + "!!!", items[1]});
        assertThat(grid(form)).hasCSS("filter", "none");
        close(form);
    }

    // a group shown as a pivot from the start (PIVOT) has its pivot set up as its data comes, in automatic update: the
    // records count and the price's sum at once, a change after it at once, the table's rows at once too
    @Test
    public void aGroupShownAsAPivotFromTheStart() throws Exception {
        String[] items = seed();
        Locator form = open(FOLDER, "UiToolbar.uiTbrPivotStart");
        assertThat(pivot(form)).isVisible();
        assertThat(button(form, "Pivot")).hasClass(PRESSED);
        assertThat(totals(form)).hasText(new String[] {"2", "2.25"});
        reprice(form, "1");
        assertThat(totals(form)).hasText(new String[] {"2", "3.25"});
        assertThat(button(form, "Update")).isHidden();
        assertThat(button(form, "Manual update")).not().hasClass(PRESSED);
        button(form, "Table").click();
        assertThat(names(form)).hasText(items);
        button(form, "Pivot").click();
        assertThat(pivot(form)).isVisible();
        assertThat(button(form, "Pivot")).hasClass(PRESSED);
        close(form);
    }

    // ... in manual update (enableManualUpdate): the pivot set up restores the manual update - its data held back from
    // the start, the group's count none of its rows, brought by the update, a change after it held back again -, and
    // the table stays in it, its rows waiting for the update, which brings them, nothing held back then
    @Test
    public void aGroupShownAsAPivotFromTheStartInManualUpdate() throws Exception {
        String[] items = seed();
        // the pivot sets the group's mode up as it loads (ChangeMode): a count asked once that is answered comes after
        // the data the answer would bring
        page.waitForResponse(response -> response.request().postData() != null
                && response.request().postData().contains("action.form.ChangeMode/"), () -> open(FOLDER, "UiToolbar.uiTbrPivotManual"));
        Locator form = form("UiToolbar.uiTbrPivotManual");
        Locator update = button(form, "Update");
        assertThat(pivot(form)).isVisible();
        button(form, "Number of Entries").click();
        popup("Number of Entries: 0");
        assertThat(button(form, "Manual update")).hasClass(PRESSED);
        assertThat(update).isEnabled();
        assertThat(totals(form)).hasText(new String[] {"", ""});
        update.click();
        assertThat(totals(form)).hasText(new String[] {"2", "2.25"});
        assertThat(update).isDisabled();
        button(form, "Number of Entries").click();
        popup("Number of Entries: 2");

        reprice(form, "1");
        assertThat(update).isEnabled();
        assertThat(totals(form)).hasText(new String[] {"", ""});
        update.click();
        assertThat(totals(form)).hasText(new String[] {"2", "3.25"});
        assertThat(update).isDisabled();
        button(form, "Table").click();
        assertThat(update).isEnabled();
        assertThat(grid(form)).hasCSS("filter", "opacity(0.5)");
        assertThat(button(form, "Manual update")).hasClass(PRESSED);
        update.click();
        assertThat(names(form)).hasText(items);
        assertThat(update).isDisabled();
        assertThat(grid(form)).hasCSS("filter", "none");
        close(form);
    }

    // a group started in manual update (enableManualUpdate) shows no rows until the update brings them; a change held
    // back lights the update button and blanks the cells - the server sends the values it holds back as nothing -, and
    // the update brings it, nothing held back then
    @Test
    public void aGroupStartedInManualUpdate() throws Exception {
        String[] items = seed();
        Locator form = open(FOLDER, "UiToolbar.uiTbrManual");
        Locator update = button(form, "Update");
        assertThat(button(form, "Manual update")).hasClass(PRESSED);
        assertThat(update).isEnabled();
        assertThat(grid(form)).hasCSS("filter", "opacity(0.5)");
        assertThat(names(form)).hasCount(0);
        update.click();
        assertThat(names(form)).hasText(items);
        assertThat(update).isDisabled();
        assertThat(grid(form)).hasCSS("filter", "none");

        rename(form, "1");
        assertThat(update).isEnabled();
        assertThat(grid(form)).hasCSS("filter", "opacity(0.5)");
        assertThat(names(form)).hasText(new String[] {"", ""});
        assertThat(form.locator("td[lsfusion-container='PROPERTY(price)']")).hasText(new String[] {"", ""});
        update.click();
        assertThat(names(form)).hasText(new String[] {items[0] + "!", items[1]});
        assertThat(update).isDisabled();
        assertThat(grid(form)).hasCSS("filter", "none");
        close(form);
    }

    // a manual update whose switch the design hides (showManualUpdate = FALSE): no switch and no update button, and the
    // group in manual update all the same - the grid dimmed, the server holding its rows back -, which the pivot shown
    // and the table again keep: a change made then comes with the form's answer to it, the group's rows not (#1885)
    @Test
    public void aManualUpdateWhoseSwitchIsHidden() throws Exception {
        seed();
        Locator form = open(FOLDER, "UiToolbar.uiTbrManualHidden");
        assertThat(button(form, "Pivot")).isVisible();
        assertThat(button(form, "Manual update")).hasCount(0);
        assertThat(button(form, "Update")).hasCount(0);
        assertThat(grid(form)).hasCSS("filter", "opacity(0.5)");
        assertThat(names(form)).hasCount(0);
        button(form, "Pivot").click();
        assertThat(pivot(form)).isVisible();
        button(form, "Table").click();
        assertThat(grid(form)).isVisible();
        rename(form, "1");
        assertThat(grid(form)).hasCSS("filter", "opacity(0.5)");
        assertThat(names(form)).hasCount(0);
        close(form);
    }

    // two items under names no other attempt has - what a failed attempt saved stays in the database for its rerun -,
    // the dearer one first by name
    private static String[] seed() throws Exception {
        long id = System.nanoTime();
        String[] items = {"Apple" + id, "Banana" + id};
        exec("UiToolbar.uiTbrSeed", items);
        return items;
    }

    private static Locator names(Locator form) {
        return form.locator("td[lsfusion-container='PROPERTY(name)']");
    }

    private static Locator totals(Locator form) {
        return form.locator("td.pvtVal");
    }

    private static Locator pivot(Locator form) {
        return form.locator(".pvtTable.bodytable");
    }

    // the platform's grid, its names and the totals in its pivot
    private static Locator grid(Locator form) {
        return form.locator("table.lsf-table").first();
    }

    // a button of the platform's toolbar, by its title
    private static Locator button(Locator form, String title) {
        return form.getByTitle(title, new Locator.GetByTitleOptions().setExact(true));
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

    // the form's own actions: the dearer item renamed, the cheaper one repriced, the change applied - which a manual
    // update holds back - and counted; until the count, which nothing holds back, comes with the form's answer
    private static void rename(Locator form, String changes) {
        change(form, "uiTbrRename", changes);
    }

    private static void reprice(Locator form, String changes) {
        change(form, "uiTbrReprice", changes);
    }

    private static void change(Locator form, String action, String changes) {
        form.locator("button[lsfusion-container='PROPERTY(" + action + "())']").click();
        assertThat(form.locator("[lsfusion-container='PROPERTY(uiTbrChanges())'] .panel-renderer-value")).hasText(changes);
    }
}
