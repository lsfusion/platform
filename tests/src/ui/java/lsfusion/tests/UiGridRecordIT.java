package lsfusion.tests;

import com.microsoft.playwright.Locator;
import org.junit.Test;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

// what a DESIGN puts into GRID(g), the grid's record, as the browser shows it (UiGridRecord.lsf, its views in
// UiGridRecord.js): the popup a classic custom view shows next to the card it clicks, where the platform draws the
// grid, and what a React view around the grid draws and places of it. The record is a container with no container of
// its own, and the walks of the design - the server's, and the clients' lookups of a component by its id - now step
// from a grid into it; the server's side is held by the server test forms/GridRecord. What the record's events did,
// they write in the module's log
public class UiGridRecordIT extends UiTest {

    // the record a classic custom view pops up holds what any container holds: its card shows its computed caption, and
    // is hidden by its SHOWIF where the current item has no note; its tabbed container opens with no error, and its
    // second page, selected, shows the code it holds and runs its ON TAB; its pane, collapsed, runs its ON COLLAPSE. A
    // tabbed container in the record failed the form with an error as it opened, and every switch of its tab again
    @Test
    public void aClassicViewsPopupShowsWhatTheRecordHolds() throws Exception {
        String[] items = seed();
        Locator form = open("UiGridRecord.uiGridRecord", "UiGridRecord.uiGridRecordCards");
        Locator popup = page.locator(".tippy-box"), card = popup.locator("[lsfusion-container='recordCard']"),
                caption = popup.locator(".caption-panel:has(> [lsfusion-container='recordCard']) .caption-panel-header");

        card(form, items[0]).click();
        assertThat(popup.locator("[lsfusion-container='PROPERTY(recordNote)']")).containsText("ripe");
        assertThat(caption).hasText("Card of " + items[0]);
        assertThat(card).isVisible();

        card(form, items[1]).click();
        assertThat(popup.locator("[lsfusion-container='PROPERTY(paneName)']")).containsText(items[1]);
        assertThat(card).hasCount(1);
        assertThat(card).isHidden(); // the second item has no note

        popup.getByText("More", new Locator.GetByTextOptions().setExact(true)).click();
        Locator code = popup.locator("[lsfusion-container='PROPERTY(recordCode)']");
        assertThat(code).isVisible(); // the page shown: an inactive tab keeps its DOM
        assertThat(code).containsText("B2");
        log("more of " + items[1]);

        popup.getByText("Pane", new Locator.GetByTextOptions().setExact(true)).click();
        log("more of " + items[1], "pane of " + items[1] + " collapsed");
        assertNoErrorDialog();
        close(form);
    }

    // a React view around the grid is given what the record holds as what any container holds: the entries of a card of
    // the record it places (custom = NULL) and of a container of it it draws carry their computed captions; where the
    // current item has no note, the placed card's entry says hidden by its SHOWIF, the host the view places it in is
    // hidden, and so is a nested React view of the record under the same SHOWIF. The view's taking the card out runs
    // the card's ON COLLAPSE, and its no longer showing the container it draws (useShown) runs that one's: what the
    // hidden container holds is no longer read - its code, changed meanwhile, reads null once the change is answered,
    // as the bumps counted outside it say -, until it is shown again
    @Test
    public void aReactViewDrawsAndPlacesWhatTheRecordHolds() throws Exception {
        String[] items = seed();
        Locator form = open("UiGridRecord.uiGridRecord", "UiGridRecord.uiGridRecordBoard");
        Locator current = form.locator(".ui-grid-record-current"), hidden = form.locator(".ui-grid-record-placed-hidden"),
                host = form.locator("[data-lsf-sid='recordPlaced']"), nested = form.locator(".ui-grid-record-nested"),
                code = form.locator(".ui-grid-record-code"), bumps = form.locator(".ui-grid-record-bumps");
        assertThat(current).hasText(items[0]);
        assertThat(form.locator(".ui-grid-record-placed-caption")).hasText("Card of " + items[0]);
        assertThat(form.locator(".ui-grid-record-drawn-caption")).hasText("Card of " + items[0]);
        assertThat(hidden).hasText("false");
        assertThat(host).containsText("ripe");
        assertThat(nested).isVisible();

        row(form, items[1]).click();
        assertThat(current).hasText(items[1]);
        assertThat(hidden).hasText("true"); // the second item has no note
        assertThat(host).hasCount(1);
        assertThat(host).isHidden();
        assertThat(nested).hasCount(1);
        assertThat(nested).isHidden();

        row(form, items[0]).click();
        assertThat(current).hasText(items[0]);
        assertThat(hidden).hasText("false");
        assertThat(host).isVisible();
        assertThat(nested).isVisible();

        // the view's first placing of the record's children can reach the server as their hiding and their showing
        // again, which runs the card's ON COLLAPSE as the form opens: the log starts over once the form has answered
        // what came after, the changes of the current item
        exec("UiGridRecord.uiGridRecordForget");
        button(form, "Unplace").click();
        assertThat(host).hasCount(0);
        log("placed of " + items[0] + " collapsed");
        button(form, "Place").click();
        assertThat(host).containsText("ripe");

        assertThat(code).hasText("A1");
        button(form, "Hide drawn").click();
        log("placed of " + items[0] + " collapsed", "drawn of " + items[0] + " collapsed");
        button(form, "Bump").click();
        assertThat(bumps).hasText("1"); // the bump answered
        assertThat(code).hasText("null");
        button(form, "Show drawn").click();
        assertThat(code).hasText("A1+");
        assertNoErrorDialog();
        // saved, the form closes asking nothing
        form.locator("button[lsfusion-container='PROPERTY(formApply())']").click();
        close(form);
    }

    // two items under names no other attempt has - what a failed attempt saved stays in the database for its rerun -,
    // shown from now on in this order: the first with the note "ripe" and the code A1, the second with no note and B2
    private static String[] seed() throws Exception {
        long id = System.nanoTime();
        String[] items = {"Apple " + id, "Banana " + id};
        exec("UiGridRecord.uiGridRecordSeed", items);
        return items;
    }

    // what the record's events did since the seed, in turn
    private static void log(String... done) throws Exception {
        check("UiGridRecord.uiGridRecordCheckLog", String.join(", ", done));
    }

    // a card the classic view draws, by its item's name
    private static Locator card(Locator form, String name) {
        return form.locator(".ui-grid-record-card").filter(new Locator.FilterOptions().setHasText(name));
    }

    // a row the React view draws, by its item's name
    private static Locator row(Locator form, String name) {
        return form.locator(".ui-grid-record-row").filter(new Locator.FilterOptions().setHasText(name));
    }

    private static Locator button(Locator form, String text) {
        return form.getByText(text, new Locator.GetByTextOptions().setExact(true));
    }

    // the dialog the web client shows a failed request in stays until it is closed: none is there
    private void assertNoErrorDialog() {
        assertThat(page.locator(".modal-content")).hasCount(0);
    }
}
