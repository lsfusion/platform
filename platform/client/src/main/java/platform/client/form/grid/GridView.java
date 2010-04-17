package platform.client.form.grid;

import platform.client.form.queries.QueryView;
import platform.client.form.ClientForm;
import platform.client.form.LogicsSupplier;

import javax.swing.*;
import java.awt.*;

public class GridView extends JPanel {

    final JPanel queriesContainer;

    final JScrollPane pane;
    final GridBagConstraints paneConstraints;

    private final GridTable gridTable;
    public GridTable getTable() {
        return gridTable;
    }

    public GridView(LogicsSupplier logicsSupplier, ClientForm form, QueryView findView, QueryView filterView) {

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        gridTable = new GridTable(logicsSupplier, form) {

            protected void neededToBeShown() {
                GridView.this.setVisible(true);
            }

            protected void neededToBeHidden() {
                GridView.this.setVisible(false);
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

//              отключим поиски пока они не работают
//                queriesContainer.add(findView);
//                queriesContainer.add(Box.createRigidArea(new Dimension(4,0)));
        queriesContainer.add(filterView);
        queriesContainer.add(Box.createHorizontalGlue());

        add(pane);
        add(queriesContainer);

        // делаем, чтобы по нажатию кнопок в гриде вызывались фильтры и поиски
        findView.addActions(gridTable);
        filterView.addActions(gridTable);

    }
}
