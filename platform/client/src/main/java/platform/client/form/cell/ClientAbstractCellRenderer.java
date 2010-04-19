package platform.client.form.cell;

import platform.client.logics.ClientCellView;
import platform.client.form.PropertyRendererComponent;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.util.*;

// приходится наследоваться от JComponent только для того, чтобы поддержать updateUI
public class ClientAbstractCellRenderer extends JComponent
                                 implements TableCellRenderer {


    public Component getTableCellRendererComponent(JTable table,
                                                   Object value,
                                                   boolean isSelected,
                                                   boolean hasFocus,
                                                   int row,
                                                   int column) {

        ClientCellView property = ((ClientCellViewTable)table).getCellView(column);
        PropertyRendererComponent currentComp = property.getRendererComponent();
        currentComp.setValue(value, isSelected, hasFocus);

        JComponent comp = currentComp.getComponent();

        renderers.add(comp);

        return comp;
    }

    private final java.util.List<JComponent> renderers = new ArrayList<JComponent>();
    @Override
    public void updateUI() {
        for (JComponent comp : renderers)
            comp.updateUI();
    }

}
