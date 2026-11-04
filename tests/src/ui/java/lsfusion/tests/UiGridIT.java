package lsfusion.tests;

import com.microsoft.playwright.Locator;
import org.junit.Test;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.Assert.assertEquals;

// the platform's grid (UiGrid.lsf)
public class UiGridIT extends UiTest {

    // a value typed into a cell reaches the database when the form is saved
    @Test
    public void editAndSave() throws Exception {
        String name = "Item " + System.nanoTime(); // unique : the item of a failed attempt stays in the database for its rerun
        exec("UiGrid.uiGridSeed", name);

        Locator form = open("UiGrid.uiGrid", "UiGrid.uiGridItems");
        Locator quantity = row(form, "uiGridName(i)", name).locator("td[lsfusion-container='PROPERTY(uiGridQuantity(i))']");
        edit(quantity, "123"); // under 1000 : above it the grid shows a thousands separator
        assertThat(quantity).hasText("123");
        form.locator("button[lsfusion-container='PROPERTY(formApply())']").click();
        check("UiGrid.uiGridCheckQuantity", name, "123");
        close(form);
    }

    // the footer of a column of images is an image too, which the grid draws as it draws the cells: the server sent it as
    // TRUE, which the grid's image renderer threw on - the footer stayed empty, and the form no longer closed
    @Test
    public void anImageFooterIsDrawn() {
        Locator form = open("UiGrid.uiGrid", "UiGrid.uiGridPictures");
        assertEquals("a footer", form.locator("tfoot img").evaluate("img => fetch(img.src)"
                + ".then(response => response.ok ? response.text() : img.src + ' -> HTTP ' + response.status)"));
        close(form);
    }

    // a footer of another type than its column's is drawn as a value of its own type, as its element too: under a column
    // of images a text file as a file's icon, a string as a text, a link to an image as the image, a logical as a checked
    // box, and under a column of logicals a string as a text, not in the column's input. The grid drew such a footer with
    // the column's renderer, which threw on the image column's (the footers stayed empty) and drew the string as a box
    @Test
    public void aFooterOfAnotherTypeIsDrawnAsSuch() {
        Locator form = open("UiGrid.uiGrid", "UiGrid.uiGridFooters");
        assertThat(form.locator("tfoot i.bi-file-earmark")).hasCount(1);
        assertThat(form.locator("tfoot")).containsText("a string footer");
        assertThat(form.locator("tfoot img[src='a-link-footer.png']")).hasCount(1);
        assertThat(form.locator("tfoot input[type=checkbox]")).isChecked();
        assertThat(form.locator("tfoot input")).hasCount(1);
        assertThat(form.locator("tfoot")).containsText("a footer of checks");
        close(form);
    }
}
