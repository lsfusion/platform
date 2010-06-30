package platform.client.form.grid;

import platform.client.form.ClientForm;
import platform.client.form.GroupObjectLogicsSupplier;
import platform.client.form.queries.QueryView;

import javax.swing.*;
import java.awt.*;

public abstract class GridView extends JPanel {

    final JPanel queriesContainer;

    final JScrollPane pane;
    final GridBagConstraints paneConstraints;

    private final GridTable gridTable;
    public GridTable getTable() {
        return gridTable;
    }

    public GridView(GroupObjectLogicsSupplier logicsSupplier, ClientForm form, QueryView findView, QueryView filterView) {

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        gridTable = new GridTable(logicsSupplier, form) {

            protected void needToBeShown() {
                GridView.this.needToBeShown();
            }

            protected void needToBeHidden() {
                GridView.this.needToBeHidden();
            }
        };

        gridTable.getTableHeader().setPreferredSize(new Dimension(1000, 34));

        pane = new JScrollPane(gridTable);
        pane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
        pane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        gridTable.setFillsViewportHeight(true);

        paneConstraints = new GridBagConstraints();
        paneConstraints.fill = GridBagConstraints.BOTH;
        paneConstraints.weightx = 1;
        paneConstraints.weighty = 1;
        paneConstraints.insets = new Insets(4,4,4,4);

        queriesContainer = new JPanel();
        queriesContainer.setLayout(new BoxLayout(queriesContainer, BoxLayout.X_AXIS));

        if (findView != null) {
            queriesContainer.add(findView);
            queriesContainer.add(Box.createRigidArea(new Dimension(4,0)));
            findView.addActions(gridTable);
        }

        if (filterView != null) {
            queriesContainer.add(filterView);
            filterView.addActions(gridTable);
        }

        queriesContainer.add(Box.createHorizontalGlue());

        add(pane);
        add(queriesContainer);

    }

    protected abstract void needToBeShown();
    protected abstract void needToBeHidden();
}
