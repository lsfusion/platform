package lsfusion.gwt.client.form.property.panel.view;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.EventTarget;
import com.google.gwt.dom.client.Style;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.base.Dimension;
import lsfusion.gwt.client.base.GwtSharedUtils;
import lsfusion.gwt.client.base.TooltipManager;
import lsfusion.gwt.client.base.view.FlexPanel;
import lsfusion.gwt.client.base.view.GFlexAlignment;
import lsfusion.gwt.client.base.view.ResizableComplexPanel;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.cell.GEditBindingMap;
import lsfusion.gwt.client.form.property.cell.classes.ColorDTO;

import static lsfusion.gwt.client.base.GwtClientUtils.getOffsetSize;
import static lsfusion.gwt.client.base.GwtClientUtils.isShowing;

public class DataPanelRenderer implements PanelRenderer {
    public final GPropertyDraw property;

    private final FlexPanel panel;
    private final ResizableComplexPanel gridPanel;

    private final HTML label;
    
    private final GSinglePropertyTable valueTable;

    private String caption;
    private String tooltip;

    private EventTarget focusTargetAfterEdit;
    private final boolean vertical;

    public DataPanelRenderer(final GFormController form, GPropertyDraw iproperty, GGroupObjectValue columnKey) {
        this.property = iproperty;

        vertical = property.panelCaptionAbove;

        tooltip = property.getTooltipText(property.getCaptionOrEmpty());

        label = new HTML();
        label.addStyleName("customFontPresenter");
        setLabelText(caption = property.getEditCaption());

        TooltipManager.registerWidget(label, new TooltipManager.TooltipHelper() {
            @Override
            public String getTooltip() {
                return tooltip;
            }

            @Override
            public boolean stillShowTooltip() {
                return label.isAttached() && label.isVisible();
            }
        });

        if (property.captionFont != null) {
            property.captionFont.apply(label.getElement().getStyle());
        }

        valueTable = new ValueTable(form, columnKey);

        gridPanel = new ResizableComplexPanel();
        gridPanel.addStyleName("dataPanelRendererGridPanel");

        if (!property.focusable) {
            valueTable.setTableFocusable(false);
        }

        gridPanel.add(valueTable);

        valueTable.setSize("100%", "100%");

        panel = new Panel(vertical);
        panel.addStyleName("dataPanelRendererPanel");

        panel.add(label, GFlexAlignment.CENTER);
        panel.add(gridPanel, vertical ? GFlexAlignment.STRETCH : GFlexAlignment.CENTER, 1);

        String preferredHeight = property.getValueHeight(null) + "px";
        String preferredWidth = property.getValueWidth(null) + "px";

        gridPanel.setHeight(preferredHeight);
        if (!vertical) {
            gridPanel.setWidth(preferredWidth);
        }

        gridPanel.getElement().getStyle().setProperty("minHeight", preferredHeight);
        gridPanel.getElement().getStyle().setProperty("minWidth", preferredWidth);
        gridPanel.getElement().getStyle().setProperty("maxHeight", preferredHeight);

        finishLayoutSetup();

        valueTable.getElement().setPropertyObject("groupObject", property.groupObject);
        form.addPropertyBindings(property, () -> new GFormController.Binding(property.groupObject) {
            @Override
            public void pressed(EventTarget eventTarget) {
                focusTargetAfterEdit = eventTarget;
                valueTable.editCellAt(0, 0, GEditBindingMap.CHANGE);
            }
            @Override
            public boolean showing() {
                return isShowing(panel);
            }
        });
    }

    private void finishLayoutSetup() {
        if (property.isVerticallyStretched()) {
            panel.setChildAlignment(gridPanel, GFlexAlignment.STRETCH);

            gridPanel.getElement().getStyle().clearHeight();
            //gridPanel.getElement().getStyle().clearProperty("minHeight");
            gridPanel.getElement().getStyle().clearProperty("maxHeight");
            valueTable.setupFillParent();
        }

        if (property.isHorizontallyStretched()) {
            gridPanel.getElement().getStyle().clearWidth();
            gridPanel.getElement().getStyle().clearProperty("maxWidth");
        }
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
            setLabelText(property.getEditCaption(caption));
            tooltip = property.getTooltipText(caption);
        }
    }
    
    private void setLabelText(String text) {
        boolean empty = text.isEmpty();
        label.setHTML(empty ? SafeHtmlUtils.EMPTY_SAFE_HTML : SafeHtmlUtils.fromString(text));
        if (!vertical) {
            label.getElement().getStyle().setMarginRight(empty ? 0 : 4, Style.Unit.PX);
        }
    }

    @Override
    public void setDefaultIcon() {
    }

    @Override
    public void setImage(String iconPath) {
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
        Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
            public void execute () {
                valueTable.setFocus(true);
            }
        });
    }

    private class Panel extends FlexPanel {
        public Panel(boolean vertical) {
            super(vertical);
        }

        @Override
        public Dimension getMaxPreferredSize() {
            Dimension pref = getOffsetSize(label);
            if (!vertical) {
                pref.width += 4; //extra for right label margin
            }

            //+extra for borders and margins
            int width = property.getValueWidth(null) + 4;
            int height = property.getValueHeight(null) + 4;

            if (isVertical()) {
                pref.width = Math.max(pref.width, width);
                pref.height += height;
            } else {
                pref.width += width;
                pref.height = Math.max(pref.height, height);
            }

            return pref;
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
                gridPanel.addStyleName("dataPanelRendererGridPanelFocused");
            }
        }

        @Override
        protected void onBlur() {
            super.onBlur();
            if (property.focusable) {
                gridPanel.removeStyleName("dataPanelRendererGridPanelFocused");
            }
        }

        @Override
        public void takeFocusAfterEdit() {
            if (focusTargetAfterEdit != null) {
                Element.as(focusTargetAfterEdit).focus();
                focusTargetAfterEdit = null;
            } else {
                setFocus(true);
            }
        }
    }
}
