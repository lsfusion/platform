package platform.client.form.cell;

import com.google.common.base.Throwables;
import platform.client.SwingUtils;
import platform.client.form.ClientFormController;
import platform.client.form.ClientPropertyTable;
import platform.client.logics.ClientGroupObjectValue;
import platform.client.logics.ClientPropertyDraw;

import javax.swing.table.TableCellEditor;
import java.awt.*;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;

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

    public void setReadOnly(boolean readOnly) {
        model.setReadOnly(readOnly);
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

    public void pasteTable(List<List<String>> table) {
        if (!table.isEmpty() && !table.get(0).isEmpty()) {
            try {
                Map<ClientPropertyDraw, List<ClientGroupObjectValue>> cells = singletonMap(model.getProperty(), singletonList(model.getColumnKey()));
                getForm().pasteMulticellValue(cells, table.get(0).get(0));
            } catch (IOException e) {
                Throwables.propagate(e);
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
