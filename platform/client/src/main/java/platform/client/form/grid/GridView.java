package platform.client.form.grid;

import platform.client.form.ClientFormController;
import platform.client.form.GroupObjectController;

import javax.swing.*;
import java.awt.*;

public abstract class GridView extends JPanel {
    final JScrollPane pane;

    private final GridTable gridTable;

    public GridTable getTable() {
        return gridTable;
    }

    public GridView(GroupObjectController groupObjectController, ClientFormController form, boolean tabVertical, boolean verticalScroll) {
        setLayout(new BorderLayout());

        gridTable = new GridTable(groupObjectController, form) {
            protected void needToBeShown() {
                GridView.this.needToBeShown();
            }

            protected void needToBeHidden() {
                GridView.this.needToBeHidden();
            }
        };

        gridTable.setTabVertical(tabVertical);

        pane = new JScrollPane(gridTable);
        int verticalConst = verticalScroll ? ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED : ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER;
        pane.setVerticalScrollBarPolicy(verticalConst);
        pane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        gridTable.setFillsViewportHeight(true);

        add(pane, BorderLayout.CENTER);
        add(groupObjectController.getToolbarView(), BorderLayout.SOUTH);
    }

    protected abstract void needToBeShown();

    protected abstract void needToBeHidden();
}
