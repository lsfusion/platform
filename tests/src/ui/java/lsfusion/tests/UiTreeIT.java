package lsfusion.tests;

import com.microsoft.playwright.Locator;
import org.junit.Test;

// a double click on a tree's node unfolds it
public class UiTreeIT extends UiTest {

    @Test
    public void expandNode() throws Exception {
        long id = System.nanoTime(); // unique : the node of a failed attempt stays in the database for its rerun
        String category = "Category " + id, item = "Item " + id;
        exec("UiTree.uiTreeSeed", category, item);

        Locator form = open("UiTree.uiTree", "UiTree.uiTreeItems");
        // a mouse down on another row's node only selects the row (DataGrid.onCellBefore), a double click opens it
        cell(form, "uiTreeCategoryName(c)", category).dblclick();
        cell(form, "uiTreeItemName(i)", item).waitFor();
        close(form);
    }
}
