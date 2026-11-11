package lsfusion.tests;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

// the selection of a grid whose rows a React view draws (UiReactGrid.lsf, its view UiReactGrid.js), made through the
// members of the view's controller as the platform's table makes its own: the rows selected a range the server keeps,
// over the rows the view has not read too, the columns the view's, the form reading both - and copied and pasted as
// the table copies and pastes its own (#1675)
public class UiReactGridIT extends UiTest {

    private static final String FOLDER = "UiReactGrid.uiRg";

    private final View view = view("window.uiRgView",
            "const rows = data && data.i ? data.i.list : [], current = rows.find(row => row.current), i = controller && controller.i;");

    // the form over 500 items, the first of them current, a page of them read
    private void openGrid() throws Exception {
        exec("UiReactGrid.uiRgSeed", "500");
        open(FOLDER, "UiReactGrid.uiRgGrid");
        view.waitFor("current && current.number.value === 1");
        assertTrue(((Number) view.value("rows.length")).intValue() < 500);
    }

    // a click on a cell is a row and a column chosen in one batch, its one request carrying both; a range of rows goes
    // on to the last row of the whole list, which the server finds, and takes in the rows the view has not read; a
    // column chosen alone sends no row; the form reads the rows and the columns selected, the active property, and its
    // event of a column's selection occurs; every row and column is selected with the current ones staying
    @Test
    public void rowsAndColumnsAreSelectedAsThePlatformsTableSelectsThem() throws Exception {
        openGrid();
        List<String> requests = requests(request -> request.method().equals("POST"));
        view.answer("controller.batch(() => { i.change(rows[1], {selection: 'start'}); i.properties.change('qty', {selection: 'start'}); })");
        view.waitFor("current.number.value === 2 && data.i.qty.current && data.i.qty.selected && !data.i.name.selected");
        view.waitFor("data.selectedCount.value === 1 && data.qtySelected.value === true && data.qtyActive.value === true && data.qtyEvents.value === 1");
        assertEquals(1, requests.stream().filter(request -> request.contains(signature("ExecuteBatch"))
                && request.contains(signature("ChangeGroupObject")) && request.contains(signature("ChangePropertyActive"))).count());
        assertEquals(0, view.value("rows.filter(row => row.selected).length"));

        view.answer("i.change(controller.form.END, {selection: 'extend'})");
        view.waitFor("current.number.value === 500 && data.selectedCount.value === 499 && rows.every(row => row.selected)");
        requests.clear();
        view.answer("i.properties.change('name', {selection: 'extend'})");
        view.waitFor("data.i.name.current && data.i.name.selected && data.i.qty.selected && !data.i.number.selected");
        view.waitFor("data.nameSelected.value === true && data.qtySelected.value === true && data.qtyActive.value !== true");
        assertEquals(1, count(requests, "ChangePropertyActive"));
        assertEquals(0, count(requests, "ChangeGroupObject"));

        view.answer("i.change({to: 'home'}, {selection: 'keep'})"); // the range stays
        view.waitFor("current.number.value === 1 && data.selectedCount.value === 499 && !current.selected");
        view.answer("controller.form.selectAll('i')");
        view.waitFor("data.selectedCount.value === 500 && current.number.value === 1 && data.i.name.current");
        view.waitFor("data.i.number.selected && data.i.name.selected && data.i.qty.selected");
        view.answer("controller.form.changeObject('i', rows[2].objects, {selection: 'start'})");
        view.waitFor("current.number.value === 3 && data.selectedCount.value === 1 && rows.every(row => !row.selected)");
    }

    // a move to an edge of the list is found when its request ends, as an action's is: in a batch it is found under the
    // filters the batch sets, and an action run after it in the batch still runs on the row that was current
    @Test
    public void anEdgeOfTheListInABatchIsFoundWhenItsRequestEnds() throws Exception {
        openGrid();
        view.answer("controller.batch(() => { i.viewFilters.change([{property: 'number', compare: '>=', value: 400}]); i.change(controller.form.HOME); })");
        view.waitFor("current.number.value === 400 && rows[0].number.value === 400");
        view.answer("controller.batch(() => { i.change(controller.form.END, {selection: 'extend'}); controller.i.capture.exec(); })");
        view.waitFor("current.number.value === 500 && data.captured.value === 400 && data.selectedCount.value === 101");
    }

    // the cells copied and pasted are the rows selected - the current one while none is - in the columns selected: one
    // value goes into every one of them, and into the current row alone while no row is selected - not into every row
    // of the list; a table goes into the cells from the current one on, a property the form shows read only left as it
    // is with the values after it in their own cells; every row is copied, the rows the view has not read too; a batch
    // carries neither. The view's handlers of the browser's copy and paste events do the same from the keyboard
    @Test
    public void theSelectionIsCopiedAndPasted() throws Exception {
        openGrid();
        view.answer("controller.batch(() => { i.change(rows[1], {selection: 'start'}); i.properties.change('qty', {selection: 'start'}); })");
        assertEquals("2", view.eval("i.copy()"));
        view.answer("controller.batch(() => { i.change(rows[3], {selection: 'extend'}); i.properties.change('name', {selection: 'extend'}); })");
        assertEquals("item 2\t2\nitem 3\t3\nitem 4\t4", view.eval("controller.form.copy('i')"));
        view.answer("i.paste('70')");
        view.waitFor("[1, 2, 3].every(index => rows[index].name.value === '70' && rows[index].qty.value === 70)");
        view.waitFor("rows[0].qty.value === 1 && rows[4].qty.value === 5");
        view.answer("i.change(rows[5], {selection: 'start'})");
        view.answer("controller.form.paste('i', '80')");
        view.waitFor("rows[5].name.value === '80' && rows[5].qty.value === 80 && rows[4].qty.value === 5 && rows[6].qty.value === 7 && rows[3].qty.value === 70");

        view.answer("controller.batch(() => { i.change(rows[7], {selection: 'start'}); i.properties.change('number', {selection: 'start'}); })");
        view.answer("i.paste('900\\ta\\t91\\n901\\tb\\t92')");
        view.waitFor("rows[7].number.value === 8 && rows[7].name.value === 'a' && rows[7].qty.value === 91");
        view.waitFor("rows[8].number.value === 9 && rows[8].name.value === 'b' && rows[8].qty.value === 92 && rows[9].qty.value === 10");

        view.answer("i.selectAll()");
        String all = view.eval("i.copy()");
        assertEquals(500, all.split("\n").length);
        assertTrue(all, all.startsWith("1\titem 1\t1\n2\t70\t70\n"));
        String refusal = "a batch is running, which carries changes only: make this call before the batch or after it";
        assertEquals("controller.i.copy(): " + refusal, view.rejection("(() => { let text; controller.batch(() => { text = i.copy(); }); return text; })()"));
        assertEquals("controller.i.paste(): " + refusal, view.rejection("(() => { let pasted; controller.batch(() => { pasted = i.paste('1'); }); return pasted; })()"));

        page.context().grantPermissions(Arrays.asList("clipboard-read", "clipboard-write"));
        view.answer("controller.batch(() => { i.change(rows[0], {selection: 'start'}); i.properties.change('name', {selection: 'start'}); })");
        page.locator(".ui-rg-view").focus();
        page.keyboard().press("Control+C");
        page.waitForFunction("() => navigator.clipboard.readText().then(text => text === 'item 1')");
        page.evaluate("() => navigator.clipboard.writeText('pasted')");
        page.keyboard().press("Control+V");
        view.waitFor("rows[0].name.value === 'pasted' && rows[1].name.value === '70'");

        // with no current row nothing is pasted, as the platform's table has no cell to paste into: no item is added
        view.answer("i.viewFilters.change([{property: 'number', compare: '>=', value: 1000}])");
        view.waitFor("rows.length === 0");
        Object items = view.value("data.items.value");
        view.answer("i.paste('a\\t1\\nb\\t2')");
        view.answer("i.viewFilters.change([])");
        view.waitFor("rows.length > 0");
        assertEquals(items, view.value("data.items.value"));
    }
}
