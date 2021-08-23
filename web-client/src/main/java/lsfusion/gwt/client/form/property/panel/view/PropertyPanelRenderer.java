package lsfusion.gwt.client.form.property.panel.view;

import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.base.Dimension;
import lsfusion.gwt.client.base.Pair;
import lsfusion.gwt.client.base.view.FlexPanel;
import lsfusion.gwt.client.base.view.ResizableComplexPanel;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.object.panel.controller.GPropertyPanelController;
import lsfusion.gwt.client.form.property.GPropertyDraw;

import static lsfusion.gwt.client.base.GwtClientUtils.getOffsetSize;
import static lsfusion.gwt.client.view.StyleDefaults.DATA_PANEL_LABEL_MARGIN;

public class PropertyPanelRenderer extends PanelRenderer {

    private final FlexPanel panel;

    private final Label label;
    
    private final boolean vertical;
    private final boolean tableFirst;
    
    private Boolean labelMarginRight = null;

    public PropertyPanelRenderer(final GFormController form, ActionOrPropertyValueController controller, GPropertyDraw property, GGroupObjectValue columnKey, GPropertyPanelController.CaptionContainer captionContainer) {
        super(form, controller, property, columnKey, captionContainer);

        vertical = property.panelCaptionVertical;
        tableFirst = property.isPanelCaptionLast();

        panel = new Panel(vertical);
        panel.addStyleName("dataPanelRendererPanel");

        label = new Label();
        if (this.property.captionFont != null)
            this.property.captionFont.apply(label.getElement().getStyle());

        if (!tableFirst && captionContainer == null)
            panel.add(label, property.getPanelCaptionAlignment());

        // we need to wrap into simple panel to make layout independent from property value (make flex-basis 0 for upper components)
        ResizableComplexPanel simplePanel = new ResizableComplexPanel();
        panel.addFill(simplePanel); // getWidth(), getHeight()

        if (tableFirst && captionContainer == null)
            panel.add(label, property.getPanelCaptionAlignment());

        if (!vertical)
            labelMarginRight = captionContainer != null || !tableFirst;

        Pair<Integer, Integer> valueSizes;
        if(property.autoSize) { // we still need a panel to append corners
            assert captionContainer == null;
            simplePanel.getElement().getStyle().setPosition(Style.Position.RELATIVE); // for corners (setStatic sets position absolute, so we don't need to do this for setStatic)
            valueSizes = value.setDynamic(simplePanel, true);
            if(property.isAutoDynamicHeight())
                valueSizes = null;
        } else
            valueSizes = value.setStatic(simplePanel, true);

        if(captionContainer != null && valueSizes != null)
            captionContainer.put(label, valueSizes, property.getPanelCaptionAlignment());

        appendCorners(property, simplePanel); // it's a hack to add

        finalizeInit();
    }

    public void appendCorners(GPropertyDraw property, com.google.gwt.user.client.ui.Panel panel) {
        if (property.notNull || property.hasChangeAction) {
            DivElement changeEventSign = Document.get().createDivElement();
            changeEventSign.addClassName("rightBottomCornerTriangle");
            changeEventSign.addClassName(property.notNull ? "notNullCornerTriangle" : "changeActionCornerTriangle");
            panel.getElement().appendChild(changeEventSign);
        }
    }

    @Override
    public Widget getComponent() {
        return panel;
    }

    @Override
    protected Widget getTooltipWidget() {
        return label;
    }

    protected void setLabelText(String text) {
        label.setText(text);
        if (labelMarginRight != null) {
            if (labelMarginRight) {
                label.getElement().getStyle().setMarginRight(text.isEmpty() ? 0 : DATA_PANEL_LABEL_MARGIN, Style.Unit.PX);
            } else {
                label.getElement().getStyle().setMarginLeft(text.isEmpty() ? 0 : DATA_PANEL_LABEL_MARGIN, Style.Unit.PX);
            }
        }
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
            int width = property.getValueWidthWithPadding(null) + 4;
            int height = property.getValueHeightWithPadding(null) + 4;

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
}
