package lsfusion.gwt.form.shared.view.panel;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.EventTarget;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.*;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.base.client.Dimension;
import lsfusion.gwt.base.client.ui.FlexPanel;
import lsfusion.gwt.base.client.ui.GFlexAlignment;
import lsfusion.gwt.base.client.ui.HasPreferredSize;
import lsfusion.gwt.base.client.ui.ResizableComplexPanel;
import lsfusion.gwt.base.shared.GwtSharedUtils;
import lsfusion.gwt.form.client.form.ui.GFormController;
import lsfusion.gwt.form.client.form.ui.GPanelController;
import lsfusion.gwt.form.client.form.ui.GSinglePropertyTable;
import lsfusion.gwt.form.client.form.ui.TooltipManager;
import lsfusion.gwt.form.client.form.ui.layout.GFormLayoutImpl;
import lsfusion.gwt.form.shared.view.GEditBindingMap;
import lsfusion.gwt.form.shared.view.GKeyStroke;
import lsfusion.gwt.form.shared.view.GPropertyDraw;
import lsfusion.gwt.form.shared.view.changes.GGroupObjectValue;
import lsfusion.gwt.form.shared.view.changes.dto.ColorDTO;

import static lsfusion.gwt.form.client.HotkeyManager.Binding;

public class DataPanelRenderer implements PanelRenderer {
    private static final GFormLayoutImpl layoutImpl = GFormLayoutImpl.get();

    public final GPropertyDraw property;

    public final FlexPanel panel;
    public final ResizableComplexPanel gridPanel;
    private final SimplePanel focusPanel;

    public final Label label;
    public final GSinglePropertyTable valueTable;

    private String caption;
    private String tooltip;

    private EventTarget focusTargetAfterEdit;

    public DataPanelRenderer(GFormController form, GPropertyDraw iproperty, GGroupObjectValue columnKey) {
        this.property = iproperty;

        boolean vertical = property.panelLabelAbove;

        tooltip = property.getTooltipText(property.getCaptionOrEmpty());

        label = new Label(caption = property.getEditCaption());
        if (!vertical) {
            label.getElement().getStyle().setMarginRight(4, Style.Unit.PX);
        }

        label.addMouseOverHandler(new MouseOverHandler() {
            @Override
            public void onMouseOver(MouseOverEvent event) {
                TooltipManager.get().showTooltip(event.getClientX(), event.getClientY(), tooltip);
            }
        });
        label.addMouseOutHandler(new MouseOutHandler() {
            @Override
            public void onMouseOut(MouseOutEvent event) {
                TooltipManager.get().hideTooltip();
            }
        });
        label.addMouseMoveHandler(new MouseMoveHandler() {
            @Override
            public void onMouseMove(MouseMoveEvent event) {
                TooltipManager.get().updateMousePosition(event.getClientX(), event.getClientY());
            }
        });

        if (property.headerFont != null) {
            property.headerFont.apply(label.getElement().getStyle());
        }

        valueTable = new ValueTable(form, columnKey);

        gridPanel = new GridPanel();
        gridPanel.addStyleName("dataPanelRendererGridPanel");

        if (property.focusable) {
            focusPanel = new SimplePanel();
            focusPanel.addStyleName("dataPanelRendererFocusPanel");
            focusPanel.setVisible(false);
            gridPanel.add(focusPanel);
        } else {
            valueTable.setTableFocusable(false);
            focusPanel = null;
        }

        gridPanel.add(valueTable);

        valueTable.setSize("100%", "100%");

        panel = new FlexPanel(vertical);
        panel.addStyleName("dataPanelRendererPanel");

        // Рамка фокуса сделана как абсолютно позиционированный div с border-width: 2px,
        // который выходит за пределы панели, поэтому убираем overflow: hidden
        panel.getElement().getStyle().clearOverflow();

        property.installMargins(panel);

        panel.add(label, GFlexAlignment.CENTER);
        panel.add(gridPanel, vertical ? GFlexAlignment.STRETCH : GFlexAlignment.CENTER, 1, "auto");

        String preferredHeight = property.getPreferredHeight();
        String preferredWidth = property.getPreferredWidth();

        gridPanel.setHeight(preferredHeight);
        if (!vertical) {
            gridPanel.setWidth(preferredWidth);
        }

        gridPanel.getElement().getStyle().setProperty("minHeight", preferredHeight);
        gridPanel.getElement().getStyle().setProperty("minWidth", preferredWidth);
        gridPanel.getElement().getStyle().setProperty("maxHeight", preferredHeight);
        gridPanel.getElement().getStyle().setProperty("maxWidth", preferredWidth);

        valueTable.getElement().setPropertyObject("groupObject", property.groupObject);
        if (property.editKey != null) {
            form.addHotkeyBinding(property.groupObject, property.editKey, new Binding() {
                @Override
                public boolean onKeyPress(NativeEvent event, GKeyStroke key) {
                    focusTargetAfterEdit = event.getEventTarget();
                    valueTable.editCellAt(0, 0, GEditBindingMap.CHANGE);
                    return true;
                }
            });
        }
    }

    @Override
    public void setupLayout(GPanelController.GPropertyController controller) {
        layoutImpl.setupDataPanelRenderer(controller, this);
    }

    @Override
    public Widget getComponent() {
        return panel;
    }

    @Override
    public void setValue(Object value) {
        valueTable.setValue(value);
    }

    @Override
    public void setReadOnly(boolean readOnly) {
        valueTable.setReadOnly(readOnly);
    }

    @Override
    public void setCaption(String caption) {
        if (!GwtSharedUtils.nullEquals(this.caption, caption)) {
            this.caption = caption;
            label.setText(property.getEditCaption(caption));
            tooltip = property.getTooltipText(caption);
        }
    }

    @Override
    public void setDefaultIcon() {
    }

    @Override
    public void setIcon(String iconPath) {
    }

    @Override
    public void updateCellBackgroundValue(Object value) {
        valueTable.setBackground((ColorDTO) value);
    }

    @Override
    public void updateCellForegroundValue(Object value) {
        valueTable.setForeground((ColorDTO) value);
    }

    @Override
    public void focus() {
        valueTable.setFocus(true);
    }

    private class GridPanel extends ResizableComplexPanel implements HasPreferredSize {
        @Override
        public Dimension getPreferredSize() {
            //+2 for border
            return new Dimension(property.getPreferredPixelWidth() + 2 + property.getHorizontalMargin(),
                                 property.getPreferredPixelHeight() + 2 + property.getVerticalMargin());
        }
    }

    private class ValueTable extends GSinglePropertyTable {
        public ValueTable(GFormController form, GGroupObjectValue columnKey) {
            super(form, DataPanelRenderer.this.property, columnKey);
        }

        @Override
        protected void onFocus() {
            super.onFocus();
            if (property.focusable) {
                focusPanel.setVisible(true);
            }
        }

        @Override
        protected void onBlur() {
            super.onBlur();
            if (property.focusable) {
                focusPanel.setVisible(false);
            }
        }

        @Override
        public void onEditFinished() {
            if (focusTargetAfterEdit != null) {
                Element.as(focusTargetAfterEdit).focus();
                focusTargetAfterEdit = null;
            } else {
                setFocus(true);
            }
        }
    }
}
