package lsfusion.tests;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Route;
import com.microsoft.playwright.options.KeyboardModifier;
import com.microsoft.playwright.options.SelectOption;
import com.microsoft.playwright.options.WaitForSelectorState;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.Assert.assertEquals;

// sortings and the state the server applies, in the browser (UiReactOrders.lsf, its React view in UiReactOrders.js):
// the sortings of the rows a React view draws and of the platform's grid, by its header, and ORDER, FILTER, FILTERGROUP
// and ACTIVATE TAB applied on the server - from an action, from a handler and as a form opens -, which the action reads
// back at once and the client shows and sends nothing back for, the user events counting only what the user changes
public class UiReactOrdersIT extends UiTest {

    // the sortings of the rows a React view draws (#1859): the form's own, which name a property the view may not
    // sort by, as each property says; one sorting with no direction takes such a one off, a new one is appended, a
    // sorted one keeps its place when its direction changes, an array states the whole list - the rows following
    // each -, and the list keeps its identity while the sortings stay as they are. Of the group whose rows the platform
    // draws the view has a panel property alone: no sortings, no rows, no member to sort it with
    @Test
    public void aViewSortsTheRowsItDraws() throws Exception {
        String s = seed();
        Locator form = open("UiReactOrders.uiRor", "UiReactOrders.uiRorBoard");
        Locator orders = form.locator(".ui-ror-orders"), rows = form.locator(".ui-ror-row");
        assertThat(orders).hasText("price asc, name desc");
        assertThat(rows).hasText(new String[] {"Banana " + s + "|9", "Apple " + s + "|5", "Cherry " + s + "|1"});
        assertThat(form.locator(".ui-ror-nosort")).hasText("name false, price true, qty false");
        assertThat(form.locator(".ui-ror-properties")).hasText("name, price, qty");
        assertThat(form.locator(".ui-ror-other")).hasText("no orders, no rows, properties pPrice, members pPrice");

        board("controller.o.orders.change({property: 'price'})");
        assertThat(rows).hasText(new String[] {"Cherry " + s + "|1", "Banana " + s + "|9", "Apple " + s + "|5"});
        assertThat(orders).hasText("name desc");
        board("controller.o.orders.change({property: 'qty', desc: false})");
        assertThat(orders).hasText("name desc, qty asc");
        board("controller.o.orders.change({property: 'name', desc: false})");
        assertThat(rows).hasText(new String[] {"Apple " + s + "|5", "Banana " + s + "|9", "Cherry " + s + "|1"});
        assertThat(orders).hasText("name asc, qty asc");
        board("controller.o.orders.change([{property: 'qty', desc: false}])");
        assertThat(rows).hasText(new String[] {"Cherry " + s + "|1", "Apple " + s + "|5", "Banana " + s + "|9"});
        assertThat(orders).hasText("qty asc");

        page.evaluate("() => { window.uiRorHeld = window.uiRorBoardState.data.o.orders; }");
        // the value is predicted locally, so compare the list's identity only after the change has been answered
        page.waitForResponse(response -> response.request().postData() != null
                        && response.request().postData().contains(".ExecuteEventAction/"),
                () -> board("controller.o.qty.change(4, data.o.list[0])"));
        page.waitForFunction("() => !window.uiRorBoardState.data.o.list[0].qty.loading");
        assertThat(rows).hasText(new String[] {"Cherry " + s + "|4", "Apple " + s + "|5", "Banana " + s + "|9"});
        assertEquals("the same sortings after a value changed", true, page.evaluate("() => window.uiRorBoardState.data.o.orders === window.uiRorHeld"));
        board("controller.o.orders.change([])");
        assertThat(rows).hasText(new String[] {"Apple " + s + "|5", "Banana " + s + "|9", "Cherry " + s + "|4"});
        assertThat(orders).hasText("");
        assertEquals("the same sortings after they changed", false, page.evaluate("() => window.uiRorBoardState.data.o.orders === window.uiRorHeld"));
        form.locator("button[lsfusion-container='PROPERTY(formApply())']").click();
        close(form);
    }

    // a sorting a view cannot state is refused, saying why, with nothing changed and nothing sent: a direction on a
    // property the view may not sort by, a property of another group, a field an order does not have, a property listed
    // twice, an order that is no object. A sorting stated after them is merged into the form's own sortings, which
    // they left as they were, and the rows follow when the price is taken off then
    @Test
    public void aSortingAViewCannotStateIsRefused() throws Exception {
        String s = seed();
        Requests sent = requests();
        Locator form = open("UiReactOrders.uiRor", "UiReactOrders.uiRorBoard");
        Locator orders = form.locator(".ui-ror-orders"), rows = form.locator(".ui-ror-row");
        assertThat(orders).hasText("price asc, name desc");
        String change = "controller.o.orders.change(): ";
        assertEquals(change + "'price' cannot be sorted by",
                refusal("controller.o.orders.change({property: 'price', desc: true})"));
        assertEquals(change + "unknown property 'pName' of object group 'o'",
                refusal("controller.o.orders.change({property: 'pName', desc: true})"));
        assertEquals(change + "unknown field 'dir'; an order has property, desc",
                refusal("controller.o.orders.change({property: 'name', dir: true})"));
        assertEquals(change + "order 1: 'name' is already in this list",
                refusal("controller.o.orders.change([{property: 'name'}, {property: 'name', desc: true}])"));
        assertEquals(change + "an order must be an object {property, desc}", refusal("controller.o.orders.change('name')"));

        board("controller.o.orders.change({property: 'qty', desc: true})");
        assertThat(orders).hasText("price asc, name desc, qty desc");
        board("controller.o.orders.change({property: 'price'})");
        assertThat(orders).hasText("name desc, qty desc");
        assertThat(rows).hasText(new String[] {"Cherry " + s + "|1", "Banana " + s + "|9", "Apple " + s + "|5"});
        assertEquals("the sortings sent", 2, sent.count("SetPropertyOrders"));
        close(form);
    }

    // a user event is for what the user changes (#1871): the sortings a view states again, the list the group has,
    // are sent and fire no ORDERS; a list that changes them fires it. The events are counted once the rows of that last
    // list are shown: they come with its answer, and the answers in the order of the requests, so the count is the one
    // the requests before it left
    @Test
    public void aSortingRequestThatChangesNothingFiresNoUserEvent() throws Exception {
        String s = seed();
        Requests sent = requests();
        Locator form = open("UiReactOrders.uiRor", "UiReactOrders.uiRorBoard");
        Locator orders = form.locator(".ui-ror-orders"), rows = form.locator(".ui-ror-row"),
                events = form.locator(".ui-ror-events");
        assertThat(orders).hasText("price asc, name desc");
        board("controller.o.orders.change([{property: 'name', desc: true}])");
        assertThat(rows).hasText(new String[] {"Cherry " + s + "|1", "Banana " + s + "|9", "Apple " + s + "|5"});
        assertThat(events).hasText("1");
        board("controller.o.orders.change(data.o.orders)");
        board("controller.o.orders.change([{property: 'qty', desc: true}])");
        assertThat(orders).hasText("qty desc");
        assertThat(rows).hasText(new String[] {"Banana " + s + "|9", "Apple " + s + "|5", "Cherry " + s + "|1"});
        assertThat(events).hasText("2");
        assertEquals("the sortings sent", 3, sent.count("SetPropertyOrders"));
        close(form);
    }

    // the platform's grid draws the form's own sortings, with nothing sent for them, and a header sorts on a double
    // click - a real one: plain, by its column alone, the same column again turning it round; with ctrl a column is
    // added after the ones sorted by, and a sorted one turned round, the others kept; with shift taken off - one
    // request each, the rows following
    @Test
    public void theGridDrawsTheFormsSortingsAndAHeaderSortsByADoubleClick() throws Exception {
        String s = seed();
        Requests sent = requests();
        Locator form = open("UiReactOrders.uiRor", "UiReactOrders.uiRorClassic");
        Locator names = column(form, "cName");
        assertThat(names).hasText(new String[] {"Banana " + s, "Apple " + s, "Cherry " + s});
        assertArrows(form, "cName desc", "cPrice asc");

        header(form, "cQty").dblclick();
        assertThat(names).hasText(new String[] {"Cherry " + s, "Apple " + s, "Banana " + s});
        assertArrows(form, "cQty asc");
        header(form, "cQty").dblclick();
        assertThat(names).hasText(new String[] {"Banana " + s, "Apple " + s, "Cherry " + s});
        assertArrows(form, "cQty desc");
        // with ctrl the name goes after the quantity - the arrows say so at once -, which orders the rows alone, their
        // quantities being apart: the rows stay by the quantity, as they would not had the name been put first, and
        // they are what the server read by then once an action run after the click is answered - the answers come in
        // the order of the requests
        header(form, "cName").dblclick(new Locator.DblclickOptions().setModifiers(Collections.singletonList(KeyboardModifier.CONTROL)));
        assertArrows(form, "cName asc", "cQty desc");
        button(form, "uiRorPing").click();
        assertThat(value(form, "uiRorPings")).hasText("1");
        assertThat(names).hasText(new String[] {"Banana " + s, "Apple " + s, "Cherry " + s});
        // ... and the quantity turned round with ctrl, the name kept after it
        header(form, "cQty").dblclick(new Locator.DblclickOptions().setModifiers(Collections.singletonList(KeyboardModifier.CONTROL)));
        assertThat(names).hasText(new String[] {"Cherry " + s, "Apple " + s, "Banana " + s});
        assertArrows(form, "cName asc", "cQty asc");
        header(form, "cQty").dblclick(new Locator.DblclickOptions().setModifiers(Collections.singletonList(KeyboardModifier.SHIFT)));
        assertThat(names).hasText(new String[] {"Apple " + s, "Banana " + s, "Cherry " + s});
        assertArrows(form, "cName asc");
        assertEquals("the header's requests", 5, sent.count("SetPropertyOrders"));
        close(form);
    }

    // a header's sorting is shown at once, before the answer - held back here until it is seen -, and the next double
    // click starts from what the first one showed: the price sorted by, then turned round, one request each, the rows
    // following the last one once the answers come
    @Test
    public void aHeadersSortingIsShownBeforeTheAnswer() throws Exception {
        String s = seed();
        Requests sent = requests();
        Locator form = open("UiReactOrders.uiRor", "UiReactOrders.uiRorClassic");
        Locator names = column(form, "cName");
        header(form, "cName").dblclick();
        assertThat(names).hasText(new String[] {"Apple " + s, "Banana " + s, "Cherry " + s});
        assertArrows(form, "cName asc");

        hold();
        header(form, "cPrice").dblclick();
        assertArrows(form, "cPrice asc");
        header(form, "cPrice").dblclick();
        assertArrows(form, "cPrice desc");
        release();
        assertThat(names).hasText(new String[] {"Cherry " + s, "Apple " + s, "Banana " + s});
        assertArrows(form, "cPrice desc");
        assertEquals("the header's requests", 3, sent.count("SetPropertyOrders"));
        close(form);
    }

    // a view that replaces the grid - a pivot - starts sorted as the group is: the name selected, the pivot's rows are
    // the names, in the order the group is sorted by; and the grid that replaces it again knows what the group is
    // sorted by: its header draws the arrow again (#1868)
    @Test
    public void aGridShownAgainAfterAPivotDrawsTheSortings() throws Exception {
        String s = seed();
        Locator form = open("UiReactOrders.uiRor", "UiReactOrders.uiRorClassic");
        Locator names = column(form, "cName");
        header(form, "cName").dblclick();
        assertThat(names).hasText(new String[] {"Apple " + s, "Banana " + s, "Cherry " + s});
        header(form, "cName").dblclick();
        assertThat(names).hasText(new String[] {"Cherry " + s, "Banana " + s, "Apple " + s});
        assertArrows(form, "cName desc");
        names.first().click();

        form.locator("[title='Pivot']").click();
        assertThat(form.locator(".pvtTable.bodytable .pvtRowLabel")).hasText(new String[] {"Cherry " + s, "Banana " + s,
                "Apple " + s});
        form.locator("[title='Table']").click();
        assertThat(names).hasText(new String[] {"Cherry " + s, "Banana " + s, "Apple " + s});
        assertArrows(form, "cName desc");
        close(form);
    }

    // an ORDER from an action is applied on the server at once - ORDERS right after it in the action reads the new
    // sorting - and shown, with nothing sent back and no ORDERS user event; a header's double click then starts from
    // the server's sorting, and is the user's (#1856)
    @Test
    public void anOrderFromAnActionIsShownWithNothingSentBack() throws Exception {
        String s = seed();
        Requests sent = requests();
        Locator form = open("UiReactOrders.uiRor", "UiReactOrders.uiRorState");
        Locator names = column(form, "sName");
        assertThat(names).hasText(new String[] {"Apple " + s, "Banana " + s, "Cherry " + s});
        assertArrows(form, "sName asc");

        button(form, "uiRorOrderByPrice").click();
        assertThat(names).hasText(new String[] {"Cherry " + s, "Apple " + s, "Banana " + s});
        assertArrows(form, "sPrice desc");
        assertJson(value(form, "uiRorOrdersRead"), "[{\"desc\":true,\"property\":\"sPrice\"}]");
        assertThat(value(form, "uiRorOrdersEvents")).hasText("");
        header(form, "sPrice").dblclick();
        assertThat(names).hasText(new String[] {"Banana " + s, "Apple " + s, "Cherry " + s});
        assertArrows(form, "sPrice asc");
        assertThat(value(form, "uiRorOrdersEvents")).hasText("1");
        assertEquals("the sortings sent", 1, sent.count("SetPropertyOrders"));
        close(form);
    }

    // an ON ORDERS handler answering a header's double click with an ORDER of its own: the handler's sorting is shown
    // and the rows are read by it, with nothing sent back for it and the user event fired once, for the click (#1856)
    @Test
    public void anOnOrdersHandlerAnswersTheClickOnce() throws Exception {
        String s = seed();
        Requests sent = requests();
        Locator form = open("UiReactOrders.uiRor", "UiReactOrders.uiRorState");
        Locator names = column(form, "sName");
        button(form, "uiRorEchoOn").click();
        header(form, "sName").dblclick();
        assertThat(names).hasText(new String[] {"Banana " + s, "Apple " + s, "Cherry " + s});
        assertArrows(form, "sQty desc");
        // an action after it: what would have been sent back by then is
        button(form, "uiRorOrderByPrice").click();
        assertThat(names).hasText(new String[] {"Cherry " + s, "Apple " + s, "Banana " + s});
        assertArrows(form, "sPrice desc");
        assertThat(value(form, "uiRorOrdersEvents")).hasText("1");
        assertEquals("the sortings sent", 1, sent.count("SetPropertyOrders"));
        close(form);
    }

    // an ORDER of an ON ORDER handler, which the reading of the group fires, is what the rows are read with (#1870):
    // the handler's sorting is shown, and the rows follow it, not the header's
    @Test
    public void anOrderOfAnOnOrderHandlerIsWhatTheRowsAreReadWith() throws Exception {
        String s = seed();
        Locator form = open("UiReactOrders.uiRor", "UiReactOrders.uiRorState");
        button(form, "uiRorOrderOnOrderOn").click();
        header(form, "sPrice").dblclick();
        assertThat(column(form, "sName")).hasText(new String[] {"Cherry " + s, "Apple " + s, "Banana " + s});
        assertArrows(form, "sQty asc");
        close(form);
    }

    // a FILTER of an ON ORDER handler, which the reading of the group fires, filters the rows that reading reads, and
    // fires no ON FILTER of its own: a FILTER from an action after it fires it once, and the count would be one more
    // had the handler's fired it (#1870)
    @Test
    public void aFilterOfAnOnOrderHandlerFiltersTheRowsWithNoEventOfItsOwn() throws Exception {
        String s = seed();
        Locator form = open("UiReactOrders.uiRor", "UiReactOrders.uiRorState");
        Locator names = column(form, "sName"), events = value(form, "uiRorFilterEvents");
        assertThat(names).hasText(new String[] {"Apple " + s, "Banana " + s, "Cherry " + s});
        assertThat(events).hasText("1"); // the first reading of the group
        button(form, "uiRorFilterOnOrderOn").click();
        header(form, "sName").dblclick();
        assertThat(names).hasText(new String[] {"Banana " + s, "Apple " + s});
        assertArrows(form, "sName desc");
        assertThat(events).hasText("1");
        button(form, "uiRorFilterNone").click();
        assertThat(names).hasText(new String[] {"Cherry " + s, "Banana " + s, "Apple " + s});
        assertThat(events).hasText("2");
        close(form);
    }

    // a FILTER from an action is applied on the server at once - FILTERS right after it reads the new condition - and
    // the rows filtered, and a FILTER of nothing takes it off, FILTERS reading no condition - an empty value -, with
    // nothing sent back and no FILTERS user event (#1856)
    @Test
    public void aFilterFromAnActionIsAppliedWithNothingSentBack() throws Exception {
        String s = seed();
        Requests sent = requests();
        Locator form = open("UiReactOrders.uiRor", "UiReactOrders.uiRorState");
        Locator names = column(form, "sName");
        Locator read = value(form, "uiRorFiltersRead");
        button(form, "uiRorFilterQty").click();
        assertThat(names).hasText(new String[] {"Apple " + s, "Banana " + s});
        assertJson(read, "[{\"compare\":\">\",\"negation\":false,\"property\":\"sQty\",\"value\":\"2\"}]");
        button(form, "uiRorFilterNone").click();
        assertThat(names).hasText(new String[] {"Apple " + s, "Banana " + s, "Cherry " + s});
        assertJson(read, "Empty string");
        // a request of the user's after them: what would have been sent back by then is
        header(form, "sName").dblclick();
        assertThat(names).hasText(new String[] {"Cherry " + s, "Banana " + s, "Apple " + s});
        assertArrows(form, "sName desc");
        assertEquals("the conditions sent", 0, sent.count("SetUserFilters"));
        assertThat(value(form, "uiRorFiltersEvents")).hasText("");
        close(form);
    }

    // a FILTERGROUP from an action selects the filter on the server at once - FILTERGROUPS right after it reads it -,
    // the list shows it, the rows filtered, and 0 selects none, read back as 0, with nothing sent back and no
    // FILTERGROUPS user event; a choice of the user's is sent, once, and fires it (#1857)
    @Test
    public void aFilterGroupFromAnActionIsShownWithNothingSentBack() throws Exception {
        String s = seed();
        Requests sent = requests();
        Locator form = open("UiReactOrders.uiRor", "UiReactOrders.uiRorState");
        Locator names = column(form, "sName"), list = form.locator("select.filter-group-select");
        Locator read = value(form, "uiRorFilterGroupRead");
        button(form, "uiRorFilterGroupSecond").click();
        assertThat(list.locator("option:checked")).hasText("Qty <= 2");
        assertThat(names).hasText(new String[] {"Cherry " + s});
        assertThat(read).hasText("2");
        button(form, "uiRorFilterGroupNone").click();
        assertThat(list.locator("option:checked")).hasText("(All)");
        assertThat(names).hasText(new String[] {"Apple " + s, "Banana " + s, "Cherry " + s});
        assertThat(read).hasText("0");
        assertThat(value(form, "uiRorFilterGroupsEvents")).hasText("");

        list.selectOption(new SelectOption().setLabel("Qty > 2"));
        assertThat(names).hasText(new String[] {"Apple " + s, "Banana " + s});
        assertThat(value(form, "uiRorFilterGroupsEvents")).hasText("1");
        assertEquals("the filters selected sent", 1, sent.count("SetRegularFilter"));
        close(form);
    }

    // an ACTIVATE TAB from an action shows the tab with nothing sent back and no TAB user event, two of one container
    // in one action the last one; a click on a tab is sent, and fires the event; and an ON TAB handler activating both
    // tabs of its container answers the click once, with no loop (#1858). The requests are counted once the event of
    // the last click shows: what was sent before it is in by then. As the form opens, the client selects its first tab
    // - the server has none selected - and sends that
    @Test
    public void aTabActivatedFromAnActionIsShownWithNothingSentBack() throws Exception {
        seed();
        Requests sent = requests();
        Locator form = open("UiReactOrders.uiRor", "UiReactOrders.uiRorState");
        Locator first = tab(form, "uiRorFirst"), second = tab(form, "uiRorSecond"), events = value(form, "uiRorTabEvents");
        assertActive(first, second);
        button(form, "uiRorActivateSecond").click();
        assertActive(second, first);
        first.click();
        assertActive(first, second);
        second.click();
        assertActive(second, first);
        assertThat(events).hasText("1");
        assertEquals("the tabs sent", 3, sent.count("SetTabActive"));

        first.click();
        assertActive(first, second);
        button(form, "uiRorActivateBoth").click();
        assertActive(second, first);
        button(form, "uiRorTabLoopOn").click();
        first.click();
        second.click();
        assertActive(second, first);
        assertThat(events).hasText("2");
        first.click(); // two clicks after it: what a loop would have sent is in before them
        second.click();
        assertActive(second, first);
        assertThat(events).hasText("3");
        assertEquals("the tabs sent", 8, sent.count("SetTabActive"));
        close(form);
    }

    // what ON INIT applies - a sorting, the filter of a filter group, the tab shown - is shown as the form opens, with
    // nothing sent back and no user events (#1857, #1858); the sorting, by the price, puts the rows in an order other
    // than the one they were made in
    @Test
    public void whatTheFormAppliesAsItOpensIsShownWithNothingSentBack() throws Exception {
        String s = seed();
        Requests sent = requests();
        Locator form = open("UiReactOrders.uiRor", "UiReactOrders.uiRorInit");
        Locator first = tab(form, "uiRorInitFirst"), second = tab(form, "uiRorInitSecond");
        assertActive(second, first);
        assertThat(value(form, "uiRorTabEvents")).hasText("");
        assertThat(value(form, "uiRorFilterGroupsEvents")).hasText("");

        first.click();
        assertActive(first, second);
        assertThat(form.locator("select.filter-group-select option:checked")).hasText("Init qty > 2");
        assertThat(column(form, "iName")).hasText(new String[] {"Banana " + s, "Apple " + s});
        assertArrows(form, "iPrice asc");
        assertThat(value(form, "uiRorTabEvents")).hasText("1");
        assertEquals("the tabs sent", 1, sent.count("SetTabActive"));
        assertEquals("the filters selected sent", 0, sent.count("SetRegularFilter"));
        assertEquals("the sortings sent", 0, sent.count("SetPropertyOrders"));
        close(form);
    }

    // a NONULL filter group of one filter - a check box - has the filter selected as the form opens, with nothing sent
    // back, and a FILTERGROUP of 0 clears it, FILTERGROUPS reading 0 right after it; a click of the user's is sent
    // (#1857)
    @Test
    public void aCheckBoxOfAFilterGroupFollowsTheServer() throws Exception {
        String s = seed();
        Requests sent = requests();
        Locator form = open("UiReactOrders.uiRor", "UiReactOrders.uiRorNoNull");
        Locator names = column(form, "nName"), check = form.locator(".filter-group-check input");
        assertThat(check).isChecked();
        assertThat(names).hasText(new String[] {"Apple " + s, "Banana " + s});
        button(form, "uiRorNoNullNone").click();
        assertThat(check).not().isChecked();
        assertThat(names).hasText(new String[] {"Apple " + s, "Banana " + s, "Cherry " + s});
        assertThat(value(form, "uiRorFilterGroupRead")).hasText("0");
        check.click();
        assertThat(check).isChecked();
        assertThat(names).hasText(new String[] {"Apple " + s, "Banana " + s});
        assertEquals("the filters selected sent", 1, sent.count("SetRegularFilter"));
        close(form);
    }

    // the default filter of a list, and the first one of a NONULL list, are what the server selects as the form opens:
    // the lists show them, and the rows are filtered by them, with nothing sent back; a choice of the user's is sent
    // (#1857)
    @Test
    public void theDefaultFiltersAreShownAsTheFormOpens() throws Exception {
        String s = seed();
        Requests sent = requests();
        Locator form = open("UiReactOrders.uiRor", "UiReactOrders.uiRorDefaults");
        Locator names = column(form, "dName"), lists = form.locator("select.filter-group-select");
        assertThat(lists.locator("option:checked")).hasText(new String[] {"DF qty > 6", "DF qty < 100"});
        assertThat(names).hasText(new String[] {"Banana " + s});
        lists.first().selectOption(new SelectOption().setLabel("(All)"));
        assertThat(names).hasText(new String[] {"Apple " + s, "Banana " + s, "Cherry " + s});
        assertEquals("the filters selected sent", 1, sent.count("SetRegularFilter"));
        close(form);
    }

    // a user event is for what the user changes (#1871): Enter in the value of a condition a FILTER of an action put
    // into the filter panel applies the same conditions again - a request that changes nothing, which fires no
    // user event -, and the condition taken off fires FILTERS, and no FILTERS PROPERTY, there being no condition
    // on the property any more
    @Test
    public void aFilterRequestThatChangesNothingFiresNoUserEvent() throws Exception {
        String s = seed();
        Requests sent = requests();
        Locator form = open("UiReactOrders.uiRor", "UiReactOrders.uiRorState");
        Locator names = column(form, "sName");
        button(form, "uiRorFilterQty").click();
        assertThat(names).hasText(new String[] {"Apple " + s, "Banana " + s});
        Locator condition = form.locator("div.filter");
        assertThat(condition).hasCount(1);

        condition.locator(".filter-data-property-value").click();
        idle();
        page.keyboard().press("Enter");
        idle();
        condition.locator("button[title='Remove filter']").click();
        assertThat(condition).hasCount(0);
        assertThat(names).hasText(new String[] {"Apple " + s, "Banana " + s, "Cherry " + s});
        assertThat(value(form, "uiRorFiltersEvents")).hasText("1");
        assertThat(value(form, "uiRorFiltersPropertyEvents")).hasText("");
        assertEquals("the conditions sent", 2, sent.count("SetUserFilters"));
        close(form);
    }

    // the items, named with a suffix no other attempt has - what a failed attempt saved stays in the database for its
    // rerun -, shown from now on: Apple of 1.5 and 5, Banana of 0.75 and 9, Cherry of 2.25 and 1
    private static String seed() throws Exception {
        String suffix = String.valueOf(System.nanoTime());
        exec("UiReactOrders.uiRorSeed", suffix);
        return suffix;
    }

    // calls of the members of the board's view, with its data - failing with what they throw
    private void board(String calls) {
        Object refusal = call(calls);
        if (refusal != null)
            throw new AssertionError(calls + ": " + refusal);
    }

    // ... and calls refused: what they throw, failing if they do not
    private String refusal(String calls) {
        Object refusal = call(calls);
        if (refusal == null)
            throw new AssertionError(calls + ": not refused");
        return (String) refusal;
    }

    private Object call(String calls) {
        return page.evaluate("() => { const {data, controller} = window.uiRorBoardState;"
                + " try { " + calls + "; return null; } catch (e) { return e.message; } }");
    }

    // the requests the page sends from now on, by the class of the action each carries (GWT-RPC names it in the body)
    private static final class Requests {
        private final List<String> bodies = Collections.synchronizedList(new ArrayList<>());

        int count(String action) {
            synchronized (bodies) {
                return (int) bodies.stream().filter(body -> body.contains("." + action + "/")).count();
            }
        }
    }

    private Requests requests() {
        Requests requests = new Requests();
        page.onRequest(request -> {
            String body = request.postData();
            if (body != null)
                requests.bodies.add(body);
        });
        return requests;
    }

    // the form's requests held from now on, until let go: the server sees none of them, so their answers wait
    private final List<Route> held = new ArrayList<>();

    private void hold() {
        page.route("**/main/dispatch", held::add);
    }

    // ... the requests held let go, in the order they were made, and no more held
    private void release() {
        page.unroute("**/main/dispatch");
        for (Route route : held)
            route.resume();
        held.clear();
    }

    // until no request blocks the input, which the client drops while one does
    private void idle() {
        page.locator(".lsf-blocking").waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.HIDDEN));
    }

    // the cells of a column, in the order the grid shows its rows
    private static Locator column(Locator form, String property) {
        return form.locator("td[lsfusion-container='PROPERTY(" + property + ")']");
    }

    // a column's header as the table shows it: the table keeps a copy of its header out of sight
    private static Locator header(Locator form, String property) {
        return form.locator("th[lsfusion-container='PROPERTY(" + property + ")']:visible");
    }

    // the arrows the header shows, each "<property> asc|desc" in the order of the columns - one reading of all of them;
    // an image of neither direction as its classes, so that it matches no arrow
    private static final String ARROWS = "form => Array.from(form.querySelectorAll('th')).filter(th => {"
            + " const box = th.getBoundingClientRect(); return box.width > 0 && box.height > 0"
            + " && getComputedStyle(th).visibility !== 'hidden' && th.querySelector('.sort-img'); })"
            + ".map(th => { const image = th.querySelector('.sort-img').className,"
            + " up = image.includes('arrow-up'), down = image.includes('arrow-down');"
            + " return th.getAttribute('lsfusion-container').replace(/^PROPERTY\\((.*)\\)$/, '$1')"
            + " + (up !== down ? (up ? ' asc' : ' desc') : ' [' + image + ']'); }).join(', ')";

    // ... until they are exactly these, and no others, failing with what they are then
    private void assertArrows(Locator form, String... arrows) {
        String expected = String.join(", ", arrows);
        long deadline = System.currentTimeMillis() + 10_000;
        while (true) {
            Object shown = form.evaluate(ARROWS);
            if (expected.equals(shown))
                return;
            if (System.currentTimeMillis() > deadline)
                throw new AssertionError("the arrows are '" + shown + "', not '" + expected + "'");
            page.waitForTimeout(100);
        }
    }

    // the JSON a panel property shows, which the platform indents, compacted and with the keys of each object sorted -
    // the server writes them in no order of its own -, or the text it shows for what is no JSON, "Empty string" for an
    // empty value, until it is exactly this, failing with what it is then
    private static final String JSON = "value => { const text = value.textContent.trim(); try {"
            + " return JSON.stringify(JSON.parse(text), (key, item) => item && typeof item === 'object'"
            + " && !Array.isArray(item) ? Object.fromEntries(Object.entries(item).sort()) : item);"
            + " } catch (e) { return text; } }";

    private void assertJson(Locator value, String expected) {
        long deadline = System.currentTimeMillis() + 10_000;
        while (true) {
            Object shown = value.evaluate(JSON);
            if (expected.equals(shown))
                return;
            if (System.currentTimeMillis() > deadline)
                throw new AssertionError("the value is '" + shown + "', not '" + expected + "'");
            page.waitForTimeout(100);
        }
    }

    // a tab of the platform's tab strip, by its container
    private static Locator tab(Locator form, String container) {
        return form.locator("[lsfusion-tab='" + container + "']");
    }

    // the tab shown, and the one not
    private static void assertActive(Locator shown, Locator hidden) {
        assertThat(shown).hasClass(Pattern.compile("\\bactive\\b"));
        assertThat(hidden).not().hasClass(Pattern.compile("\\bactive\\b"));
    }

    // the value a panel property of the form level shows: the element named after the property is the value itself
    // in a panel of several of them, and holds the value otherwise
    private static Locator value(Locator form, String property) {
        String name = "[lsfusion-container='PROPERTY(" + property + "())']";
        return form.locator(".panel-renderer-value" + name + ", " + name + " .panel-renderer-value");
    }

    // a button the platform draws, by its action
    private static Locator button(Locator form, String action) {
        return form.locator("button[lsfusion-container='PROPERTY(" + action + "())']");
    }
}
