package lsfusion.client.form.cell;

import lsfusion.base.BaseUtils;
import lsfusion.client.Main;
import lsfusion.client.SwingUtils;
import lsfusion.client.form.ClientFormController;
import lsfusion.client.form.ClientPropertyContextMenuPopup;
import lsfusion.client.form.EditPropertyHandler;
import lsfusion.client.form.PropertyEditor;
import lsfusion.client.form.dispatch.EditPropertyDispatcher;
import lsfusion.client.form.editor.DialogBasedPropertyEditor;
import lsfusion.client.form.queries.ToolbarGridButton;
import lsfusion.client.logics.ClientGroupObjectValue;
import lsfusion.client.logics.ClientPropertyDraw;
import lsfusion.client.logics.classes.ClientType;
import lsfusion.interop.form.ServerResponse;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

import static javax.swing.SwingUtilities.isRightMouseButton;

public class ActionPanelView extends JButton implements PanelView, EditPropertyHandler {
    private final EditPropertyDispatcher editDispatcher = new EditPropertyDispatcher(this);
    private final ClientPropertyContextMenuPopup menu = new ClientPropertyContextMenuPopup();

    private Color defaultBackground;

    private final ClientPropertyDraw property;
    private final ClientGroupObjectValue columnKey;
    private final ClientFormController form;
    public boolean toToolbar;
    private Object value;
    private boolean readOnly;

    public ActionPanelView(final ClientPropertyDraw iproperty, final ClientGroupObjectValue icolumnKey, final ClientFormController iform) {
        super(iproperty.getEditCaption());

        this.defaultBackground = getBackground();
        this.property = iproperty;
        this.columnKey = icolumnKey;
        this.form = iform;

        setToolTip(property.getCaption());

        if (property.isReadOnly()) {
            setEnabled(false);
        }

        property.design.designComponent(this);

        addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                if (form.commitCurrentEditing()) {
                    editDispatcher.executePropertyEditAction(property, columnKey, ServerResponse.CHANGE, null);
                }
            }
        });
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (isRightMouseButton(e) ) {
                    showContextMenu(e.getPoint());
                }
            }
        });

        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_CONTEXT_MENU) {
                    Rectangle rect = getBounds();
                    Point point = new Point(rect.x, rect.y + rect.height - 1);
                    showContextMenu(point);
                }
            }
        });

        setDefaultSizes();
    }

    private void showContextMenu(Point point) {
        if (form.commitCurrentEditing()) {
            menu.show(property, this, point, new ClientPropertyContextMenuPopup.ItemSelectionListener() {
                @Override
                public void onMenuItemSelected(String actionSID) {
                    editDispatcher.executePropertyEditAction(property, columnKey, actionSID, null);
                }
            });
        }
    }

    @Override
    public int hashCode() {
        return property.getID() * 31 + columnKey.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof ActionPanelView && ((ActionPanelView) o).property.equals(property) && ((ActionPanelView) o).columnKey.equals(columnKey);
    }

    @Override
    public String toString() {
        return property.toString();
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
        setText(property.getEditCaption(caption));
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
        String toolTip = !BaseUtils.isRedundantString(property.toolTip) ? property.toolTip : caption;
        if (Main.configurationAccessAllowed) {
            toolTip += " (sID: " + property.getSID() + ")";
        }
        if (property.editKey != null) {
            toolTip += " (" + SwingUtils.getKeyStrokeCaption(property.editKey) + ")";
        }
        setToolTipText(toolTip);
    }

    private void setDefaultSizes() {
        int height = toToolbar ? ToolbarGridButton.DEFAULT_SIZE.height : property.getPreferredHeight(this);

        int minimumWidth = property.minimumSize != null ? property.getMinimumWidth(this) : 0;
        int maximumWidth = property.maximumSize != null ? property.getMaximumWidth(this) : 32767;

        setMinimumSize(new Dimension(minimumWidth, height));
        setMaximumSize(new Dimension(maximumWidth, height));
    }

    @Override
    public boolean requestValue(ClientType valueType, Object oldValue) {
        PropertyEditor propertyEditor = valueType.getChangeEditorComponent(ActionPanelView.this, form, property, null);

        assert propertyEditor != null;

        propertyEditor.getComponent(SwingUtils.computeAbsoluteLocation(ActionPanelView.this), getBounds(), null);

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
