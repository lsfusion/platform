package lsfusion.tests;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

// a React view's own filters of the rows it draws, in the browser (UiReactViewFilters.lsf, its React view in
// UiReactViewFilters.js)
public class UiReactViewFiltersIT extends UiTest {

    private final View board = view("window.uiReactViewFilters",
            " const numbers = data && data.i ? data.i.list.map(row => row.number.value).join() : null,"
            + " viewFilters = data && data.i ? data.i.viewFilters.map(f => f.property + f.compare + f.value).join() : null;");

    // the view filters a view states (#1675) filter its rows together with the group's user filters, and the view is
    // shown what it stated; a condition joined by `or` is refused; an empty list - the form's controller's here - takes
    // them off, the user filters staying as they are
    @Test
    public void aViewFiltersTheRowsItDrawsBesideTheUserFilters() throws Exception {
        exec("UiReactViewFilters.uiReactViewFiltersSeed", "10");
        open("UiReactViewFilters.uiReactViewFiltersFolder", "UiReactViewFilters.uiReactViewFilters");
        board.waitFor("numbers === '1,2,3,4,5,6,7,8,9,10' && viewFilters === ''");
        board.answer("controller.form.changeFilters('i', [{property: 'number', compare: '>=', value: 4}])");
        board.answer("controller.i.viewFilters.change([{property: 'number', compare: '<=', value: 6}])");
        board.waitFor("numbers === '4,5,6' && viewFilters === 'number<=6'");
        assertTrue(board.refusal("controller.i.viewFilters.change([{property: 'number', value: 1, or: true}])").contains("'or' cannot be true"));
        board.answer("controller.form.changeViewFilters('i', [])");
        board.waitFor("numbers === '4,5,6,7,8,9,10' && viewFilters === ''");
    }
}
