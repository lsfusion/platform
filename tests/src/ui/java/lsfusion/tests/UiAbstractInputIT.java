package lsfusion.tests;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.options.WaitForSelectorState;
import org.junit.Test;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

// the picker of an ABSTRACT object property with two implementations - the name of the object it links a document to,
// edited in a grid -, drawn under a READONLYIF that does not depend on the grid's objects, which leaves its editing to
// the change of the branches merged: a dialog of the objects to pick from, as for a property of one implementation,
// not an editor of the object's id
public class UiAbstractInputIT extends UiTest {

    // a double click on the cell of an abstract whose implementations are of one class opens the dialog of its
    // objects, and the object picked there is the document's (#1781)
    @Test
    public void aPickOfAnAbstractOfTwoImplementations() throws Exception {
        String[] seed = seed();
        Locator form = open("UiAbstractInput.uiAbstractInput", "UiAbstractInput.uiAbstractInputDocuments");
        cellOf(form, seed[0], "uiAbstractInputNameWay(d)").dblclick();
        pick(form("UiAbstractInput.uiAbstractInputWays"), "uiAbstractInputWayName(w)", seed[3]);
        assertThat(cellOf(form, seed[0], "uiAbstractInputNameWay(d)")).hasText(seed[3]);
        form.locator("button[lsfusion-container='PROPERTY(formApply())']").click();
        check("UiAbstractInput.uiAbstractInputCheckWay", seed[0], seed[3]);
        close(form);
    }

    // where the implementations are of classes of their own, the dialog a document's row opens is of its own
    // implementation's class - an order's the posts', an invoice's the couriers' -, and the object picked there is the
    // document's (#1786)
    @Test
    public void aPickPerRowOfAnAbstractOfTwoClasses() throws Exception {
        String[] seed = seed();
        Locator form = open("UiAbstractInput.uiAbstractInput", "UiAbstractInput.uiAbstractInputDocuments");
        Locator posts = form("UiAbstractInput.uiAbstractInputPosts"), couriers = form("UiAbstractInput.uiAbstractInputCouriers");

        cellOf(form, seed[0], "uiAbstractInputNameCarrier(d)").dblclick();
        assertThat(cell(posts, "uiAbstractInputCarrierName(c)", seed[4])).isVisible();
        close(posts);

        cellOf(form, seed[1], "uiAbstractInputNameCarrier(d)").dblclick();
        assertThat(cell(couriers, "uiAbstractInputCarrierName(c)", seed[6])).isVisible();
        assertThat(posts).hasCount(0);
        pick(couriers, "uiAbstractInputCarrierName(c)", seed[6]);
        assertThat(cellOf(form, seed[1], "uiAbstractInputNameCarrier(d)")).hasText(seed[6]);
        form.locator("button[lsfusion-container='PROPERTY(formApply())']").click();
        check("UiAbstractInput.uiAbstractInputCheckCarrier", seed[1], seed[6]);
        close(form);
    }

    // a change not started by the mouse - a group change, F12 - opens the same dialog, and the object picked there is
    // the document's (#1782)
    @Test
    public void aPickOnAGroupChange() throws Exception {
        String[] seed = seed();
        Locator form = open("UiAbstractInput.uiAbstractInput", "UiAbstractInput.uiAbstractInputDocuments");
        Locator way = cellOf(form, seed[0], "uiAbstractInputNameWay(d)");
        select(way);
        page.keyboard().press("F12");
        pick(form("UiAbstractInput.uiAbstractInputWays"), "uiAbstractInputWayName(w)", seed[3]);
        assertThat(way).hasText(seed[3]);
        form.locator("button[lsfusion-container='PROPERTY(formApply())']").click();
        check("UiAbstractInput.uiAbstractInputCheckWay", seed[0], seed[3]);
        close(form);
    }

    // an order and an invoice, two ways, a post and two couriers under names no other attempt has - what a failed
    // attempt saved stays in the database for its rerun -, the order and the invoice shown from now on. A dialog lists
    // its objects by name and a test picks the second way or courier of its own, never the first row, the current one
    private static String[] seed() throws Exception {
        long id = System.nanoTime();
        String[] seed = {"Order " + id, "Invoice " + id, "Mail " + id, "Pickup " + id, "Post " + id,
                         "Courier A " + id, "Courier B " + id};
        exec("UiAbstractInput.uiAbstractInputSeed", seed);
        return seed;
    }

    // the cell of a document's row
    private Locator cellOf(Locator form, String number, String property) {
        return row(form, "uiAbstractInputNumber(d)", number).locator("td[lsfusion-container='PROPERTY(" + property + ")']");
    }

    // an object picked in the dialog: its row selected, then OK
    private void pick(Locator dialog, String property, String name) {
        select(cell(dialog, property, name));
        dialog.locator("button[lsfusion-container='PROPERTY(formOk())']").click();
        dialog.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.DETACHED));
    }

    // a cell of a row other than the current one clicked, the row selected: the change of the current row is a request
    // with no glass, sent a moment after the click, and the server takes a form's requests in their order, so what
    // comes next goes once it is sent
    private void select(Locator cell) {
        page.waitForRequest(request -> request.postData() != null && request.postData().contains("ChangeGroupObject"), cell::click);
    }
}
