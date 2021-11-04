package lsfusion.gwt.client.form.property.panel.view;

import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.base.Dimension;
import lsfusion.gwt.client.base.view.FlexPanel;
import lsfusion.gwt.client.base.view.GFlexAlignment;
import lsfusion.gwt.client.base.view.ResizableComplexPanel;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.design.view.flex.LinearCaptionContainer;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.property.GPropertyDraw;

import static lsfusion.gwt.client.base.GwtClientUtils.getOffsetSize;

public class PropertyPanelRenderer extends PanelRenderer {

    private final FlexPanel panel;

    private final Label label;
    
    private final boolean vertical;

    public PropertyPanelRenderer(final GFormController form, ActionOrPropertyValueController controller, GPropertyDraw property, GGroupObjectValue columnKey, LinearCaptionContainer captionContainer) {
        super(form, controller, property, columnKey, captionContainer);

        vertical = property.panelCaptionVertical;
        boolean captionLast = property.isPanelCaptionLast();
        GFlexAlignment panelCaptionAlignment = property.getPanelCaptionAlignment(); // vertical alignment
        boolean alignCaption = property.isAlignCaption() && captionContainer != null;

        panel = new Panel(vertical);
        panel.addStyleName("dataPanelRendererPanel");

        label = new Label();
        label.addStyleName("alignPanelLabel");

        if (this.property.captionFont != null)
            this.property.captionFont.apply(label.getElement().getStyle());

        if (!alignCaption && !captionLast)
            panel.add(label, panelCaptionAlignment);

        // we need to wrap into simple panel to make layout independent from property value (make flex-basis 0 for upper components)
        ResizableComplexPanel simplePanel = new ResizableComplexPanel();
        panel.addFill(simplePanel); // getWidth(), getHeight()

        if (!alignCaption && captionLast)
            panel.add(label, panelCaptionAlignment);

        if(property.autoSize) { // we still need a panel to append corners
            simplePanel.getElement().getStyle().setPosition(Style.Position.RELATIVE); // for corners (setStatic sets position absolute, so we don't need to do this for setStatic)
            value.setDynamic(simplePanel, true);
        } else
            value.setStatic(simplePanel, true);

        if(alignCaption)
            captionContainer.put(label, panelCaptionAlignment);

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

    private boolean notEmptyText;
    protected void setLabelText(String text) {
        label.setText(text);

        boolean notEmptyText = text != null && !text.isEmpty();
        if(this.notEmptyText != notEmptyText) {
            if(notEmptyText)
                label.addStyleName("notEmptyPanelLabel");
            else
                label.removeStyleName("notEmptyPanelLabel");
            this.notEmptyText = notEmptyText;
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
