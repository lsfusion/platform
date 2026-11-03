package lsfusion.tests;

import com.microsoft.playwright.Locator;
import org.junit.Test;

import java.util.List;
import java.util.regex.Pattern;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

// the platform's editors of a grid property NOCUSTOM in the rows a React view draws (UiReactRowEditors.lsf,
// web/init/uiReactRowEditors.jsx): edits, row changes, paging, SHOWIF, the current row, filtering and sorting
public class UiReactRowEditorsIT extends UiTest {

    // every row has one host holding the platform's editor with that row's value, beside the same projected value
    @Test
    public void eachRowHasItsOwnPlatformEditor() throws Exception {
        String s = seed(3);
        Locator form = openRows();
        assertEditors(form, names(s, 1, 2, 3), 10, 20, 30);
        close(form);
    }

    // typing into one row's platform editor changes that row on the server and in the projection, leaving the others
    @Test
    public void editingOneRowChangesOnlyThatRow() throws Exception {
        String s = seed(3);
        Locator form = openRows();
        assertEditors(form, names(s, 1, 2, 3), 10, 20, 30);
        editQty(row(form, s + "-2"), 27);
        checkQty(s, 2, 27);
        checkQty(s, 1, 10);
        checkQty(s, 3, 30);
        assertEditors(form, names(s, 1, 2, 3), 10, 27, 30);
        close(form);
    }

    // adding and deleting through the view keeps the surviving editors' DOM elements; an added row edits, a deleted
    // row's editor leaves the page, and seeking the delivered page's last row replaces the editors with the next rows'
    @Test
    public void editorsFollowAddedDeletedAndPagedRows() throws Exception {
        String s = seed(3);
        Locator form = openRows();
        assertEditors(form, names(s, 1, 2, 3), 10, 20, 30);
        markEditors(form);
        click(form, "Add row");
        assertEditors(form, names(s, 1, 2, 3, 4), 10, 20, 30, 40);
        assertKept(form, names(s, 1, 2, 3));
        editQty(row(form, s + "-4"), 44);
        checkQty(s, 4, 44);
        assertEditors(form, names(s, 1, 2, 3, 4), 10, 20, 30, 44);
        editor(row(form, s + "-4")).evaluate("(e, name) => { e.uiRreKept = name; }", s + "-4");
        editor(row(form, s + "-3")).evaluate("e => { window.uiRreDeleted = e; }");
        row(form, s + "-3").locator(".ui-rre-delete").click();
        // wait for the form's response after the optimistic removal before checking editor identity
        page.evaluate("() => window.uiRreDeleteRequest");
        check("UiReactRowEditors.uiRreCheckMissing", s + "-3");
        assertEditors(form, names(s, 1, 2, 4), 10, 20, 44);
        assertThat(row(form, s + "-3")).hasCount(0);
        page.waitForFunction("() => !window.uiRreDeleted.isConnected");
        assertKept(form, names(s, 1, 2, 4));
        close(form);

        s = seed(30);
        form = openRows();
        assertThat(form.locator(".ui-rre-name")).not().hasCount(0);
        List<String> before = assertPageEditors(form);
        assertTrue("PAGESIZE 5 must deliver a subset of 30 rows: " + before, before.size() < 30);
        form.locator(".ui-rre-row").evaluateAll("rows => { window.uiRrePage = rows.map(row => ({"
                + "name: row.querySelector('.ui-rre-name').textContent, editor: row.querySelector('.panel-renderer-value')})); }");
        List<String> after = before;
        for (int move = 0; move < 3 && after.containsAll(before); move++) {
            String oldLast = after.get(after.size() - 1);
            click(form, "Next page");
            page.waitForFunction("last => { const names = document.querySelectorAll('.ui-rre-name');"
                    + " return names.length > 0 && names[names.length - 1].textContent !== last; }", oldLast);
            after = assertPageEditors(form);
        }
        assertTrue("the next page must bring new rows: " + before + " -> " + after,
                !before.containsAll(after));
        assertTrue("the next page must drop old rows: " + before + " -> " + after,
                !after.containsAll(before));
        page.waitForFunction("() => { const names = Array.from(document.querySelectorAll('.ui-rre-name'), e => e.textContent);"
                + " return window.uiRrePage.filter(old => !names.includes(old.name)).every(old => !old.editor.isConnected); }");
        String last = after.get(after.size() - 1);
        editQty(row(form, last), 99);
        check("UiReactRowEditors.uiRreCheckQty", last, "99");
        assertThat(row(form, last).locator(".ui-rre-projected")).hasText("99");
        assertThat(editor(row(form, last))).hasText("99");
        close(form);
    }

    // SHOWIF removes the visible editors and showing the property again fills every host; a returned editor edits
    @Test
    public void hiddenEditorsReturnAndCanEdit() throws Exception {
        String s = seed(3);
        Locator form = openRows();
        assertEditors(form, names(s, 1, 2, 3), 10, 20, 30);
        click(form, "Hide/show editors");
        assertThat(form.locator(".ui-rre-row")).hasCount(3);
        assertThat(form.locator(".ui-rre-qty")).hasCount(3);
        assertThat(form.locator(".ui-rre-qty .panel-renderer-value:visible")).hasCount(0);
        click(form, "Hide/show editors");
        assertEditors(form, names(s, 1, 2, 3), 10, 20, 30);
        editQty(row(form, s + "-2"), 24);
        checkQty(s, 2, 24);
        checkQty(s, 1, 10);
        checkQty(s, 3, 30);
        assertEditors(form, names(s, 1, 2, 3), 10, 24, 30);
        close(form);
    }

    // a click in the view and a seek on the server choose the row whose editor takes F2; editing another row's
    // editor addresses that row and leaves the current row and the other values alone
    @Test
    public void theCurrentRowChoosesTheKeyboardEditor() throws Exception {
        String s = seed(3);
        Locator form = openRows();
        assertEditors(form, names(s, 1, 2, 3), 10, 20, 30);
        row(form, s + "-2").locator(".ui-rre-select").click();
        assertCurrent(form, s + "-2");
        openByKey(form, row(form, s + "-2"), "20");
        pressAndWait("Escape");
        assertEditors(form, names(s, 1, 2, 3), 10, 20, 30);

        click(form, "Last from server");
        assertCurrent(form, s + "-3");
        openByKey(form, row(form, s + "-3"), "30");
        row(form, s + "-3").locator("input").fill("33");
        pressAndWait("Enter");
        checkQty(s, 3, 33);
        assertEditors(form, names(s, 1, 2, 3), 10, 20, 33);
        assertCurrent(form, s + "-3");
        editQty(row(form, s + "-1"), 11);
        checkQty(s, 1, 11);
        checkQty(s, 2, 20);
        checkQty(s, 3, 33);
        assertEditors(form, names(s, 1, 2, 3), 11, 20, 33);
        assertCurrent(form, s + "-3");
        close(form);
    }

    // filters remove rows and their editors, sorting moves each editor with its row, and dropping the filter brings
    // back editors that still edit their own rows
    @Test
    public void editorsFollowFiltersAndSorting() throws Exception {
        String s = seed(3);
        Locator form = openRows();
        assertEditors(form, names(s, 1, 2, 3), 10, 20, 30);
        editor(row(form, s + "-1")).evaluate("e => { window.uiRreFiltered = e; }");
        click(form, "Qty at least 20");
        assertEditors(form, names(s, 2, 3), 20, 30);
        assertThat(row(form, s + "-1")).hasCount(0);
        page.waitForFunction("() => !window.uiRreFiltered.isConnected");
        markEditors(form);
        click(form, "Qty descending");
        assertEditors(form, names(s, 3, 2), 30, 20);
        assertKept(form, names(s, 2, 3));
        click(form, "Drop filter");
        assertEditors(form, names(s, 3, 2, 1), 30, 20, 10);
        editQty(row(form, s + "-1"), 15);
        checkQty(s, 1, 15);
        checkQty(s, 2, 20);
        checkQty(s, 3, 30);
        assertEditors(form, names(s, 3, 2, 1), 30, 20, 15);
        close(form);
    }

    private static String seed(int count) throws Exception {
        String suffix = String.valueOf(System.nanoTime());
        exec("UiReactRowEditors.uiRreSeed", suffix, String.valueOf(count));
        return suffix;
    }

    private Locator openRows() {
        return open("UiReactRowEditors.uiRre", "UiReactRowEditors.uiRreRows");
    }

    private static String[] names(String suffix, int... ordinals) {
        String[] names = new String[ordinals.length];
        for (int i = 0; i < ordinals.length; i++)
            names[i] = suffix + "-" + ordinals[i];
        return names;
    }

    private static Locator row(Locator form, String name) {
        return form.locator(".ui-rre-row").filter(new Locator.FilterOptions().setHas(
                form.page().locator(".ui-rre-name").filter(new Locator.FilterOptions().setHasText(Pattern.compile("^" + name + "$")))));
    }

    private static Locator editor(Locator row) {
        return row.locator(".ui-rre-qty .panel-renderer-value");
    }

    private static void assertEditors(Locator form, String[] names, int... quantities) {
        assertThat(form.locator(".ui-rre-name")).hasText(names);
        assertThat(form.locator(".ui-rre-qty")).hasCount(names.length);
        assertThat(form.locator(".ui-rre-qty .panel-renderer-value")).hasCount(names.length);
        for (int i = 0; i < names.length; i++) {
            Locator row = form.locator(".ui-rre-row").nth(i), host = row.locator(".ui-rre-qty");
            assertThat(host).hasCount(1);
            assertThat(host).hasAttribute("data-lsf-sid", "i.qty");
            assertThat(host).containsClass("lsf-platform");
            assertThat(host.locator(".lsf-view-error")).hasCount(0);
            assertThat(editor(row)).hasCount(1);
            assertThat(editor(row)).isVisible();
            assertThat(editor(row)).hasText(String.valueOf(quantities[i]));
            assertThat(row.locator(".ui-rre-projected")).hasText(String.valueOf(quantities[i]));
        }
    }

    // the delivered page is a moving window; its names identify the seeded values independently of the projection
    private static List<String> assertPageEditors(Locator form) {
        List<String> names = form.locator(".ui-rre-name").allTextContents();
        assertTrue("the page has rows", !names.isEmpty());
        int[] quantities = names.stream().mapToInt(name -> Integer.parseInt(name.substring(name.lastIndexOf('-') + 1)) * 10).toArray();
        assertEditors(form, names.toArray(new String[0]), quantities);
        return names;
    }

    private void editQty(Locator row, int quantity) {
        idle();
        edit(editor(row), String.valueOf(quantity));
        idle();
    }

    private static void checkQty(String suffix, int ordinal, int quantity) throws Exception {
        check("UiReactRowEditors.uiRreCheckQty", suffix + "-" + ordinal, String.valueOf(quantity));
    }

    private static void click(Locator form, String text) {
        form.getByText(text, new Locator.GetByTextOptions().setExact(true)).click();
    }

    private static void markEditors(Locator form) {
        form.locator(".ui-rre-row").evaluateAll("rows => { rows.forEach(row => {"
                + " row.querySelector('.panel-renderer-value').uiRreKept = row.querySelector('.ui-rre-name').textContent; }); }");
    }

    private static void assertKept(Locator form, String[] names) {
        for (String name : names)
            assertEquals("the same editor for " + name, name, editor(row(form, name)).evaluate("e => e.uiRreKept"));
    }

    private static void assertCurrent(Locator form, String name) {
        assertThat(form.locator(".ui-rre-row[data-current='true'] .ui-rre-name")).hasText(name);
    }

    private void openByKey(Locator form, Locator row, String value) {
        idle();
        form.locator(".ui-rre-board").focus();
        pressAndWait("F2");
        assertThat(row.locator(".ui-rre-qty input")).isFocused();
        assertThat(row.locator(".ui-rre-qty input")).hasValue(value);
    }
}
