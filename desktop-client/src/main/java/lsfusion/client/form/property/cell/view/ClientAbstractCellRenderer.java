package lsfusion.client.form.property.cell.view;

import lsfusion.client.form.property.ClientPropertyDraw;
import lsfusion.client.form.property.cell.classes.view.StringPropertyRenderer;
import lsfusion.client.form.property.table.view.CellTableInterface;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

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

        PropertyRenderer currentComp;
        Object valueToSet = value;
        if (property != null) {
            currentComp = property.getRendererComponent();
        } else {
            currentComp = nullPropertyRenderer;
            valueToSet = "";
        }

        currentComp.updateRenderer(valueToSet,
                isSelected,
                hasFocus,
                drawFocusBorder(),
                cellTable.paintSelected(row, column),
                cellTable.hasSingleSelection(),
                table.hasFocus(),
                cellTable.getBackgroundColor(row, column),
                cellTable.getForegroundColor(row, column));

        JComponent comp = currentComp.getComponent();
        if (comp instanceof JButton) {
            ((JButton) comp).setSelected(cellTable.isPressed(row, column));
        }
        
        if (property != null) {
            comp.setFont(property.design.getFont(table));
        }

        renderers.add(comp);

        return comp;
    }
    
    protected boolean drawFocusBorder() {
        return true;
    }

    private final List<JComponent> renderers = new ArrayList<>();
    @Override
    public void updateUI() {
        for (JComponent comp : renderers) {
            comp.updateUI();
        }
    }
}
