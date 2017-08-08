package lsfusion.client.form.grid;

import lsfusion.base.BaseUtils;
import lsfusion.client.form.ClientFormController;
import lsfusion.client.form.grid.preferences.GridUserPreferences;
import lsfusion.client.form.layout.JComponentPanel;
import lsfusion.client.logics.ClientComponent;
import lsfusion.client.logics.ClientGrid;

import javax.swing.*;
import java.awt.*;

import static lsfusion.client.SwingUtils.overrideSize;

public class GridView extends JComponentPanel {
    final JScrollPane pane;

    private final ClientGrid grid;
    private final GridTable gridTable;
    private final GridController gridController;

    @Override
    public Dimension getMaxPreferredSize() { // ради этого вся ветка maxPreferredSize и делалась
        Dimension preferredTableSize = gridTable.getPreferredScrollableViewportSize();
        Dimension preferredAutoTableSize = gridTable.getPreferredSize();
        Dimension preferredSize = getPreferredSize(); // чтобы учесть header отступы и т.п.
        return new Dimension(BaseUtils.max(preferredSize.width - preferredTableSize.width + preferredAutoTableSize.width, preferredSize.width), // max, 130 px
                             BaseUtils.max(preferredSize.height - preferredTableSize.height + preferredAutoTableSize.height, preferredSize.height)); // max, 130 px
    }

    public GridView(GridController igridController, ClientFormController form, GridUserPreferences[] iuserPreferences, boolean tabVertical, boolean verticalScroll) {
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

    public GridController getGridController() {
        return gridController;
    }

    public GridTable getTable() {
        return gridTable;
    }

    public int getHeaderHeight() {
        return grid.getHeaderHeight();
    }
}