package lsfusion.tests;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.assertions.LocatorAssertions;
import org.junit.Test;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

// FILTERs of properties the platform draws as condition rows: rows of the user filters a React view draws the
// conditions of, and a fixed row of the platform's filter panel
public class UiPlatformFilterRowIT extends UiTest {

    // a row's value read as the page shows it (innerText), with no text hidden in it
    private static final LocatorAssertions.ContainsTextOptions SHOWN = new LocatorAssertions.ContainsTextOptions().setUseInnerText(true);

    // where a React view draws the conditions of the group (FILTERS(i) { custom = ... }), a FILTER the platform draws -
    // the name's, standing outside React, and the city's, which the view places (custom = NULL) - is a row of the
    // group's user filters, its caption and comparison shown, and no filter of the view's: a value typed into it states
    // its condition, which the view lists, and a condition the view states shows in it, with its comparison, kept when
    // the view takes the condition off, the row emptied
    @Test
    public void rowsOverAViewsConditions() throws Exception {
        long id = System.nanoTime(); // unique : the items of a failed attempt stay in the database for its rerun
        String apple = "Apple" + id, banana = "Banana" + id, riga = "Riga" + id;
        exec("UiPlatformFilterRow.uiPfrSeed", apple, riga);
        exec("UiPlatformFilterRow.uiPfrSeed", banana, "Vilnius" + id);

        Locator form = open("UiPlatformFilterRow.uiPfr", "UiPlatformFilterRow.uiPfrOverView");
        Locator name = form.locator("[lsfusion-container='FILTER(uiPfrName(i))']"), city = form.locator("[data-lsf-sid='FILTER(uiPfrCity(i))']");
        Locator conditions = form.locator(".ui-pfr-conditions");
        assertThat(conditions).isEmpty();
        assertThat(form.locator(".ui-pfr-parts")).hasText("no filter, city entry");
        assertThat(labels(name)).hasText(new String[] {"Name", "@"}); // a string's default comparison
        assertThat(labels(city)).hasText(new String[] {"City", "@"});
        shown(form, apple);
        shown(form, banana);

        String nana = banana.substring(2); // matched by '@' as a part of the name, not as all of it
        type(name, nana);
        gone(form, apple);
        shown(form, banana);
        assertThat(conditions).hasText("uiPfrName @ " + nana);

        form.locator(".ui-pfr-value").fill(apple);
        form.locator(".ui-pfr-equals").click();
        shown(form, apple);
        gone(form, banana);
        assertThat(conditions).hasText("uiPfrName = " + apple);
        assertThat(name).containsText(apple, SHOWN);
        assertThat(labels(name)).hasText(new String[] {"Name", "="});

        form.locator(".ui-pfr-clear").click();
        shown(form, apple);
        shown(form, banana);
        assertThat(conditions).isEmpty();
        assertThat(name).not().containsText(apple, SHOWN);
        assertThat(labels(name)).hasText(new String[] {"Name", "="});

        type(city, riga);
        shown(form, apple);
        gone(form, banana);
        assertThat(conditions).hasText("uiPfrCity @ " + riga);

        form.locator(".ui-pfr-city-off").click();
        shown(form, apple);
        shown(form, banana);
        assertThat(conditions).isEmpty();
        assertThat(city).not().containsText(riga, SHOWN);
        close(form);
    }

    // ... the toolbar's filter button shows such a row's selectors, as it shows a fixed row's of the panel: its
    // comparison's alone - the property is the FILTER's -, a comparison chosen in it stating its condition at once
    @Test
    public void aRowOverAViewShowsItsControlsByTheFilterButton() throws Exception {
        long id = System.nanoTime(); // unique : the items of a failed attempt stay in the database for its rerun
        String apple = "Apple" + id, banana = "Banana" + id, nana = banana.substring(2);
        exec("UiPlatformFilterRow.uiPfrSeed", apple, "Riga" + id);
        exec("UiPlatformFilterRow.uiPfrSeed", banana, "Vilnius" + id);

        Locator form = open("UiPlatformFilterRow.uiPfr", "UiPlatformFilterRow.uiPfrOverView");
        Locator name = form.locator("[lsfusion-container='FILTER(uiPfrName(i))']");
        Locator conditions = form.locator(".ui-pfr-conditions"), selectors = name.locator(".filter-selector:visible");
        type(name, nana);
        assertThat(conditions).hasText("uiPfrName @ " + nana);
        assertThat(selectors).hasCount(0);

        form.locator("button:has(i.bi-funnel)").click(); // the toolbar's filter button
        assertThat(selectors).hasCount(1);
        selectors.click();
        page.locator("li:text-is('=')").click();
        assertThat(conditions).hasText("uiPfrName = " + nana);
        gone(form, banana); // a part of the name is equal to no name
        gone(form, apple);
        close(form);
    }

    // ... and in the manual apply mode (userFiltersManualApplyMode), the platform's filter controls shown by that
    // button, a value typed into such a row waits for their apply, as any row's: the view lists it, the rows filtered
    // once the apply sends it
    @Test
    public void aRowOverAViewWaitsForTheApplyInManualMode() throws Exception {
        long id = System.nanoTime(); // unique : the items of a failed attempt stay in the database for its rerun
        String apple = "Apple" + id, banana = "Banana" + id, nana = banana.substring(2);
        exec("UiPlatformFilterRow.uiPfrSeed", apple, "Riga" + id);
        exec("UiPlatformFilterRow.uiPfrSeed", banana, "Vilnius" + id);
        exec("UiReactFilters.uiRflManualApply", "true");
        try {
            reload(); // a client is told the mode as it starts
            Locator form = open("UiPlatformFilterRow.uiPfr", "UiPlatformFilterRow.uiPfrOverView");
            Locator name = form.locator("[lsfusion-container='FILTER(uiPfrName(i))']");
            form.locator("button:has(i.bi-funnel)").click(); // the toolbar's filter button: the controls, their apply
            type(name, nana);
            assertThat(form.locator(".ui-pfr-conditions")).hasText("uiPfrName @ " + nana);
            shown(form, apple); // nothing applied yet
            button(form, "Apply filters").click();
            gone(form, apple);
            shown(form, banana);
            close(form);
        } finally {
            exec("UiReactFilters.uiRflManualApply", "false");
        }
    }

    // the grid shows an item, by its name - or has none of it
    private void shown(Locator form, String name) {
        assertThat(cell(form, "uiPfrName(i)", name)).isVisible();
    }
    private void gone(Locator form, String name) {
        assertThat(cell(form, "uiPfrName(i)", name)).hasCount(0);
    }

    // a fixed row of the platform's filter panel - the name's FILTER - shows the comparison of a condition the server
    // states in the label it shows with the panel's controls hidden: it kept the comparison it had before
    @Test
    public void aFixedRowShowsTheComparisonOfAConditionTheServerStates() throws Exception {
        Locator form = open("UiPlatformFilterRow.uiPfr", "UiPlatformFilterRow.uiPfrPanel");
        Locator name = form.locator("[lsfusion-container='FILTER(uiPfrName(i))']");
        assertThat(labels(name)).hasText(new String[] {"Name", "@"}); // a string's default comparison
        form.locator("button[lsfusion-container='PROPERTY(uiPfrNameEquals())']").click();
        assertThat(labels(name)).hasText(new String[] {"Name", "="});
        close(form);
    }

    // the labels a filter row shows - its caption and its comparison
    private static Locator labels(Locator row) {
        return row.locator(".filter-label:visible");
    }

    // a value typed into a filter row: its value cell edited as a grid's cell is
    private void type(Locator row, String text) {
        edit(row.locator(".filter-data-property-value"), text);
    }
    // a button of the platform's toolbar or filter controls, by its title
    private static Locator button(Locator form, String title) {
        return form.getByTitle(title, new Locator.GetByTitleOptions().setExact(true));
    }
    private void reload() {
        page.reload();
        page.locator("[lsfusion-container='System.root']").waitFor();
    }
}
