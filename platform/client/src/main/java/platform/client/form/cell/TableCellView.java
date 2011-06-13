package platform.client.form.cell;

import platform.base.BaseUtils;
import platform.client.form.ClientFormController;
import platform.client.logics.ClientGroupObjectValue;
import platform.client.logics.ClientPropertyDraw;
import platform.interop.ClassViewType;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.EventObject;

public class TableCellView extends JPanel implements CellView {

    private final JLabel label;
    private final CellTable table;

    private final ClientPropertyDraw key;
    private final ClientGroupObjectValue columnKey; // пока чисто для кэширования
    private final ClientFormController form;

    private Object highlight;
    private Color highlightColor;

    @Override
    public int hashCode() {
        return key.getID() * 31 + columnKey.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof TableCellView && ((TableCellView) o).key.equals(key) && ((TableCellView) o).columnKey.equals(columnKey);
    }

    public TableCellView(final ClientPropertyDraw key, ClientGroupObjectValue columnKey, ClientFormController form) {

        setOpaque(false);

        this.key = key;
        this.columnKey = columnKey;
        this.form = form;

        setLayout(new BoxLayout(this, (key.panelLabelAbove ? BoxLayout.Y_AXIS : BoxLayout.X_AXIS)));

        label = new JLabel();
        label.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 4));
        label.setText(key.getFullCaption());
        label.setAlignmentX(Component.CENTER_ALIGNMENT);

        key.design.designHeader(label);

        //игнорируем key.readOnly, чтобы разрешить редактирование,
        //readOnly будет проверяться на уровне сервера и обрезаться возвратом пустого changeType
        table = new CellTable(false) {

            protected boolean cellValueChanged(Object value, boolean aggValue) {
                return listener.cellValueChanged(value, aggValue);
            }

            public boolean isDataChanging() {
                return true;
            }

            public ClientPropertyDraw getProperty() {
                return TableCellView.this.key;
            }

            public boolean isCellHighlighted(int row, int column) {
                return TableCellView.this.highlight != null;
            }

            public Color getHighlightColor(int row, int column) {
                return highlightColor;
            }

            public ClientFormController getForm() {
                return TableCellView.this.form;
            }

            @Override
            public boolean clearText(int row, int column, EventObject e) {
                return key.clearText;
            }

        };

        table.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                if (e.isMetaDown() && e.getClickCount() >= 2) {
                    rightClick();
                }
            }
        });

        table.setBorder(BorderFactory.createLineBorder(Color.gray));

        table.keyChanged(key);

        setToolTip(key.caption);

        if (key.showTableFirst) {
            add(table);
            add(label);
        } else {
            add(label);
            add(table);
        }
    }

    public void rightClick() {
        startEditing(null);
    }

    @Override
    public boolean requestFocusInWindow() {
        return table.requestFocusInWindow();
    }

    @Override
    public void setFocusable(boolean focusable) {
        table.setFocusable(focusable);
        table.setCellSelectionEnabled(focusable);
    }

    public JComponent getComponent() {
        return this;
    }

    private CellViewListener listener;

    public void addListener(CellViewListener listener) {
        this.listener = listener;
    }

    public void setValue(Object ivalue) {
        table.setValue(ivalue);
    }

    public void setHighlight(Object highlight, Color highlightColor) {
        if (BaseUtils.nullEquals(this.highlight, highlight) &&
            BaseUtils.nullEquals(this.highlightColor, highlightColor)) {
            return;
        }

        this.highlight = highlight;
        this.highlightColor = highlightColor;

        revalidate();
        repaint();
    }

    public void startEditing(KeyEvent e) {

        if (table.isEditing()) return;

        table.editCellAt(0, 0);

        final Component tableEditor = table.getEditorComponent();
        if (tableEditor != null) {

            // устанавливаем следущий компонент фокуса на текущий
            if (tableEditor instanceof JComponent) {
                ((JComponent) tableEditor).setNextFocusableComponent(KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner());
            }

            tableEditor.requestFocusInWindow();
        }
    }

    public void setCaption(String caption) {
        label.setText(caption);
    }

    @Override
    public String toString() {
        return key.toString();
    }

    public void setToolTip(String caption) {
        label.setToolTipText(key.getTooltipText(caption));
    }

    public void changeViewType(ClassViewType type) {
        //пока тут ничего не делаем
    }
}
