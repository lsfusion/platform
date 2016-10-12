package lsfusion.client.form.cell;

import lsfusion.base.BaseUtils;
import lsfusion.client.SwingUtils;
import lsfusion.client.form.*;
import lsfusion.client.form.dispatch.EditPropertyDispatcher;
import lsfusion.client.form.editor.DialogBasedPropertyEditor;
import lsfusion.client.logics.ClientGroupObjectValue;
import lsfusion.client.logics.ClientPropertyDraw;
import lsfusion.client.logics.classes.ClientType;
import lsfusion.interop.form.ServerResponse;
import lsfusion.interop.form.layout.FlexConstraints;
import lsfusion.interop.form.layout.FlexLayout;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

import static javax.swing.SwingUtilities.isRightMouseButton;
import static lsfusion.client.SwingUtils.overrideSize;

public class ActionPanelView extends JButton implements PanelView, EditPropertyHandler {
    private final EditPropertyDispatcher editDispatcher = new EditPropertyDispatcher(this);
    private final ClientPropertyContextMenuPopup menu = new ClientPropertyContextMenuPopup();

    private Color defaultBackground;

    private final ClientPropertyDraw property;
    private final ClientGroupObjectValue columnKey;
    private final ClientFormController form;
    private Object value;
    private boolean readOnly;

    private JPanel panel;

    public ActionPanelView(final ClientPropertyDraw iproperty, final ClientGroupObjectValue icolumnKey, final ClientFormController iform) {
        super((String)null);

        this.defaultBackground = getBackground();
        this.property = iproperty;
        this.columnKey = icolumnKey;
        this.form = iform;

        setCaption(property.getCaption());
        setToolTip(property.getCaption());

        if (property.isReadOnly()) {
            setEnabled(false);
        }

        property.design.designComponent(this);
        if (property.focusable != null) {
            setFocusable(property.focusable);
        } else if (property.editKey != null) {
            setFocusable(false);
        }

        addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                if (form.commitCurrentEditing()) {
                    RmiQueue.runAction(new Runnable() {
                        @Override
                        public void run() {
                            editDispatcher.executePropertyEditAction(property, columnKey, ServerResponse.CHANGE, null, null);
                        }
                    });
                }
            }
        });
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (isRightMouseButton(e)) {
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

        panel = new JPanel(null);
        panel.setLayout(new FlexLayout(panel, true));
        panel.add(this, new FlexConstraints(property.alignment, 1));
        property.installMargins(panel);
    }

    private void showContextMenu(Point point) {
        if (form.commitCurrentEditing()) {
            menu.show(property, this, point, new ClientPropertyContextMenuPopup.ItemSelectionListener() {
                @Override
                public void onMenuItemSelected(final String actionSID) {
                    RmiQueue.runAction(new Runnable() {
                        @Override
                        public void run() {
                            editDispatcher.executePropertyEditAction(property, columnKey, actionSID, null, null);
                        }
                    });
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
        return panel;
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

    public boolean forceEdit() {
        if (isShowing()) {
            doClick(20);
            return true;
        }
        return false;
    }

    public void setCaption(String caption) {
        caption = property.getEditCaption(caption);
        if (BaseUtils.isRedundantString(caption)) {
            setMargin(new Insets(2, 2, 2, 2));
        } else {
            setMargin(new Insets(2, 14, 2, 14));
        }
        setText(caption);
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

    @Override
    public void setLabelWidth(int width) {
        //ignore
    }

    public void setToolTip(String caption) {
        setToolTipText(property.getTooltipText(!BaseUtils.isRedundantString(property.toolTip) ? property.toolTip : caption, true));
    }

    @Override
    public Dimension getMinimumSize() {
        return overrideSize(super.getMinimumSize(), property.minimumSize);
    }

    @Override
    public Dimension getMaximumSize() {
        return overrideSize(super.getMaximumSize(), property.maximumSize);
    }

    @Override
    public Dimension getPreferredSize() {
        return overrideSize(super.getPreferredSize(), property.preferredSize);
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

    @Override
    protected void processKeyEvent(final KeyEvent e) {
        SwingUtils.getAroundTooltipListener(this, e, new Runnable() {
            @Override
            public void run() {
                ActionPanelView.super.processKeyEvent(e);
            }
        });
    }

    @Override
    public EditPropertyDispatcher getEditPropertyDispatcher() {
        return editDispatcher;
    }
}
