package platform.client.form.cell;

import platform.base.BaseUtils;
import platform.client.SwingUtils;
import platform.client.form.ClientFormController;
import platform.client.form.EditPropertyHandler;
import platform.client.form.PropertyEditor;
import platform.client.form.dispatch.EditPropertyDispatcher;
import platform.client.form.editor.DialogBasedPropertyEditor;
import platform.client.form.queries.ToolbarGridButton;
import platform.client.logics.ClientGroupObjectValue;
import platform.client.logics.ClientPropertyDraw;
import platform.client.logics.classes.ClientType;
import platform.interop.form.ServerResponse;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;

public class ActionPanelViewContextMenuHandler extends JButton implements PanelView, EditPropertyHandler {
    private final EditPropertyDispatcher editDispatcher = new EditPropertyDispatcher(this);

    private Color defaultBackground;

    private final ClientPropertyDraw key;
    private final ClientGroupObjectValue columnKey;
    private final ClientFormController form;
    public boolean toToolbar;
    private Object value;
    private boolean readOnly;

    public ActionPanelViewContextMenuHandler(final ClientPropertyDraw ikey, final ClientGroupObjectValue icolumnKey, final ClientFormController iform) {
        super(ikey.getEditCaption());

        this.defaultBackground = getBackground();
        this.key = ikey;
        this.columnKey = icolumnKey;
        this.form = iform;

        setToolTip(key.getCaption());

        if (key.isReadOnly()) {
            setEnabled(false);
        }

        key.design.designComponent(this);

        addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                if (form.commitCurrentEditing()) {
                    editDispatcher.executePropertyEditAction(key, columnKey, ServerResponse.CHANGE, null);
                }
            }
        });
        addMouseListener(new MouseAdapter() {
        });

        setDefaultSizes();
    }

    @Override
    public int hashCode() {
        return key.getID() * 31 + columnKey.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof ActionPanelViewContextMenuHandler && ((ActionPanelViewContextMenuHandler) o).key.equals(key) && ((ActionPanelViewContextMenuHandler) o).columnKey.equals(columnKey);
    }

    @Override
    public String toString() {
        return key.toString();
    }

    public JComponent getComponent() {
        return this;
    }

    public void setValue(Object value) {
        this.value = value;
        updateButton();
    }

    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
        updateButton();
    }

    private void updateButton() {
        setEnabled(value != null && !readOnly);
    }

    public void forceEdit() {
        if (isShowing()) {
            doClick(20);
        }
    }

    public void setCaption(String caption) {
        setText(key.getEditCaption(caption));
    }

    public void setBackgroundColor(Color background) {
        setBackground(background == null ? defaultBackground : background);
    }

    @Override
    public void setBackground(Color bg) {
        super.setBackground(bg);
    }

    public void setForegroundColor(Color background) {
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

    private void setDefaultSizes() {
        int height = toToolbar ? ToolbarGridButton.DEFAULT_SIZE.height : key.getPreferredHeight(this);

        int minimumWidth = key.minimumSize != null ? key.getMinimumWidth(this) : 0;
        int maximumWidth = key.maximumSize != null ? key.getMaximumWidth(this) : 32767;

        setMinimumSize(new Dimension(minimumWidth, height));
        setMaximumSize(new Dimension(maximumWidth, height));
    }

    @Override
    public boolean requestValue(ClientType valueType, Object oldValue) {
        PropertyEditor propertyEditor = valueType.getChangeEditorComponent(ActionPanelViewContextMenuHandler.this, form, key, null);

        assert propertyEditor != null;

        propertyEditor.getComponent(SwingUtils.computeAbsoluteLocation(ActionPanelViewContextMenuHandler.this), getBounds(), null);

        //для всего, кроме диалогов ничего не записываем..
        //для диалогов - сначала спрашиваем, изменилось ли значение
        if (propertyEditor instanceof DialogBasedPropertyEditor && ((DialogBasedPropertyEditor) propertyEditor).valueChanged()) {
            editDispatcher.commitValue(propertyEditor.getCellEditorValue());
        } else {
            editDispatcher.cancelEdit();
        }

        return true;
    }

    @Override
    public ClientFormController getForm() {
        return form;
    }

    public void updateEditValue(Object value) {
        // по идее не может быть
    }
}
