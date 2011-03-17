package platform.client.form.grid;

import platform.client.form.ClientFormController;
import platform.client.form.GroupObjectController;
import platform.client.form.queries.CalculateSumButton;
import platform.client.form.queries.CountQuantityButton;
import platform.client.form.queries.FilterController;
import platform.client.form.queries.FindController;
import platform.client.logics.ClientPropertyDraw;

import javax.swing.*;
import java.awt.*;

public abstract class GridView extends JPanel {
    final JScrollPane pane;

    private final GridTable gridTable;
    public final FilterController filterController;

    public GridTable getTable() {
        return gridTable;
    }

    public GridView(GroupObjectController groupObjectController,
                    ClientFormController form,
                    FindController findController,
                    FilterController filterController,
                    CountQuantityButton countQuantityButton,
                    CalculateSumButton calculateSumButton,
                    boolean tabVertical,
                    boolean verticalScroll) {
        this.filterController = filterController;

        setLayout(new BorderLayout());

        gridTable = new GridTable(this, groupObjectController, form) {
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


        if (findController != null) {
            groupObjectController.addToToolbar(findController.getView());
            findController.getView().addActions(gridTable);
        }

        if (filterController != null) {
            groupObjectController.addToToolbar(filterController.getView());
            filterController.getView().addActions(gridTable);
        }

        if (countQuantityButton != null) {
            groupObjectController.addToToolbar(countQuantityButton);
        }

        if (calculateSumButton != null) {
            groupObjectController.addToToolbar(calculateSumButton);
        }

        add(pane, BorderLayout.CENTER);
        add(groupObjectController.getToolbarView(), BorderLayout.SOUTH);
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
