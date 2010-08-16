package platform.client.form.cell;

import platform.client.form.PropertyRendererComponent;
import platform.client.form.decorator.ClientHighlighter;
import platform.client.form.decorator.HighlighterContext;
import platform.client.logics.ClientCell;
import platform.client.logics.ClientGroupObject;

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
        ClientCell property = cellTable.getCell(column);
        PropertyRendererComponent currentComp = property.getRendererComponent();
        currentComp.setValue(value, isSelected, hasFocus);

        JComponent comp = currentComp.getComponent();

        ClientGroupObject groupObject = property.getGroupObject();
        if (groupObject != null) {
            ClientHighlighter highlighter = groupObject.grid.highlighter;
            if (highlighter != null) {
                highlighter.highlight(comp, new HighlighterContext(cellTable, value, isSelected, hasFocus, row, column));
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
