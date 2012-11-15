package platform.client.form.cell;

import platform.client.form.PropertyRendererComponent;
import platform.client.form.renderer.StringPropertyRenderer;
import platform.client.logics.ClientPropertyDraw;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.util.List;
import java.util.ArrayList;

// приходится наследоваться от JComponent только для того, чтобы поддержать updateUI
public class ClientAbstractCellRenderer extends JComponent implements TableCellRenderer {

    private static final StringPropertyRenderer nullPropertyRenderer = new StringPropertyRenderer(null);

    public Component getTableCellRendererComponent(JTable table,
                                                   Object value,
                                                   boolean isSelected,
                                                   boolean hasFocus,
                                                   int row,
                                                   int column) {
        CellTableInterface cellTable = (CellTableInterface) table;

        ClientPropertyDraw property = cellTable.getProperty(row, column);

        PropertyRendererComponent currentComp;
        if (property != null) {
            currentComp = property.getRendererComponent();
            currentComp.setValue(value, isSelected, hasFocus);
        } else {
            currentComp = nullPropertyRenderer;
            currentComp.setValue("", isSelected, hasFocus);
        }

        if (cellTable.isSelected(row, column) && !hasFocus) {
            currentComp.paintAsSelected();
        }

        JComponent comp = currentComp.getComponent();
        if (comp instanceof JButton) {
            ((JButton) comp).setSelected(cellTable.isPressed(row, column));
        }

        Color backgroundColor = cellTable.getBackgroundColor(row, column);
        if (backgroundColor != null) {
            if (!hasFocus && !isSelected && !cellTable.isSelected(row, column)) {
                comp.setBackground(backgroundColor);
            } else {
                Color bgColor = comp.getBackground();
                comp.setBackground(new Color(backgroundColor.getRGB() & bgColor.getRGB()));
            }
        }

        Color foregroundColor = cellTable.getForegroundColor(row, column);
        if (foregroundColor != null) {
            comp.setForeground(foregroundColor);
        }

        renderers.add(comp);

        return comp;
    }

    private final List<JComponent> renderers = new ArrayList<JComponent>();
    @Override
    public void updateUI() {
        for (JComponent comp : renderers) {
            comp.updateUI();
        }
    }
}
