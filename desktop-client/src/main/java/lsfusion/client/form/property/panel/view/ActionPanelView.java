package lsfusion.client.form.property.panel.view;

import lsfusion.base.BaseUtils;
import lsfusion.client.base.SwingUtils;
import lsfusion.client.base.view.ClientColorUtils;
import lsfusion.client.base.view.ClientImages;
import lsfusion.client.base.view.SwingDefaults;
import lsfusion.client.classes.ClientType;
import lsfusion.client.controller.remote.RmiQueue;
import lsfusion.client.form.controller.ClientFormController;
import lsfusion.client.form.object.ClientGroupObjectValue;
import lsfusion.client.form.property.ClientPropertyDraw;
import lsfusion.client.form.property.cell.classes.controller.DialogBasedPropertyEditor;
import lsfusion.client.form.property.cell.classes.controller.PropertyEditor;
import lsfusion.client.form.property.cell.controller.EditPropertyHandler;
import lsfusion.client.form.property.cell.controller.dispatch.EditPropertyDispatcher;
import lsfusion.client.form.property.table.view.ClientPropertyContextMenuPopup;
import lsfusion.interop.action.ServerResponse;
import lsfusion.interop.base.view.FlexAlignment;
import lsfusion.interop.base.view.FlexConstraints;
import lsfusion.interop.base.view.FlexLayout;
import lsfusion.interop.form.event.KeyInputEvent;
import lsfusion.interop.form.event.KeyStrokes;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

import static javax.swing.SwingUtilities.isRightMouseButton;
import static lsfusion.client.base.SwingUtils.overrideSize;
import static lsfusion.client.form.property.cell.EditBindingMap.getPropertyKeyPressActionSID;

public class ActionPanelView extends JButton implements PanelView, EditPropertyHandler {
    private final EditPropertyDispatcher editDispatcher;

    private final ClientPropertyDraw property;
    private final ClientGroupObjectValue columnKey;
    private final ClientFormController form;
    private Object value;
    private boolean readOnly;

    private JPanel panel;

    public ActionPanelView(final ClientPropertyDraw iproperty, final ClientGroupObjectValue icolumnKey, final ClientFormController iform) {
        super((String)null, ClientImages.getImage(iproperty.design.getImageHolder()));

        this.property = iproperty;
        this.columnKey = icolumnKey;
        this.form = iform;
        
        editDispatcher = new EditPropertyDispatcher(this, form.getDispatcherListener());

        setCaption(property.getPropertyCaption());
        setToolTip(property.getPropertyCaption());

        if (property.isReadOnly()) {
            setEnabled(false);
        }

        ClientColorUtils.designComponent(this, property.design);
        if (property.focusable != null) {
            setFocusable(property.focusable);
        } else if (property.changeKey != null) {
            setFocusable(false);
        }

        //we have 'ENTER' binding for tab action, so this 'ENTER' binding should have higher priority
        form.addBinding(new KeyInputEvent(KeyStrokes.getEnter()), new ClientFormController.Binding(property.groupObject, 0, eventObject -> eventObject.getSource() == ActionPanelView.this) {
            @Override
            public boolean pressed(InputEvent ke) {
                return form.commitCurrentEditing() && executePropertyEventAction(ServerResponse.CHANGE);
            }

            @Override
            public boolean showing() {
                return isVisible();
            }
        });

        //listen to mouse press and all other key press events
        addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                if (form.commitCurrentEditing()) {
                    RmiQueue.runAction(new Runnable() {
                        @Override
                        public void run() {
                            executePropertyEventAction(ServerResponse.CHANGE);
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
                } else {
                    final String actionSID = getPropertyKeyPressActionSID(e, property);
                    if (actionSID != null && form.commitCurrentEditing()) {
                        RmiQueue.runAction(new Runnable() {
                            @Override
                            public void run() {
                                executePropertyEventAction(actionSID);
                            }
                        });
                    }
                }
            }
        });

        panel = new JPanel(null);
        panel.setLayout(new FlexLayout(panel, true, FlexAlignment.CENTER));
        panel.add(this, new FlexConstraints(property.getAlignment(), 1));
        property.installMargins(panel);

        if(property.panelCaptionVertical) {
            setVerticalTextPosition(SwingConstants.BOTTOM);
            setHorizontalTextPosition(SwingConstants.CENTER);
        }
    }

    @Override
    public void updateUI() {
        super.updateUI();
        if (property != null) { // first call from constructor
            setIcon(ClientImages.getImage(property.design.getImageHolder()));
        }
    }

    private boolean executePropertyEventAction(String actionSID) {
        return editDispatcher.executePropertyEventAction(property, columnKey, actionSID, null);
    }

    private void showContextMenu(Point point) {
        if (form.commitCurrentEditing()) {
            new ClientPropertyContextMenuPopup().show(property, this, point, new ClientPropertyContextMenuPopup.ItemSelectionListener() {
                @Override
                public void onMenuItemSelected(final String actionSID) {
                    RmiQueue.runAction(new Runnable() {
                        @Override
                        public void run() {
                            executePropertyEventAction(actionSID);
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

    public JComponent getFocusComponent() {
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

    public boolean forceEdit() {
        doClick(0);
        return true;
    }

    public void setCaption(String caption) {
        caption = property.getEditCaption(caption);
        setText(caption);
    }

    public void setBackgroundColor(Color background) {
        setBackground(background == null ? SwingDefaults.getButtonBackground() : ClientColorUtils.getDisplayColor(background));
    }

    public void setForegroundColor(Color background) {
        // пока не highlight'им
    }

    @Override
    public void setImage(Image image) {
        setIcon(new ImageIcon(image));
    }

    public void setToolTip(String caption) {
        setToolTipText(property.getTooltipText(!BaseUtils.isRedundantString(property.toolTip) ? property.toolTip : caption));
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension baseSize = super.getPreferredSize();
        int propertyValueWidth = property.getValueWidth();
        if (propertyValueWidth == -1 && property.charWidth > 0) { // preferred width is perfect otherwise
            propertyValueWidth = property.getValueWidth(this);
        }
        int borderCorrection = SwingDefaults.getButtonBorderWidth() * 2;
        int overrideWidth = propertyValueWidth > 0 ? propertyValueWidth + borderCorrection : baseSize.width;
        return overrideSize(baseSize, new Dimension(overrideWidth, property.getValueHeight(this) + borderCorrection));  // тут видимо потому что caption'а нет
    }

    @Override
    public boolean requestValue(ClientType valueType, Object oldValue) {
        PropertyEditor propertyEditor = valueType.getChangeEditorComponent(ActionPanelView.this, form, property, null);

        assert propertyEditor != null;

        propertyEditor.getComponent(SwingUtils.computeAbsoluteLocation(ActionPanelView.this), getBounds(), null);

        //для всего, кроме диалогов выдаём ошибку.
        //для диалогов - сначала спрашиваем, изменилось ли значение
        if (propertyEditor instanceof DialogBasedPropertyEditor) {
            if (((DialogBasedPropertyEditor) propertyEditor).valueChanged()) {
                editDispatcher.commitValue(propertyEditor.getCellEditorValue());
            } else {
                editDispatcher.cancelEdit();
            }
        } else {
            editDispatcher.cancelEdit();
            throw new RuntimeException("INPUT in action is allowed only for dialog based types (file, color, etc.)");
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
    public Object getEditValue() {
        return true;
    }

    @Override
    public EditPropertyDispatcher getEditPropertyDispatcher() {
        return editDispatcher;
    }
}
