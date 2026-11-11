package lsfusion.tests;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

// the selection of a grid whose rows a React view draws (UiReactGrid.lsf, its view UiReactGrid.js), made through the
// members of the view's controller as the platform's table makes its own: the rows selected a range the server keeps,
// over the rows the view has not read too, the columns the view's, and the form reading both (#1675)
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
}
