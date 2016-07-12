package lsfusion.client.form.grid;

import lsfusion.client.form.ClientFormController;
import lsfusion.client.logics.ClientGrid;

import javax.swing.*;
import java.awt.*;

import static lsfusion.client.SwingUtils.overrideSize;

public class GridView extends JPanel {
    final JScrollPane pane;

    private final ClientGrid grid;
    private final GridTable gridTable;
    private final GridController gridController;

    public GridView(GridController igridController, ClientFormController form, GridUserPreferences[] iuserPreferences, boolean tabVertical, boolean verticalScroll) {
        super(new BorderLayout());

        gridController = igridController;

        grid = gridController.getGroupController().getGroupObject().grid;

        gridTable = new GridTable(this, form, iuserPreferences);

        gridTable.setTabVertical(tabVertical);

        pane = new JScrollPane(gridTable) {
            @Override
            public void doLayout() {
                // хак, чтобы не изменялся ряд при изменении размеров таблицы,
                // а вместо этого она скроллировалась к видимой строчке
                gridTable.setLayouting(true);
                super.doLayout();
                gridTable.setLayouting(false);
            }

            @Override
            public boolean isValidateRoot() {
                return false;
            }
        };
        pane.setVerticalScrollBarPolicy(verticalScroll ? ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED : ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
        pane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        gridTable.setFillsViewportHeight(true);

        gridTable.configureEnclosingScrollPane(pane);

        grid.installMargins(this);

        add(pane, BorderLayout.CENTER);
    }

    @Override
    public Dimension getMinimumSize() {
        return overrideSize(super.getMinimumSize(), grid.minimumSize);
    }

    @Override
    public Dimension getMaximumSize() {
        return overrideSize(super.getMaximumSize(), grid.maximumSize);
    }

    @Override
    public Dimension getPreferredSize() {
        return overrideSize(super.getPreferredSize(), grid.preferredSize);
    }

    public GridController getGridController() {
        return gridController;
    }

    public GridTable getTable() {
        return gridTable;
    }
}