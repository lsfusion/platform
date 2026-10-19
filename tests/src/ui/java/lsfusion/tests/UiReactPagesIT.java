package lsfusion.tests;

import com.microsoft.playwright.Locator;
import org.junit.Test;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

// a form whose tabbed pages are React views, each drawing a list group: every list group gets a count of its rows for
// its pivot, COUNT(<group>), which a group a React view draws never shows and its client keeps nothing of. A CANCEL -
// from a view, from a button the platform draws beside the pages, one that SHOWs a form - and the other page shown
// after it answer with no error, and what the answer carries is applied (#1892): the web client failed such an answer
// whole on the count of the group on the page not shown. The server's side is held by the server test
// forms/ReactCount. The forms open with an item not saved, which a CANCEL drops; a page not shown is not read, so its
// rows are not the current ones until the answer to showing it brings them
public class UiReactPagesIT extends UiTest {

    private static final String UNSAVED = "unsaved";

    // the issue's steps: the second page shown, the CANCEL its view runs through the action's member answers with no
    // error - the item not saved dropped from its rows -, and so does the first page shown after it, its rows brought
    @Test
    public void aCancelFromTheViewOfThePageShown() throws Exception {
        String[] items = seed();
        String all = String.join(", ", items[0], items[1], UNSAVED), saved = String.join(", ", items);
        Locator form = open("UiReactPages.uiReactPages", "UiReactPages.uiReactPagesTabs");
        Locator first = form.locator(".ui-react-pages-first"), second = form.locator(".ui-react-pages-second");
        shown(first, all);
        text(form, "Page 2").click();
        shown(second, all);

        text(form, "React cancel").click();
        shown(second, saved);
        assertThat(first).isHidden();
        assertThat(first).not().hasText(saved); // the page not shown is not read
        assertNoErrorDialog();

        text(form, "Page 1").click();
        shown(first, saved);
        assertThat(second).isHidden();
        assertNoErrorDialog();
        close(form);
    }

    // a CANCEL from a button the platform draws beside the pages, with no page switched, answers with no error - the
    // item not saved dropped from the rows -, and so does the second page shown after it, its rows brought
    @Test
    public void aCancelBesideThePagesAndTheOtherPageShownAfterIt() throws Exception {
        String[] items = seed();
        String all = String.join(", ", items[0], items[1], UNSAVED), saved = String.join(", ", items);
        Locator form = open("UiReactPages.uiReactPages", "UiReactPages.uiReactPagesButtons");
        Locator first = form.locator(".ui-react-pages-first"), second = form.locator(".ui-react-pages-second");
        shown(first, all);

        form.locator("button[lsfusion-container='PROPERTY(cancel)']").click();
        shown(first, saved);
        assertThat(second).isHidden();
        assertThat(second).not().hasText(saved); // the page not shown is not read
        assertNoErrorDialog();

        text(form, "Page 2").click();
        shown(second, saved);
        assertThat(first).isHidden();
        assertNoErrorDialog();
        close(form);
    }

    // a CANCEL that SHOWs a form answers with no error - the item not saved dropped from the rows -, and the form it
    // SHOWs opens with its rows: the client action the answer carries after the form's changes runs
    @Test
    public void aCancelThatShowsAForm() throws Exception {
        String[] items = seed();
        Locator form = open("UiReactPages.uiReactPages", "UiReactPages.uiReactPagesButtons");
        Locator first = form.locator(".ui-react-pages-first");
        shown(first, String.join(", ", items[0], items[1], UNSAVED));

        form.locator("button[lsfusion-container='PROPERTY(cancelShow)']").click();
        Locator other = form("UiReactPages.uiReactPagesOther");
        assertThat(cell(other, "otherName", items[0])).isVisible();
        assertThat(cell(other, "otherName", items[1])).isVisible();
        assertNoErrorDialog();
        close(other); // the pages' form in front again
        shown(first, String.join(", ", items));
        close(form);
    }

    // two items under names no other attempt has - what a failed attempt saved stays in the database for its rerun -,
    // shown from now on
    private static String[] seed() throws Exception {
        long id = System.nanoTime();
        String[] items = {"Apple " + id, "Banana " + id};
        exec("UiReactPages.uiReactPagesSeed", items);
        return items;
    }

    // a page's view on the page - a hidden page keeps its DOM -, its rows these
    private static void shown(Locator view, String rows) {
        assertThat(view).isVisible();
        assertThat(view).hasText(rows);
    }

    private static Locator text(Locator scope, String text) {
        return scope.getByText(text, new Locator.GetByTextOptions().setExact(true));
    }

    // the dialog the web client shows a failed answer in stays until it is closed: none is there
    private void assertNoErrorDialog() {
        assertThat(page.locator(".modal-content")).hasCount(0);
    }
}
