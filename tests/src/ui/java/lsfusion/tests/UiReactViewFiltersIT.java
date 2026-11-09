package lsfusion.tests;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

// #1675: own view filters intersect user filters and clear independently.
public class UiReactViewFiltersIT extends UiTest {
    private final View grid = view("window.uiReactViewFilters",
            "const rows = data && data.i ? data.i.list : [], current = rows.find(r => r.current);");

    private void openGrid() throws Exception {
        exec("UiReactViewFilters.uiReactViewFiltersSeed", "500");
        open("UiReactViewFilters.uiReactViewFiltersFolder", "UiReactViewFilters.uiReactViewFilters");
        grid.waitFor("rows.length && current");
    }

    @Test
    public void viewFiltersIntersectUserFiltersAndEmptyClearsOnlyTheViewSet() throws Exception {
        openGrid();
        grid.answer("controller.form.changeFilters('i', [{property:'number', compare:'>=', value:400}])");
        grid.answer("controller.i.viewFilters.change([{property:'number', compare:'<=', value:450}])");
        grid.answer("controller.filterLast.exec()");
        grid.waitFor("current.number.value === 450");
        assertTrue(grid.refusal("controller.i.viewFilters.change([{property:'number', value:1, or:true}])").contains("AND only"));
        grid.answer("controller.form.changeViewFilters('i', [])");
        grid.answer("controller.filterLast.exec()");
        grid.waitFor("current.number.value === 500 && data.i.viewFilters.length === 0");
        grid.answer("controller.filterFirst.exec()");
        grid.waitFor("current.number.value === 400");
    }
}
