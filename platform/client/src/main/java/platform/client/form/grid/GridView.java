package platform.client.form.grid;

import platform.client.form.ClientFormController;
import platform.client.form.GroupObjectLogicsSupplier;
import platform.client.form.queries.FilterController;
import platform.client.form.queries.FindController;
import platform.client.logics.ClientPropertyDraw;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public abstract class GridView extends JPanel {

    final JPanel queriesContainer;

    public final JPanel movingPropertiesContainer;

    final JScrollPane pane;

    private final GridTable gridTable;
    public final FilterController filterController;
    public JPanel bottomContainer = new JPanel();
    public GridTable getTable() {
        return gridTable;
    }

    public GridView(GroupObjectLogicsSupplier logicsSupplier, ClientFormController form, FindController findController,
                    FilterController filterController, boolean tabVertical, boolean verticalScroll) {
        this.filterController = filterController;

        setLayout(new BorderLayout());

        gridTable = new GridTable(this, logicsSupplier, form) {
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

        bottomContainer.setLayout(new BorderLayout());

        queriesContainer = new JPanel();
        queriesContainer.setBorder(new EmptyBorder(0, 0, 0, 0));
        queriesContainer.setLayout(new BoxLayout(queriesContainer, BoxLayout.X_AXIS));
        movingPropertiesContainer = new JPanel();
        movingPropertiesContainer.setLayout(new BoxLayout(movingPropertiesContainer, BoxLayout.X_AXIS));

        if (findController != null) {
            queriesContainer.add(findController.getView());
            findController.getView().addActions(gridTable);
        }

        if (filterController != null) {
            queriesContainer.add(filterController.getView());
            filterController.getView().addActions(gridTable);
        }

        bottomContainer.add(queriesContainer, BorderLayout.WEST);
        bottomContainer.add(movingPropertiesContainer, BorderLayout.CENTER);

        add(pane, BorderLayout.CENTER);
        add(bottomContainer, BorderLayout.SOUTH);
    }

    protected abstract void needToBeShown();

    protected abstract void needToBeHidden();

    public void quickEditFilter() {
        quickEditFilter(null);
    }

    public void quickEditFilter(ClientPropertyDraw propertyDraw) {
        if (filterController != null) {
            filterController.quickEditFilter(propertyDraw);
            gridTable.selectProperty(propertyDraw);
        }
    }

    public boolean hasActiveFilter() {
        return filterController != null && filterController.hasActiveFilter();
    }
}
