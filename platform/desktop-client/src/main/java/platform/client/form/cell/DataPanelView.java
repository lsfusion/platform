package platform.client.form.cell;

import platform.client.Main;
import platform.client.SwingUtils;
import platform.client.form.ClientFormController;
import platform.client.form.ClientFormLayout;
import platform.client.form.dispatch.SimpleChangePropertyDispatcher;
import platform.client.logics.ClientGroupObjectValue;
import platform.client.logics.ClientPropertyDraw;
import platform.interop.event.ValueEvent;
import platform.interop.event.ValueEventListener;

import javax.swing.*;
import java.awt.*;

import static platform.base.BaseUtils.nullEquals;
import static platform.client.SwingUtils.getNewBoundsIfNotAlmostEquals;

public class DataPanelView extends JPanel implements PanelView {
    private final JLabel label;

    private final DataPanelViewTable table;

    private final ClientPropertyDraw property;
    private final ClientGroupObjectValue columnKey;

    private ValueEventListener valueEventListener;
    private final ClientFormController form;

    private final SimpleChangePropertyDispatcher simpleDispatcher;

    public DataPanelView(final ClientFormController iform, final ClientPropertyDraw iproperty, ClientGroupObjectValue icolumnKey) {
        setOpaque(false);

        form = iform;
        property = iproperty;
        columnKey = icolumnKey;
        simpleDispatcher = form.getSimpleChangePropertyDispatcher();

        setLayout(new BoxLayout(this, (property.panelLabelAbove ? BoxLayout.Y_AXIS : BoxLayout.X_AXIS)));

        label = new JLabel();
        label.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 4));
        label.setText(property.getEditCaption());
        label.setAlignmentX(Component.CENTER_ALIGNMENT);

        property.design.designHeader(label);

        //игнорируем key.readOnly, чтобы разрешить редактирование,
        //readOnly будет проверяться на уровне сервера и обрезаться возвратом пустого changeType
        table = new DataPanelViewTable(iform, columnKey, property);

        setToolTip(property.getCaption());

        if (property.showTableFirst) {
            add(table);
            add(label);
        } else {
            add(label);
            add(table);
        }

        if (property.eventID != null) {
            valueEventListener = new ValueEventListener() {
                @Override
                public void actionPerfomed(final ValueEvent event) {
                    // может вызываться не из EventDispatchingThread
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            ClientFormLayout focusLayout = SwingUtils.getClientFormlayout(KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner());
                            ClientFormLayout curLayout = SwingUtils.getClientFormlayout(table);
                            if ((curLayout != null) && (curLayout.equals(focusLayout))) {
                                forceChangeValue(event.getValue());
                            }
                        }
                    });
                }
            };
            Main.eventBus.addListener(valueEventListener, property.eventID);
        }
    }

    @Override
    public int hashCode() {
        return property.getID() * 31 + columnKey.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof DataPanelView && ((DataPanelView) o).property.equals(property) && ((DataPanelView) o).columnKey.equals(columnKey);
    }

    protected void forceChangeValue(Object value) {
        if (form.commitCurrentEditing()) {
            simpleDispatcher.changeProperty(value, table.getProperty(), columnKey);
        }
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

    public void setValue(Object ivalue) {
        table.setValue(ivalue);
    }

    public void setReadOnly(boolean readOnly) {
        table.setReadOnly(readOnly);
    }

    public void setBackgroundColor(Color background) {
        if (nullEquals(table.getBackgroundColor(), background)) {
            return;
        }

        table.setBackgroundColor(background);

        revalidate();
        repaint();
    }

    public void setForegroundColor(Color foreground) {
        if (nullEquals(table.getForegroundColor(), foreground)) {
            return;
        }

        table.setForegroundColor(foreground);

        revalidate();
        repaint();
    }

    @SuppressWarnings("deprecation")
    public void forceEdit() {
        if (!table.isEditing()) {
            table.editCellAt(0, 0, null);
            Component editorComponent = table.getEditorComponent();
            if (editorComponent instanceof JComponent) {
                ((JComponent) editorComponent).setNextFocusableComponent(KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner());
            }
        }
    }

    public void setCaption(String caption) {
        label.setText(property.getEditCaption(caption));
    }

    @Override
    public String toString() {
        return property.toString();
    }

    public void setToolTip(String caption) {
        label.setToolTipText(property.getTooltipText(caption));
    }

    public Icon getIcon() {
        throw new RuntimeException("not supported");
    }

    public void setIcon(Icon icon) {
        throw new RuntimeException("not supported");
    }

   //Чтобы лэйаут не прыгал игнорируем мелкие изменения координат
    @Override
    public void setBounds(int x, int y, int width, int height) {
        Rectangle newBounds = getNewBoundsIfNotAlmostEquals(this, x, y, width, height);
        super.setBounds(newBounds.x, newBounds.y, newBounds.width,  newBounds.height);
    }
}