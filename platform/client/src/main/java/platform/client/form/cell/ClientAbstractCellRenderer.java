package platform.client.form.cell;

import platform.client.form.PropertyRendererComponent;
import platform.client.logics.ClientPropertyDraw;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.util.ArrayList;

// приходится наследоваться от JComponent только для того, чтобы поддержать updateUI
public class ClientAbstractCellRenderer extends JComponent
                                 implements TableCellRenderer {


    public Component getTableCellRendererComponent(JTable table,
                                                   Object value,
                                                   boolean isSelected,
                                                   boolean hasFocus,
                                                   int row,
                                                   int column) {
        CellTableInterface cellTable = (CellTableInterface) table;
        ClientPropertyDraw property = cellTable.getProperty(column);
        PropertyRendererComponent currentComp = property.getRendererComponent();
        currentComp.setValue(value, isSelected, hasFocus);

        JComponent comp = currentComp.getComponent();

        if (cellTable.getHighlightValue(row) != null) {
            Color highlightColor = cellTable.getHighlightColor();
            if (highlightColor == null) {
                highlightColor = Color.yellow;
            }
            
            if (!hasFocus && !isSelected) {
                comp.setBackground(highlightColor);
            } else {
                Color bgColor = comp.getBackground();
                comp.setBackground(new Color(highlightColor.getRGB() & bgColor.getRGB()));
            }
        }

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
