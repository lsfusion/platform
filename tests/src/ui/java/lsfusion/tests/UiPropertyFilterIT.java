package lsfusion.tests;

import com.microsoft.playwright.Locator;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

// a property's filter, FILTER(<property>), under React views (UiPropertyFilter.lsf, its views in UiPropertyFilter.js):
// a part of its property's group a view draws, whoever draws the group's conditions - the platform's filter panel, a
// view of the whole group, a view of the conditions, a panel with no box -, a filter's own React view, and the
// platform's row a view places; what its condition does to the rows (#1904)
public class UiPropertyFilterIT extends UiTest {

    private static final String FOLDER = "UiPropertyFilter.uiPf";

    // over the platform's filter panel: the filter a view draws is hidden by its SHOWIF; shown, it filters the
    // platform's grid - the panel showing no row of its own for it, the filter being that row - and no value takes it
    // off; the panel's row of the city, which the view places, is typed into (#1904)
    @Test
    public void aFilterOverThePlatformsPanel() throws Exception {
        List<String> items = seed();
        Locator form = open(FOLDER, "UiPropertyFilter.uiPfOverPlatform");
        assertThat(form.locator(".ui-pf-hidden")).isVisible();
        form.locator("[lsfusion-container='PROPERTY(uiPfSearchShown())'] input").click();
        Locator name = form.locator("input.ui-pf-name");
        rows(form, items);
        name.fill("an");
        rows(form, items.get(1));
        assertThat(name).hasValue("an");
        assertThat(conditionRow(form.locator("[lsfusion-container='FILTERS(i)']"), "Name")).hasCount(0);
        name.fill("");
        rows(form, items);

        Locator city = form.locator("[data-lsf-sid='FILTER(uiPfCity(i))']");
        assertThat(conditionRow(city, "City")).hasCount(1);
        edit(city.locator(".filter-data-property-value"), "Riga");
        rows(form, items.get(1));
        close(form);
    }

    // a view drawing the whole group draws its properties' filters beside the group's conditions and rows: each states
    // its condition in its filter's place - the name's before the city's, as the form declares them, whichever is
    // given first; the city's taken off alone -, '' takes one off, and the filter's options state its negation and
    // its comparison (#1904)
    @Test
    public void theFiltersOfAViewOfTheWholeGroup() throws Exception {
        List<String> items = seed();
        Locator form = open(FOLDER, "UiPropertyFilter.uiPfBoard");
        Locator rows = form.locator(".ui-pf-rows"), conditions = form.locator(".ui-pf-conditions");
        assertThat(rows).hasText(String.join(", ", items));
        assertThat(conditions).isEmpty();
        form.locator("input.ui-pf-city").fill("Minsk");
        assertThat(rows).hasText(items.get(0) + ", " + items.get(2));
        assertThat(conditions).hasText(Pattern.compile("^uiPfCity \\S+ Minsk$"));
        form.locator("input.ui-pf-name").fill("a"); // Apple, Banana and Date, Minsk's Apple alone
        assertThat(rows).hasText(items.get(0));
        assertThat(conditions).hasText(Pattern.compile("^uiPfName \\S+ a; uiPfCity \\S+ Minsk$"));
        form.locator("input.ui-pf-city").fill("");
        assertThat(rows).hasText(items.get(0) + ", " + items.get(1) + ", " + items.get(3));
        assertThat(conditions).hasText(Pattern.compile("^uiPfName \\S+ a$"));
        form.locator("input.ui-pf-name").fill("");
        assertThat(rows).hasText(String.join(", ", items));

        form.locator("input.ui-pf-value").fill("an");
        form.locator(".ui-pf-not").click();
        assertThat(rows).hasText(items.get(0) + ", " + items.get(2) + ", " + items.get(3));
        assertThat(conditions).hasText(Pattern.compile("^not uiPfName \\S+ an$"));
        form.locator("input.ui-pf-value").fill("Date"); // a part of Date's name, which no name equals
        form.locator(".ui-pf-equals").click();
        assertThat(rows).isEmpty();
        assertThat(conditions).hasText("uiPfName = Date");
        form.locator("input.ui-pf-value").fill(items.get(3));
        form.locator(".ui-pf-equals").click();
        assertThat(rows).hasText(items.get(3));
        assertThat(conditions).hasText("uiPfName = " + items.get(3));
        form.locator("input.ui-pf-name").fill("");
        assertThat(rows).hasText(String.join(", ", items));
        close(form);
    }

    // a filter with a React view of its own in a form the platform draws otherwise: it filters the platform's grid,
    // whose filter panel shows its own row of the city and none of the name, the filter being that row (#1904)
    @Test
    public void aFiltersOwnView() throws Exception {
        List<String> items = seed();
        Locator form = open(FOLDER, "UiPropertyFilter.uiPfOwn");
        Locator name = form.locator("input.ui-pf-name"), panel = form.locator("[lsfusion-container='FILTERS(i)']");
        rows(form, items);
        name.fill("err");
        rows(form, items.get(2));
        assertThat(conditionRow(panel, "City")).hasCount(1);
        assertThat(conditionRow(panel, "Name")).hasCount(0);
        name.fill("");
        rows(form, items);
        close(form);
    }

    // a view drawing the conditions draws the filter declared in their box: it filters the platform's grid, and the
    // view's conditions list its condition; the container's SHOWIF hides the conditions and the filter's entry says it
    // is hidden too, the condition staying; shown again, it is not hidden (#1904)
    @Test
    public void aFilterInTheConditionsAViewDraws() throws Exception {
        List<String> items = seed();
        Locator form = open(FOLDER, "UiPropertyFilter.uiPfInConditions");
        Locator view = form.locator("[data-name-hidden]");
        Locator toggle = form.locator("button[lsfusion-container='PROPERTY(uiPfToggleConditions())']");
        assertThat(view).hasAttribute("data-name-hidden", "false");
        rows(form, items);
        form.locator("input.ui-pf-name").fill("an");
        rows(form, items.get(1));
        assertThat(form.locator(".ui-pf-conditions")).hasText(Pattern.compile("^uiPfName \\S+ an$"));
        toggle.click();
        assertThat(view).hasAttribute("data-conditions-hidden", "true");
        assertThat(view).hasAttribute("data-name-hidden", "true");
        rows(form, items.get(1));
        toggle.click();
        assertThat(view).hasAttribute("data-conditions-hidden", "false");
        assertThat(view).hasAttribute("data-name-hidden", "false");
        assertThat(form.locator("input.ui-pf-name")).hasValue("an");
        assertThat(form.locator(".ui-pf-conditions")).hasText(Pattern.compile("^uiPfName \\S+ an$"));
        rows(form, items.get(1));
        form.locator("input.ui-pf-name").fill("");
        rows(form, items);
        close(form);
    }

    // filters a view draws over the platform's panel with no box: a LOGICAL one - a boolean filter - whose true filters,
    // and whose false - a two-state LOGICAL's NULL - takes it off, as none does; a condition an action
    // states on the server (FILTER PROPERTY) filters the grid and shows in the filter the view draws (#1904)
    @Test
    public void filtersOverAPanelWithNoBox() throws Exception {
        List<String> items = seed();
        Locator form = open(FOLDER, "UiPropertyFilter.uiPfNoBox");
        Locator cheap = form.locator(".ui-pf-cheap"), name = form.locator(".ui-pf-name");
        assertThat(form.locator(".ui-pf-cheap-type")).hasText("boolean");
        assertThat(cheap).hasText("null");
        rows(form, items);
        form.locator(".ui-pf-cheap-true").click();
        rows(form, items.get(0), items.get(1));
        assertThat(cheap).hasText("true");
        form.locator(".ui-pf-cheap-false").click();
        rows(form, items);
        assertThat(cheap).hasText("null");
        form.locator(".ui-pf-cheap-none").click();
        rows(form, items);
        assertThat(cheap).hasText("null");

        form.locator("button[lsfusion-container='PROPERTY(uiPfStateAn())']").click();
        rows(form, items.get(1));
        assertThat(name).hasText("\"an\"");
        form.locator(".ui-pf-name-none").click();
        rows(form, items);
        assertThat(name).hasText("null");
        close(form);
    }

    // Apple, Banana, Cherry and Date under names no other attempt has - what a failed attempt saved stays in the
    // database for its rerun -: Apple and Cherry in Minsk, Banana in Riga, Date in Vilnius; Apple and Banana the cheap
    // ones
    private static List<String> seed() throws Exception {
        long id = System.nanoTime();
        List<String> items = Arrays.asList("Apple " + id, "Banana " + id, "Cherry " + id, "Date " + id);
        exec("UiPropertyFilter.uiPfSeed", items.toArray(new String[0]));
        return items;
    }

    // the platform's grid shows these items, in this order, and no other: one assertion of the whole list, so that it
    // never holds of two states of the grid at once
    private static void rows(Locator form, List<String> shown) {
        assertThat(form.locator("td[lsfusion-container='PROPERTY(uiPfName(i))']")).hasText(shown.toArray(new String[0]));
    }

    private static void rows(Locator form, String... shown) {
        rows(form, Arrays.asList(shown));
    }

    // a condition row of the platform's under a container, by its caption
    private static Locator conditionRow(Locator container, String caption) {
        return container.locator(".filter").filter(new Locator.FilterOptions().setHas(
                container.page().locator(".filter-label").filter(new Locator.FilterOptions().setHasText(Pattern.compile("^" + caption + "$")))));
    }
}
