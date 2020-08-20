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
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.property.GPropertyDraw;

import static lsfusion.gwt.client.base.GwtClientUtils.getOffsetSize;

public class PropertyPanelRenderer extends PanelRenderer {

    private final FlexPanel panel;

    private final Label label;
    
    private final boolean vertical;

    public PropertyPanelRenderer(final GFormController form, GPropertyDraw property, GGroupObjectValue columnKey) {
        super(form, property, columnKey);

        vertical = property.panelCaptionVertical;

        panel = new Panel(vertical);
        panel.addStyleName("dataPanelRendererPanel");

        label = new Label();
//        label.addStyleName("customFontPresenter");
        if (this.property.captionFont != null) {
            this.property.captionFont.apply(label.getElement().getStyle());
        }
        panel.add(label, GFlexAlignment.CENTER);

        // we need to wrap into simple panel to make layout independent from property value
        ResizableComplexPanel simplePanel = new ResizableComplexPanel();
        if(property.autoSize) { // we still need a panel to append corners
            simplePanel.getElement().getStyle().setPosition(Style.Position.RELATIVE); // for corners (setStatic sets position absolute, so we don't need to do this for setStatic)
            value.setDynamic(simplePanel, true);
        } else
            value.setStatic(simplePanel, true);
        panel.add(simplePanel, panel.getWidgetCount(), GFlexAlignment.STRETCH, 1); // getWidth(), getHeight()
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
        if (!vertical) {
            label.getElement().getStyle().setMarginRight(text.isEmpty() ? 0 : 4, Style.Unit.PX);
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
