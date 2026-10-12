package lsfusion.tests;

import com.microsoft.playwright.Locator;
import org.junit.Test;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

// a value typed into a grid's cell reaches the database when the form is saved
public class UiGridIT extends UiTest {

    @Test
    public void editAndSave() throws Exception {
        String name = "Item " + System.nanoTime(); // unique : the item of a failed attempt stays in the database for its rerun
        exec("UiGrid.uiGridSeed[STRING[50]]", name);

        Locator form = open("UiGrid.uiGrid", "UiGrid.uiGridItems");
        Locator quantity = row(form, "uiGridName(i)", name).locator("td[lsfusion-container='PROPERTY(uiGridQuantity(i))']");
        edit(quantity, "123"); // under 1000 : above it the grid shows a thousands separator
        assertThat(quantity).hasText("123");
        form.locator("button[lsfusion-container='PROPERTY(formApply())']").click();
        check("UiGrid.uiGridCheckQuantity[STRING[50],INTEGER]", name, "123");
        close(form);
    }
}
