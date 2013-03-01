package platform.client.form.cell;

import platform.base.BaseUtils;
import platform.client.Main;
import platform.client.SwingUtils;
import platform.client.form.ClientFormController;
import platform.client.form.ClientPropertyTable;
import platform.client.logics.ClientGroupObjectValue;
import platform.client.logics.ClientPropertyDraw;

import javax.swing.table.TableCellEditor;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.text.ParseException;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

public abstract class SingleCellTable extends ClientPropertyTable {

    private final SingleCellTableModel model;

    public SingleCellTable(ClientGroupObjectValue columnKey) {
        super(new SingleCellTableModel(columnKey));

        model = (SingleCellTableModel) getModel();

        SwingUtils.setupSingleCellTable(this);
    }

    public void setProperty(ClientPropertyDraw property) {
        setName(property.getCaption());
        model.setProperty(property);

        setMinimumSize(property.getMinimumSize(this));
        setPreferredSize(property.getPreferredSize(this));
        setMaximumSize(property.getMaximumSize(this));
    }

    public void setValue(Object value) {
        model.setValue(value);
        repaint();
    }

    @Override
    public int getCurrentRow() {
        return 0;
    }

    public ClientGroupObjectValue getColumnKey(int row, int col) {
        return model.getColumnKey();
    }

    public ClientPropertyDraw getProperty() {
        return model.getProperty();
    }

    public ClientPropertyDraw getProperty(int row, int column) {
        return model.getProperty();
    }

    @Override
    public String getToolTipText(MouseEvent e) {
        Object value = model.getValue();
        if (!BaseUtils.isRedundantString(value)) {
            String tooltip = value.toString();
            if (value instanceof Date) {
                tooltip = Main.formatDate(value);
            } else if (value instanceof Double) {
                tooltip = String.valueOf((double) Math.round(((Double) value) * 1000) /1000);
            } else if (value instanceof Color) {
                tooltip = "#" + Integer.toHexString(((Color) value).getRGB()).substring(2, 8);
            } else if (getProperty().echoSymbols) {
                tooltip = null;
            }
            return tooltip == null ? null : SwingUtils.toMultilineHtml(BaseUtils.rtrim(tooltip), createToolTip().getFont());
        } else {
            return null;
        }
    }

    public void pasteTable(List<List<String>> table) {
        if (!table.isEmpty() && !table.get(0).isEmpty()) {
            try {
                Object value = model.getProperty().parseString(getForm(), model.getColumnKey(), table.get(0).get(0), true);
                if (value != null) {
                    pasteValue(value);
                }
            } catch (ParseException ignored) {
            }
        }
    }

    public boolean isSelected(int row, int column) {
        return false;
    }

    // приходится делать вот таким извращенным способом, поскольку ComponentListener срабатывает после перерисовки формы
    @Override
    public void setBounds(int x, int y, int width, int height) {
        rowHeight = height;
        super.setBounds(x, y, width, height);
    }

    protected abstract void pasteValue(Object value);

    public abstract ClientFormController getForm();

    @Override
    public Component prepareEditor(TableCellEditor editor, int row, int column) {
        Component editorComp = super.prepareEditor(editor, row, column);
        if (editorComp != null) {
            //вырезаем traversal-кнопки, потому что иначе фокус просто вернётся в таблицу
            editorComp.setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, new HashSet<AWTKeyStroke>());
            editorComp.setFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS, new HashSet<AWTKeyStroke>());
        }
        return editorComp;
    }
}
