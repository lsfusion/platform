package lsfusion.client.form.property.panel.view;

import lsfusion.base.BaseUtils;
import lsfusion.base.Pair;
import lsfusion.base.SystemUtils;
import lsfusion.client.base.SwingUtils;
import lsfusion.client.form.controller.ClientFormController;
import lsfusion.client.form.design.view.ClientFormLayout;
import lsfusion.client.form.design.view.FlexPanel;
import lsfusion.client.form.design.view.flex.LinearCaptionContainer;
import lsfusion.client.form.design.view.widget.LabelWidget;
import lsfusion.client.form.design.view.widget.Widget;
import lsfusion.client.form.object.ClientGroupObjectValue;
import lsfusion.client.form.property.ClientPropertyDraw;
import lsfusion.client.form.property.cell.controller.dispatch.EditPropertyDispatcher;
import lsfusion.client.form.property.cell.controller.dispatch.SimpleChangePropertyDispatcher;
import lsfusion.client.tooltip.LSFTooltipManager;
import lsfusion.client.view.MainFrame;
import lsfusion.interop.base.view.FlexAlignment;
import lsfusion.interop.form.event.ValueEvent;
import lsfusion.interop.form.event.ValueEventListener;

import javax.swing.*;
import java.awt.*;

import static lsfusion.base.BaseUtils.nullEquals;
import static lsfusion.client.base.view.SwingDefaults.getDataPanelLabelMargin;

public class DataPanelView extends FlexPanel implements PanelView {
    private final LabelWidget label;

    private final DataPanelViewTable table;

    private final ClientPropertyDraw property;

    private final ClientGroupObjectValue columnKey;

    //чтобы не собрался GC
    @SuppressWarnings("FieldCanBeLocal")
    private final ValueEventListener valueEventListener;

    private final ClientFormController form;

    private final SimpleChangePropertyDispatcher simpleDispatcher;

    private final boolean vertical;
    private boolean tableFirst;

    private Boolean labelMarginRight = null;

    public DataPanelView(final ClientFormController iform, final ClientPropertyDraw property, ClientGroupObjectValue icolumnKey, LinearCaptionContainer captionContainer) {
        super(property.panelCaptionVertical, FlexAlignment.CENTER);

        form = iform;
        this.property = property;
        columnKey = icolumnKey;
        simpleDispatcher = form.getSimpleChangePropertyDispatcher();

        vertical = property.panelCaptionVertical;
        tableFirst = property.isPanelCaptionLast();

        //игнорируем key.readOnly, чтобы разрешить редактирование,
        //readOnly будет проверяться на уровне сервера и обрезаться возвратом пустого changeType
        table = new DataPanelViewTable(iform, columnKey, property);

        label = new LabelWidget();

        Integer captionWidth = property.getCaptionWidth();
        Integer captionHeight = property.getCaptionHeight();

        setLabelText(property.getChangeCaption());

        property.design.designHeader(label);
        if (property.focusable != null) {
            setFocusable(property.focusable);
        } else if (property.changeKey != null) {
            setFocusable(false);
        }

        if (!tableFirst && captionContainer == null) {
            add(label, property.getPanelCaptionAlignment(), 0.0, vertical ? captionHeight : captionWidth);
        }

        transparentResize = true;
        Pair<Integer, Integer> valueSizes;
        if(property.autoSize) {
            assert captionContainer == null;
            add(table, FlexAlignment.STRETCH, 1.0);
            valueSizes = setDynamic(table, true, property);
            if(property.isAutoDynamicHeight())
                valueSizes = null;
        } else {
            add(table, FlexAlignment.STRETCH, 1.0);
            valueSizes = setStatic(table, property);
        }

        if(captionContainer != null && valueSizes != null)
            captionContainer.put(label, new Pair<>(captionWidth, captionHeight), valueSizes, property.getPanelCaptionAlignment());

        if (tableFirst && captionContainer == null) {
            add(label, property.getPanelCaptionAlignment(), 0.0, vertical ? captionHeight : captionWidth);
        }

        if (!vertical)
            labelMarginRight = captionContainer != null || !tableFirst;

        setOpaque(false);
        setToolTip(property.getPropertyCaption());

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
                            if ((curLayout != null) && (curLayout.equals(focusLayout)) && SwingUtils.isRecursivelyVisible(table)) {
                                forceChangeValue(event.getValue());
                            }
                        }
                    });
                }
            };
            MainFrame.instance.eventBus.addListener(valueEventListener, property.eventID);
        } else {
            valueEventListener = null;
        }
    }

    // copy of ActionOrPropertyValue methods

    public static Pair<Integer, Integer> setStatic(Widget widget, ClientPropertyDraw property) {
        return setBaseSize(widget, true, property);
    }

    public static Pair<Integer, Integer> setDynamic(Widget widget, boolean hasBorder, ClientPropertyDraw property) {
        // leaving sizes -1 = auto ?? it won't work since it seems that table will return 0 preferred size
        return setBaseSize(widget, hasBorder, property);
    }

    public static Pair<Integer, Integer> setBaseSize(Widget widget, boolean hasBorder, ClientPropertyDraw property) {
        // if widget is wrapped into absolute positioned simple panel, we need to include paddings (since borderWidget doesn't include them)
        JComponent component = widget.getComponent();
        int valueWidth = hasBorder ? property.getValueWidthWithPadding(component) : property.getValueWidth(component);
        int valueHeight = hasBorder ? property.getValueHeightWithPadding(component) : property.getValueHeight(component);
        // about the last parameter oppositeAndFixed, here it's tricky since we don't know where this borderWidget will be added, however it seems that all current stacks assume that they are added with STRETCH alignment
        setBaseSize(widget, false, valueWidth); // STRETCH in upper call
        setBaseSize(widget, true, valueHeight);
        // it seems that there is one more margin pixel in desktop
        return new Pair<>(valueWidth + 2 + 2, valueHeight + 2 + 2); // should correspond to margins (now border : 1px which equals to 2px) in panelRendererValue style
    }

    @Override
    public int hashCode() {
        return property.getID() * 31 + columnKey.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof DataPanelView && ((DataPanelView) o).property.equals(property) && ((DataPanelView) o).columnKey.equals(columnKey);
    }

    public ClientPropertyDraw getProperty() {
        return property;
    }

    public ClientGroupObjectValue getColumnKey() {
        return columnKey;
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

    public DataPanelView getWidget() {
        return this;
    }

    public JComponent getFocusComponent() {
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

        // background is used in PropertyRenderer. so don't modify it according to color theme here.
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

    @Override
    public void setImage(Image image) {
        if (nullEquals(table.getImage(), image)) {
            return;
        }

        table.setImage(image);

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
        setLabelText(property.getChangeCaption(caption));
    }
    
    public void setLabelText(String text) {
        label.setText(text);

        if (!property.panelCaptionVertical) {
            if (BaseUtils.isRedundantString(text)) {
                label.setBorder(BorderFactory.createEmptyBorder());
            } else {
                label.setBorder(BorderFactory.createEmptyBorder(0,
                        tableFirst && labelMarginRight != null && !labelMarginRight ? getDataPanelLabelMargin() : 0,
                        0,
                        tableFirst && labelMarginRight != null && labelMarginRight ? 0 : getDataPanelLabelMargin()));
            }
        }
    }

    @Override
    public String toString() {
        return property.toString();
    }

    public void setToolTip(String caption) {
        LSFTooltipManager.initTooltip(label, property.getTooltipText(caption), property.path, property.creationPath);
    }

    public Icon getIcon() {
        throw new RuntimeException("not supported");
    }

    public void setIcon(Icon icon) {
        throw new RuntimeException("not supported");
    }

    @Override
    public EditPropertyDispatcher getEditPropertyDispatcher() {
        return table.getEditPropertyDispatcher();
    }
}