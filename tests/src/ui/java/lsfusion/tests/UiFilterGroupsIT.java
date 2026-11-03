package lsfusion.tests;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.options.SelectOption;
import org.junit.Test;

import java.util.Arrays;
import java.util.regex.Pattern;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.Assert.assertEquals;

// filter groups (UiFilterGroups.lsf, its view UiFilterGroups.js): drawn by a React view - a part of the group, of a
// tree's group too, its filters selected by the view, by the server and by a filter's key -, and the platform's own - a
// check box and a list, selected by the user, by the server and by a key, a list that must have a filter selected, and
// the grid's count and sum over them
public class UiFilterGroupsIT extends UiTest {

    private static final String FOLDER = "UiFilterGroups.uiFgr";

    // a filter group a view draws is a part of its group, on the group's node under its own name - the filter selected,
    // the filters' captions, whether none may be -, its member selecting one by its index, or none, the rows following;
    // what the server selects (FILTERGROUP ... FROM) shows in it, an index out of the list is refused, and the key of a
    // filter selects it, as it does in the platform's list, pressed again keeping it
    @Test
    public void aFilterGroupAViewDraws() throws Exception {
        String[] items = seed();
        Locator form = open(FOLDER, "UiFilterGroups.uiFgrView");
        view.waitFor("data.i.list.length === 2");
        assertEquals(Arrays.asList(null, Arrays.asList("Dear", "Cheap"), true, true), view.value("[data.i.uiFgrKind.value,"
                + " data.i.uiFgrKind.filters, data.i.uiFgrKind.canClear, typeof controller.i.uiFgrKind.change === 'function']"));
        view.drawn().call("controller.i.uiFgrKind.change(0)");
        assertThat(rows(form)).hasText(new String[] {items[0]});
        view.waitFor("data.i.uiFgrKind.value === 0");
        view.drawn().call("controller.i.uiFgrKind.change(null)");
        assertThat(rows(form)).hasText(items);
        view.waitFor("data.i.uiFgrKind.value === null");
        form.locator("button[lsfusion-container='PROPERTY(uiFgrKindCheap())']").click();
        assertThat(rows(form)).hasText(new String[] {items[1]});
        view.waitFor("data.i.uiFgrKind.value === 1");
        assertEquals("controller.i.uiFgrKind.change(): takes the index of one of its filters, 0 to 1, or null for none",
                view.drawn().refusal("controller.i.uiFgrKind.change(5)"));

        view.drawn().call("controller.i.uiFgrKind.change(null)");
        assertThat(rows(form)).hasText(items);
        key(form.locator(".ui-fgr-view"), "F10");
        assertThat(rows(form)).hasText(new String[] {items[0]});
        view.waitFor("data.i.uiFgrKind.value === 0");
        key(form.locator(".ui-fgr-view"), "F10");
        assertEquals("1", view.drawn().answer("controller.i.getCount()")); // asked after the key, the count would be of both rows
        assertThat(rows(form)).hasText(new String[] {items[0]});
        view.waitFor("data.i.uiFgrKind.value === 0");
        close(form);
    }

    // a filter group of a tree's group drawn by a view is a part of that group, on its node - none on the tree's -; the
    // platform draws no check box of it, and the view's selection filters the group in the platform's tree, the
    // categories staying, the empty one too
    @Test
    public void aFilterGroupOfATreesGroupAViewDraws() throws Exception {
        String[] items = seed();
        Locator form = open(FOLDER, "UiFilterGroups.uiFgrTree");
        view.waitFor("!!data.i.uiFgrCheap");
        assertEquals(Arrays.asList(null, Arrays.asList("Cheap"), true, true, false), view.value("[data.i.uiFgrCheap.value,"
                + " data.i.uiFgrCheap.filters, data.i.uiFgrCheap.canClear, 'uiFgrCheap' in controller.i, 'cats' in data]"));
        assertThat(form.getByLabel("Cheap", new Locator.GetByLabelOptions().setExact(true))).hasCount(0);
        button(form, "Expand whole tree").click();
        assertThat(treeNames(form)).hasText(new String[] {category, items[0], items[1], emptyCategory});
        view.drawn().call("controller.i.uiFgrCheap.change(0)");
        assertThat(treeNames(form)).hasText(new String[] {category, items[1], emptyCategory});
        view.waitFor("data.i.uiFgrCheap.value === 0");
        view.drawn().call("controller.i.uiFgrCheap.change(null)");
        assertThat(treeNames(form)).hasText(new String[] {category, items[0], items[1], emptyCategory});
        view.waitFor("data.i.uiFgrCheap.value === null");
        close(form);
    }

    // the platform's filter groups: a check box for a group of one filter, off, and a list for a group of several, at
    // "(All)" then its filters; the two apply together, each keeping its selection as the other changes; the server
    // selects a filter of either and clears it (FILTERGROUP ... FROM n, 0 for none), the check box and the list showing
    // it
    @Test
    public void thePlatformsCheckBoxAndList() throws Exception {
        String[] items = seed();
        Locator form = open(FOLDER, "UiFilterGroups.uiFgrPlatform");
        Locator check = form.getByLabel("Dear only", new Locator.GetByLabelOptions().setExact(true)), list = list(form);
        assertThat(list.locator("option")).hasText(new String[] {"(All)", "Cheap", "Not cheap"});
        platformGroups(form, false, "(All)", items);

        check.check();
        platformGroups(form, true, "(All)", items[0]);
        list.selectOption(new SelectOption().setLabel("Cheap"));
        platformGroups(form, true, "Cheap");
        check.uncheck();
        platformGroups(form, false, "Cheap", items[1]);
        list.selectOption(new SelectOption().setLabel("Not cheap"));
        platformGroups(form, false, "Not cheap", items[0]);
        list.selectOption(new SelectOption().setLabel("(All)"));
        platformGroups(form, false, "(All)", items);

        action(form, "uiFgrByPriceNotCheap").click();
        platformGroups(form, false, "Not cheap", items[0]);
        action(form, "uiFgrByPriceAll").click();
        platformGroups(form, false, "(All)", items);
        action(form, "uiFgrDearOnlyOn").click();
        platformGroups(form, true, "(All)", items[0]);
        action(form, "uiFgrDearOnlyOff").click();
        platformGroups(form, false, "(All)", items);
        close(form);
    }

    // the rows the platform's check box and list leave - the server's answer, waited for -, and then both, as they
    // show after it
    private static void platformGroups(Locator form, boolean checked, String option, String... rows) {
        if (rows.length == 0)
            assertThat(names(form)).hasCount(0);
        else
            assertThat(names(form)).hasText(rows);
        Locator check = form.getByLabel("Dear only", new Locator.GetByLabelOptions().setExact(true));
        if (checked)
            assertThat(check).isChecked();
        else
            assertThat(check).not().isChecked();
        assertThat(list(form).locator("option:checked")).hasText(option);
    }

    // the grid's count and a column's sum are of the rows the filter groups leave; a text column has no sum
    @Test
    public void theCountAndTheSumOverTheFilterGroups() throws Exception {
        String[] items = seed();
        Locator form = open(FOLDER, "UiFilterGroups.uiFgrPlatform");
        assertThat(names(form)).hasText(items);
        button(form, "Number of Entries").click();
        popup("Number of Entries: 2");
        cell(form, "price", "0.75").click();
        button(form, "Calculate Sum").click();
        popup("Sum [Price]: 2.25");

        form.getByLabel("Dear only", new Locator.GetByLabelOptions().setExact(true)).check();
        assertThat(names(form)).hasText(new String[] {items[0]});
        button(form, "Number of Entries").click();
        popup("Number of Entries: 1");
        cell(form, "price", "1.5").click();
        button(form, "Calculate Sum").click();
        popup("Sum [Price]: 1.5");
        cell(form, "name", items[0]).click();
        button(form, "Calculate Sum").click();
        popup("Unable to calculate sum [Name]");
        close(form);
    }

    // a list that must have a filter selected (NONULL) has no "(All)" and starts with its default filter; another one
    // selected is the list's own, with no offset for an "(All)"; a filter's key in the grid selects it, the list
    // showing it at once
    @Test
    public void aListThatMustHaveAFilterSelected() throws Exception {
        String[] items = seed();
        Locator form = open(FOLDER, "UiFilterGroups.uiFgrNoNull");
        Locator list = list(form);
        assertThat(list.locator("option")).hasText(new String[] {"Cheap", "Dear (F10)"});
        assertThat(names(form)).hasText(new String[] {items[0]});
        assertThat(list.locator("option:checked")).hasText("Dear (F10)");
        list.selectOption(new SelectOption().setLabel("Cheap"));
        assertThat(names(form)).hasText(new String[] {items[1]});
        assertThat(list.locator("option:checked")).hasText("Cheap");
        key(cell(form, "name", items[1]), "F10");
        assertThat(names(form)).hasText(new String[] {items[0]});
        assertThat(list.locator("option:checked")).hasText("Dear (F10)");
        close(form);
    }

    // the key of a check box's filter, pressed in the grid, puts it on, and pressed again takes it off
    @Test
    public void theKeyOfACheckBoxsFilter() throws Exception {
        String[] items = seed();
        Locator form = open(FOLDER, "UiFilterGroups.uiFgrKey");
        Locator check = form.getByLabel("Dear only (F9)", new Locator.GetByLabelOptions().setExact(true));
        assertThat(check).not().isChecked();
        key(cell(form, "name", items[1]), "F9");
        assertThat(check).isChecked();
        assertThat(names(form)).hasText(new String[] {items[0]});
        key(cell(form, "name", items[0]), "F9");
        assertThat(check).not().isChecked();
        assertThat(names(form)).hasText(items);
        close(form);
    }

    // the category of the test's two items, and one with none
    private String category, emptyCategory;

    // a category with two items in it and one with none, under names no other attempt has - what a failed attempt saved
    // stays in the database for its rerun -, the dearer item first by name, the full category first; returns the items
    private String[] seed() throws Exception {
        long id = System.nanoTime();
        String[] items = {"Apple" + id, "Banana" + id};
        category = "Fruit" + id;
        emptyCategory = "Veg" + id;
        exec("UiFilterGroups.uiFgrSeed", category, emptyCategory, items[0], items[1]);
        return items;
    }

    // the names in the platform's grid or tree, and in the rows the view draws
    private static Locator names(Locator form) {
        return form.locator("td[lsfusion-container='PROPERTY(name)']");
    }

    // the rows of the platform's tree, top down, each by its category's or its item's name
    private static Locator treeNames(Locator form) {
        return form.locator("td[lsfusion-container='PROPERTY(catName)'], td[lsfusion-container='PROPERTY(name)']")
                .filter(new Locator.FilterOptions().setHasText(Pattern.compile("\\S")));
    }

    private static Locator rows(Locator form) {
        return form.locator(".ui-fgr-row");
    }

    // the platform's list of a filter group's filters
    private static Locator list(Locator form) {
        return form.locator("select.filter-group-select");
    }

    // what the view was last drawn with, its data and its controller, as a script names them
    private final View view = view("window.uiFgrView", "");
}
