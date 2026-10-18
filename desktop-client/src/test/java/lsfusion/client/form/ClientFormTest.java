package lsfusion.client.form;

import lsfusion.client.form.design.ClientContainer;
import lsfusion.client.form.object.table.grid.ClientGrid;
import org.junit.Test;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

// A component the form's changes name by its id is found wherever the design has it: what a grid's record holds - what a
// design puts into GRID(g), a container with no container of its own, inside its grid - too, as what any container
// holds is. The changes name a container by its id for its caption, a tab to activate, a container to collapse, and any
// component for its SHOWIF and class
public class ClientFormTest {
    private final ClientForm form = new ClientForm();
    private final ClientContainer main = new ClientContainer();
    private final ClientGrid grid = new ClientGrid();
    private final ClientContainer record = new ClientContainer();

    public ClientFormTest() {
        main.ID = 1; main.main = true;
        form.mainContainer = main;
        grid.ID = 2; grid.container = main;
        main.children.add(grid);
        record.ID = 3;
        grid.record = record;
    }

    private static ClientContainer container(int id, ClientContainer parent) {
        ClientContainer container = new ClientContainer();
        container.ID = id; container.container = parent;
        parent.children.add(container);
        return container;
    }

    @Test
    public void whatAGridsRecordHoldsIsFoundById() {
        ClientContainer card = container(4, record), pages = container(5, record), page = container(6, pages);

        assertSame(card, form.findContainerByID(4));
        assertSame(page, form.findContainerByID(6));
        assertSame(page, form.findComponentByID(6));
        assertSame(grid, form.findComponentByID(2));
        assertNull(form.findContainerByID(2)); // a grid is no container
    }

    // ... and a grid the design puts nothing into has no record to walk
    @Test
    public void aGridWithNoRecordHoldsNothing() {
        grid.record = null;
        assertNull(form.findComponentByID(3));
        assertSame(grid, form.findComponentByID(2));
    }
}
