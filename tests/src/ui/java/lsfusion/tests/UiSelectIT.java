package lsfusion.tests;

import com.microsoft.playwright.Locator;
import org.junit.Test;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

// a property's selection under a React view (UiSelect.lsf, its views in web/init/uiSelect.jsx): who draws it
public class UiSelectIT extends UiTest {

    // a SELECT with NOCUSTOM is drawn by the platform inside a React view, which places it - in the panel, per row -, and
    // picks as the platform's element picks; a render function given in CUSTOM draws a SELECT, given the options as that
    // element is
    @Test
    public void aSelectionThePlatformDrawsInAReactView() throws Exception {
        String[] orders = seed();
        Locator form = open("UiSelect.uiSelect", "UiSelect.uiSelectPlaced");
        assertThat(form.locator(".ui-select-panel select option:checked")).hasText("New"); // the dropdown
        Locator rows = form.locator(".ui-select-row");
        assertThat(rows).hasCount(2);
        for (int i = 0; i < 2; i++)
            for (String status : new String[] {"New", "Done", "Lost"})
                assertThat(text(rows.nth(i), status)).isVisible();
        click(rows.nth(1), "Done");
        assertThat(rows.nth(1).getByLabel("Done", new Locator.GetByLabelOptions().setExact(true))).isChecked();

        assertThat(text(form, "fn New")).hasAttribute("aria-pressed", "true");
        assertThat(text(form, "fn Lost")).hasAttribute("aria-pressed", "false");
        click(form, "fn Lost");
        assertThat(text(form, "fn Lost")).hasAttribute("aria-pressed", "true");

        save(form);
        check("UiSelect.uiSelectCheckStatus", orders[1], "Done");
        check("UiSelect.uiSelectCheckStatus", orders[0], "Lost");
        close(form);
    }

    // two orders under names no other attempt has - what a failed attempt saved stays in the database for its rerun -:
    // the first New, the second with no status
    private static String[] seed() throws Exception {
        String[] orders = {"1-" + System.nanoTime(), "2-" + System.nanoTime()};
        exec("UiSelect.uiSelectSeed", orders);
        return orders;
    }

    // a button, a check box or a label of the view, by its text
    private static Locator text(Locator scope, String text) {
        return scope.getByText(text, new Locator.GetByTextOptions().setExact(true));
    }

    private static void click(Locator scope, String text) {
        text(scope, text).click();
    }

    private static void save(Locator form) {
        form.locator("button[lsfusion-container='PROPERTY(formApply())']").click();
    }
}
