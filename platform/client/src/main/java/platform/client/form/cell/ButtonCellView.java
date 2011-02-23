package platform.client.form.cell;

import platform.base.BaseUtils;
import platform.client.ClientButton;
import platform.client.SwingUtils;
import platform.client.form.ClientFormController;
import platform.client.form.PropertyEditorComponent;
import platform.client.logics.ClientGroupObjectValue;
import platform.client.logics.ClientPropertyDraw;
import platform.interop.ClassViewType;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

public class ButtonCellView extends ClientButton implements CellView {
    private ClientPropertyDraw key;
    private ClientGroupObjectValue columnKey; // чисто для кэширования
    public boolean toToolbar;
    private String caption;

    @Override
    public int hashCode() {
        return key.getID() * 31 + columnKey.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof ButtonCellView && ((ButtonCellView) o).key.equals(key) && ((ButtonCellView) o).columnKey.equals(columnKey);
    }

    public ButtonCellView(final ClientPropertyDraw key, ClientGroupObjectValue columnKey, final ClientFormController form) {
        super(key.getFullCaption());
        this.key = key;
        this.columnKey = columnKey;

        setToolTip(key.caption);

        if (key.readOnly) {
            setEnabled(false);
        }

        key.design.designComponent(this);

        addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent ae) {

                try {

                    PropertyEditorComponent editor = key.getEditorComponent(form, null);
                    if (editor != null) {
                        editor.getComponent(SwingUtils.computeAbsoluteLocation(ButtonCellView.this), getBounds(), null);
                        if (editor.valueChanged())
                            listener.cellValueChanged(editor.getCellEditorValue());
                    }

                } catch (Exception e) {
                    throw new RuntimeException("Ошибка при выполнении действия", e);
                }
            }
        });

        setDefaultSizes();
    }

    public JComponent getComponent() {
        return this;
    }

    private CellViewListener listener;

    public void addListener(CellViewListener listener) {
        this.listener = listener;
    }

    public void setValue(Object value) {
        setEnabled(value != null);
    }

    public void startEditing(KeyEvent e) {
        doClick(20);
    }

    public void setCaption(String caption) {
        this.caption = caption;
        if (!toToolbar || getIcon() == null) {
            setText(caption);
        } else {
            setText("");
        }
    }

    public void setHighlight(Object highlight, Color highlightColor) {
        // пока не highlight'им
    }

    public void setToolTip(String caption) {
        String toolTip = !BaseUtils.isRedundantString(key.toolTip) ? key.toolTip : caption;
        toolTip += " (sID: " + key.getSID() + ")";
        if (key.editKey != null) {
            toolTip += "(" + SwingUtils.getKeyStrokeCaption(key.editKey) + ")";
        }
        setToolTipText(toolTip);
    }

    public void changeViewType(ClassViewType type) {
        toToolbar = (type == ClassViewType.GRID);
        setCaption(caption);
        if (toToolbar && key.design.image != null) {
            setPreferredSize(new Dimension(key.design.image.getIconWidth() + 2, key.design.image.getIconHeight() + 2));
            setMaximumSize(new Dimension(key.design.image.getIconWidth() + 2, key.design.image.getIconHeight() + 2));
        } else {
            setPreferredSize(null);
            setDefaultSizes();
        }
    }

    private void setDefaultSizes() {
        setMinimumSize(new Dimension(0, 18));
        setMaximumSize(new Dimension(32767, 18));
    }
}
