package lsfusion.tests;

import com.microsoft.playwright.Locator;
import org.junit.Test;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

// a FILTER of a property the platform draws as a condition row: a fixed row of the platform's filter panel
public class UiPlatformFilterRowIT extends UiTest {

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
}
