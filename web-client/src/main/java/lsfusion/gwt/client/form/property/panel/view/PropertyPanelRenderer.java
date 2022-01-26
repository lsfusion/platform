package lsfusion.gwt.client.form.property.panel.view;

import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.base.view.FlexPanel;
import lsfusion.gwt.client.base.view.GFlexAlignment;
import lsfusion.gwt.client.base.view.ResizableComplexPanel;
import lsfusion.gwt.client.base.view.SizedWidget;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.design.view.flex.LinearCaptionContainer;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.property.GPropertyDraw;

import static lsfusion.gwt.client.base.GwtClientUtils.getOffsetSize;

public class PropertyPanelRenderer extends PanelRenderer {

    private SizedWidget sizedView;

    private Label label;

    public PropertyPanelRenderer(final GFormController form, ActionOrPropertyValueController controller, GPropertyDraw property, GGroupObjectValue columnKey, LinearCaptionContainer captionContainer) {
        super(form, controller, property, columnKey, captionContainer);

        // we need to wrap into panel to make layout independent from property value (size component - make flex-basis 0 for upper components)
        // plus for corners and border
        boolean needCorners = property.notNull || property.hasChangeAction;
        // in fact in grid we always wrap into div (because element to be added is td and it has totally different behaviour)
        // however here it doesn't seem to be necessary (except when we need corners), so we won't do this
        ResizableComplexPanel valuePanel = null;
        if(!property.autoSize || needCorners)
            valuePanel = new ResizableComplexPanel();
        SizedWidget valueWidget = value.setSized(valuePanel);
        if (needCorners)
            appendCorners(property.notNull, valuePanel); // it's a hack to add

        sizedView = initCaption(valueWidget, property, captionContainer);
        sizedView.widget.addStyleName("dataPanelRendererPanel");

        finalizeInit();
    }

    private SizedWidget initCaption(SizedWidget valuePanel, GPropertyDraw property, LinearCaptionContainer captionContainer) {
        if(property.caption == null) // if there is no (empty) static caption and no dynamic caption
            return valuePanel;

        label = new Label();
        label.addStyleName("alignPanelLabel");

        if (this.property.captionFont != null)
            this.property.captionFont.apply(label.getElement().getStyle());

        GFlexAlignment panelCaptionAlignment = property.getPanelCaptionAlignment(); // vertical alignment
        boolean captionLast = property.isPanelCaptionLast();

        if(property.isAlignCaption() && captionContainer != null) { // align caption
            SizedWidget sizedLabel = new SizedWidget(label);
            if(!panelCaptionAlignment.equals(GFlexAlignment.END))
                captionLast = false; // it's odd having caption last for alignments other than END

            captionContainer.put(captionLast ? valuePanel : sizedLabel, panelCaptionAlignment);

            return captionLast ? sizedLabel : valuePanel;
        }

        FlexPanel panel;
        panel = new FlexPanel(property.panelCaptionVertical);

        if (!captionLast)
            panel.add(label, panelCaptionAlignment);

        valuePanel.addFill(panel);

        if (captionLast)
            panel.add(label, panelCaptionAlignment);

        return new SizedWidget(panel);
    }

    public void appendCorners(boolean notNull, com.google.gwt.user.client.ui.Panel panel) {
        Element panelElement = panel.getElement();
        panelElement.getStyle().setPosition(Style.Position.RELATIVE); // for corners (setStatic sets position absolute, so we don't need to do this for setStatic)
        DivElement changeEventSign = Document.get().createDivElement();
        changeEventSign.addClassName("rightBottomCornerTriangle");
        if(notNull)
            changeEventSign.addClassName("notNullCornerTriangle");
        else
            changeEventSign.addClassName("changeActionCornerTriangle");
        panelElement.appendChild(changeEventSign);
    }

    @Override
    public SizedWidget getSizedWidget() {
        return sizedView;
    }

    @Override
    protected Widget getTooltipWidget() {
        return label != null ? label : super.getTooltipWidget();
    }

    private boolean notEmptyText;
    protected void setLabelText(String text) {
        if(label == null) {
            assert text == null || text.isEmpty();
            return;
        }

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
}
