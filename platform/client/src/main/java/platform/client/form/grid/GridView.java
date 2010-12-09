package platform.client.form.grid;

import platform.client.form.ClientFormController;
import platform.client.form.GroupObjectLogicsSupplier;
import platform.client.form.queries.FilterController;
import platform.client.form.queries.FindController;
import platform.client.logics.ClientPropertyDraw;

import javax.swing.*;
import java.awt.*;

public abstract class GridView extends JPanel {

    final JPanel queriesContainer;

    final JScrollPane pane;
    final GridBagConstraints paneConstraints;

    private final GridTable gridTable;
    public final FilterController filterController;

    public GridTable getTable() {
        return gridTable;
    }

    public GridView(GroupObjectLogicsSupplier logicsSupplier, ClientFormController form, FindController findController, FilterController filterController, boolean tabVertical) {
        this.filterController = filterController;

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

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
        pane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
        pane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        gridTable.setFillsViewportHeight(true);

        paneConstraints = new GridBagConstraints();
        paneConstraints.fill = GridBagConstraints.BOTH;
        paneConstraints.weightx = 1;
        paneConstraints.weighty = 1;
        paneConstraints.insets = new Insets(4, 4, 4, 4);

        queriesContainer = new JPanel();
        queriesContainer.setLayout(new BoxLayout(queriesContainer, BoxLayout.X_AXIS));

        if (findController != null) {
            queriesContainer.add(findController.getView());
            queriesContainer.add(Box.createRigidArea(new Dimension(4, 0)));
            findController.getView().addActions(gridTable);
        }

        if (filterController != null) {
            queriesContainer.add(filterController.getView());
            filterController.getView().addActions(gridTable);
        }

        queriesContainer.add(Box.createHorizontalGlue());

        add(pane);
        add(queriesContainer);

    }

    protected abstract void needToBeShown();

    protected abstract void needToBeHidden();

    public void quickEditFilter() {
        quickEditFilter(null);
    }

    public void quickEditFilter(ClientPropertyDraw propertyDraw) {
        if (filterController != null) {
            filterController.quickEditFilter(propertyDraw);
        }
    }
}
