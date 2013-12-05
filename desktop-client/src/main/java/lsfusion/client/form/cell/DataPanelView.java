package lsfusion.client.form.cell;

import lsfusion.base.SystemUtils;
import lsfusion.client.CaptureKeyEventsDispatcher;
import lsfusion.client.Main;
import lsfusion.client.SwingUtils;
import lsfusion.client.form.ClientFormController;
import lsfusion.client.form.dispatch.SimpleChangePropertyDispatcher;
import lsfusion.client.form.layout.ClientFormLayout;
import lsfusion.client.logics.ClientGroupObjectValue;
import lsfusion.client.logics.ClientPropertyDraw;
import lsfusion.interop.event.ValueEvent;
import lsfusion.interop.event.ValueEventListener;
import lsfusion.interop.form.layout.CachableLayout;
import lsfusion.interop.form.layout.FlexAlignment;
import lsfusion.interop.form.layout.HasLabels;

import javax.swing.*;
import java.awt.*;

import static java.lang.Math.max;
import static lsfusion.base.BaseUtils.nullEquals;

public class DataPanelView extends JPanel implements PanelView, HasLabels {
    private final JLabel label;

    private final DataPanelViewTable table;

    private final ClientPropertyDraw property;

    private final ClientGroupObjectValue columnKey;

    //чтобы не собрался GC
    @SuppressWarnings("FieldCanBeLocal")
    private final ValueEventListener valueEventListener;

    private final ClientFormController form;

    private final SimpleChangePropertyDispatcher simpleDispatcher;

    private int labelWidth = -1;

    public DataPanelView(final ClientFormController iform, final ClientPropertyDraw iproperty, ClientGroupObjectValue icolumnKey) {
        super(null);

        form = iform;
        property = iproperty;
        columnKey = icolumnKey;
        simpleDispatcher = form.getSimpleChangePropertyDispatcher();

        setLayout(new DataPanelLayout());

        //игнорируем key.readOnly, чтобы разрешить редактирование,
        //readOnly будет проверяться на уровне сервера и обрезаться возвратом пустого changeType
        table = new DataPanelViewTable(iform, columnKey, property);

        label = new JLabel(property.getEditCaption());
        if (property.panelLabelAbove) {
            label.setHorizontalAlignment(SwingConstants.CENTER);
        }

        property.design.designHeader(label);
        if (property.focusable != null) {
            setFocusable(property.focusable);
        } else if (property.editKey != null) {
            setFocusable(false);
        }

        add(label);
        add(table);

        setOpaque(false);
        setToolTip(property.getCaption());

        property.installMargins(this);

        if (property.eventID != null) {
            valueEventListener = new ValueEventListener() {
                @Override
                public void actionPerfomed(final ValueEvent event) {
                    // может вызываться не из EventDispatchingThread
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            ClientFormLayout focusLayout = SwingUtils.getClientFormLayout(KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner());
                            ClientFormLayout curLayout = SwingUtils.getClientFormLayout(table);
                            if ((curLayout != null) && (curLayout.equals(focusLayout))) {
                                forceChangeValue(event.getValue());
                            }
                        }
                    });
                }
            };
            Main.eventBus.addListener(valueEventListener, property.eventID);
        } else {
            valueEventListener = null;
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
    public boolean forceEdit() {
        if (table.isShowing()) {
            if (!table.isEditing()) {
                table.editCellAt(0, 0, null);
                Component editor = table.getEditorComponent();
                if (editor instanceof JComponent) {
                    ((JComponent) editor).setNextFocusableComponent(KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner());
                    if (editor instanceof JTextField) {
                        editor.requestFocusInWindow();
                        CaptureKeyEventsDispatcher.get().setCapture(editor);
                        //даём java немного времени для дочитывания клавиш из буфера клавиатуры, иначе не работает сканер
                        SystemUtils.sleep(100);
                    }
                }
            }
            return true;
        }
        return false;
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

    @Override
    public boolean hasLabels() {
        //todo: пока убираем выравнивание заголовков
//        return true;
        return false;
    }

    @Override
    public int getLabelsPreferredWidth() {
        return property.panelLabelAbove ? 0 : label.getPreferredSize().width;
    }

    @Override
    public void setLabelsWidth(int width) {
        labelWidth = width;
    }

    private class DataPanelLayout extends CachableLayout {

        public DataPanelLayout() {
            super(DataPanelView.this, false);
        }

        @Override
        protected Dimension layoutSize(Container parent, ComponentSizeGetter sizeGetter) {
            Dimension labelSize = sizeGetter.get(label);
            Dimension tableSize = sizeGetter.get(table);
            int width;
            int height;
            if (property.panelLabelAbove) {
                width = max(labelSize.width, tableSize.width);
                height = limitedSum(labelSize.height, tableSize.height);
            } else {
                width = limitedSum(8, labelSize.width, tableSize.width);
                height = max(labelSize.height, tableSize.height);
            }

            return new Dimension(width, height);
        }

        @Override
        public void layoutContainer(Container parent) {
            boolean vertical = property.panelLabelAbove;
            boolean tableFirst = property.showTableFirst;

            Insets in = parent.getInsets();

            int width = parent.getWidth() - in.left - in.right;
            int height = parent.getHeight() - in.top - in.bottom;

            Dimension labelPref = label.getPreferredSize();
            Dimension tablePref = table.getPreferredSize();

            if (!tableFirst && labelWidth != -1) {
                labelPref.width = labelWidth;
            }

            int tableSpace = width;
            int tableLeft = in.left;
            int tableTop = in.top;
            int tableHeight = height;
            if (vertical) {
                tableHeight -= labelPref.height;
                if (!tableFirst) {
                    tableTop += labelPref.height;
                }
            } else {
                //horizontal
                tableSpace = max(0, tableSpace - 4 - labelPref.width - 4);
                if (!tableFirst) {
                    tableLeft += 4 + labelPref.width + 4;
                }
            }

            int tableWidth = tableSpace;
            if (property.alignment != FlexAlignment.STRETCH) {
                tableWidth = Math.min(tableSpace, tablePref.width);
                if (property.alignment == FlexAlignment.TRAILING) {
                    tableLeft += tableSpace - tableWidth;
                } else if (property.alignment == FlexAlignment.CENTER) {
                    tableLeft += (tableSpace - tableWidth)/2;
                }
            }

            int labelWidth = vertical ? width : labelPref.width;
            int labelHeight = labelPref.height;
            int labelLeft = in.left;
            int labelTop = in.top;

            if (vertical) {
                if (tableFirst) {
                    labelTop += tableHeight;
                }
            } else {
                labelTop += max(0, height - labelHeight)/2;
                labelLeft += tableFirst ? 4 + tableSpace + 4 : 4;
            }

            label.setBounds(labelLeft, labelTop, labelWidth, labelHeight);
            table.setBounds(tableLeft, tableTop, tableWidth, tableHeight);
        }
    }
}