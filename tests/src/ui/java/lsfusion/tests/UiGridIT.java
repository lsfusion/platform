package lsfusion.tests;

import com.microsoft.playwright.Locator;
import org.junit.Test;

import java.util.regex.Pattern;

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

    // the column's pattern formats a footer of the column's kind of type only, as the print view does: a whole number under
    // the dates or under the numbers with leading zeros is shown as a number, a date under the dates and a number under the
    // numbers by their columns' patterns. The grid formatted every footer by its column's pattern - the number under the
    // dates as "yyyy42.dd", a number pattern read out of the date pattern, and under the numbers as "000042"
    @Test
    public void aFooterOfAnotherTypeIsNotFormattedByItsColumnsPattern() {
        Locator form = open("UiGrid.uiGrid", "UiGrid.uiGridPatterns");
        assertThat(form.locator("tfoot th:not(.arrow-th)")).hasText(new String[]{"42", "2031.31.12", "42", "000007"});
        close(form);
    }

    // a render function of a property named image is given the HTML of the image a cell names, and its string footer as
    // the text: the footer was converted as a cell is, as the image it names, and threw on the string - "createImageHTML
    // is not a function" in an Error box, the footer left empty
    @Test
    public void aRenderFunctionIsGivenAStringFooterAsAText() throws Exception {
        exec("UiGrid.uiGridSeed", "Item " + System.nanoTime());
        Locator form = open("UiGrid.uiGrid", "UiGrid.uiGridIcons");
        assertThat(form.locator("tfoot")).containsText("a string footer of icons");
        assertThat(form.locator("tbody i.bi-plus").first()).isVisible();
        close(form);
    }

    // a range of rows runs on from the row it starts from and shrinks back, a range of columns runs on beside it, a move
    // without Shift starts both anew, and every row and column is selected (GGridSelection)
    @Test
    public void rowsAndColumnsAreSelected() throws Exception {
        exec("UiGrid.uiGridSeedLines", "20");
        Locator form = open("UiGrid.uiGrid", "UiGrid.uiGridSelection");
        Locator count = form.locator("[lsfusion-container='PROPERTY(selectedCount)']");
        Locator nameSelected = form.locator("[lsfusion-container='PROPERTY(nameSelected)'] input");
        Locator qtySelected = form.locator("[lsfusion-container='PROPERTY(qtySelected)'] input");
        Locator selectedCells = form.locator("td.column-select");
        click(form.locator("td[lsfusion-container='PROPERTY(qty)']").nth(1));
        assertThat(qtySelected).isChecked();
        for (int i = 0; i < 3; i++)
            press("Shift+ArrowDown");
        assertThat(count).hasText(Pattern.compile("(^|\\D)4$"));
        assertThat(selectedCells).hasCount(4);
        press("Shift+ArrowUp");
        assertThat(count).hasText(Pattern.compile("(^|\\D)3$"));
        assertThat(nameSelected).not().isChecked();
        press("Shift+ArrowLeft");
        assertThat(nameSelected).isChecked();
        assertThat(qtySelected).isChecked();
        assertThat(selectedCells).hasCount(6);
        press("ArrowRight"); // a range anew: the quantity alone, and its row
        assertThat(nameSelected).not().isChecked();
        assertThat(count).hasText(Pattern.compile("(^|\\D)1$"));
        assertThat(selectedCells).hasCount(0);
        answered(0, () -> press("Control+a"));
        assertThat(count).hasText(Pattern.compile("(^|\\D)20$"));
        assertThat(nameSelected).isChecked();
        assertThat(selectedCells).hasCount(60);
        close(form);
    }
}
