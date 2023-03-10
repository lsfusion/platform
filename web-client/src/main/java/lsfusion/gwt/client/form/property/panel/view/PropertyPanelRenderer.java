package lsfusion.gwt.client.form.property.panel.view;

import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.base.AppBaseImage;
import lsfusion.gwt.client.base.BaseImage;
import lsfusion.gwt.client.base.Result;
import lsfusion.gwt.client.base.view.GFlexAlignment;
import lsfusion.gwt.client.base.view.SizedFlexPanel;
import lsfusion.gwt.client.base.view.SizedWidget;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.design.view.CaptionWidget;
import lsfusion.gwt.client.form.design.view.GFormLayout;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.property.GPropertyDraw;

public class PropertyPanelRenderer extends PanelRenderer {

    private SizedWidget sizedView;

    private Widget label;

    public PropertyPanelRenderer(final GFormController form, ActionOrPropertyValueController controller, GPropertyDraw property, GGroupObjectValue columnKey, Result<CaptionWidget> captionContainer) {
        super(form, controller, property, columnKey);

//        value.getElement().setAttribute("id", property.sID);
        value.getElement().setId(property.propertyFormName);

        SizedWidget valueWidget = value.getSizedWidget();

        setStyles(valueWidget.widget.getElement(), property.isEditableNotNull(), property.hasChangeAction);

        sizedView = initCaption(valueWidget, property, captionContainer);

        finalizeInit();
    }

    public static void setStyles(Element panelElement, boolean notNull, boolean hasChangeAction) {
        if (notNull)
            panelElement.addClassName("property-not-null");
        else if(hasChangeAction)
            panelElement.addClassName("property-has-change");
    }

    @Override
    public void update(Object value, boolean loading, Object image, Object valueElementClass, Object background, Object foreground, boolean readOnly) {
        // we don't need image in value
        super.update(value, loading, null, valueElementClass, background, foreground, readOnly);

        if(property.hasDynamicImage())
            BaseImage.updateImage((AppBaseImage)image, label, property.panelCaptionVertical);
    }

    private SizedWidget initCaption(SizedWidget valuePanel, GPropertyDraw property, Result<CaptionWidget> captionContainer) {
        if(property.caption == null) // if there is no (empty) static caption and no dynamic caption
            return valuePanel;

        label = GFormLayout.createLabelCaptionWidget();
        BaseImage.initImageText(label, null, property.appImage, property.panelCaptionVertical);
        label.addStyleName("panel-label");
        if(!(property.isTagInput() || property.valueElementClass != null))
            label.addStyleName("text-secondary");
//        label.addStyleName("fw-semibold");

        label.getElement().setAttribute("for", property.propertyFormName);

        if (this.property.captionFont != null)
            this.property.captionFont.apply(label.getElement().getStyle());

        // mostly it is needed to handle margins / paddings / layouting but we do it ourselves
//        CellRenderer cellRenderer = property.getCellRenderer();
//        cellRenderer.renderPanelLabel(label);

        GFlexAlignment panelCaptionAlignment = property.getPanelCaptionAlignment(); // vertical alignment
        GFlexAlignment panelValueAlignment = property.getPanelValueAlignment(); // vertical alignment
        boolean captionLast = property.isPanelCaptionLast();
        SizedWidget sizedLabel = new SizedWidget(label, property.getCaptionWidth(), property.getCaptionHeight());

        if(property.isAlignCaption() && captionContainer != null) { // align caption
            if(!panelCaptionAlignment.equals(GFlexAlignment.END))
                captionLast = false; // it's odd having caption last for alignments other than END

            captionContainer.set(new CaptionWidget(captionLast ? valuePanel : sizedLabel, GFlexAlignment.START, panelCaptionAlignment, panelValueAlignment));

            return captionLast ? sizedLabel : valuePanel;
        }

        SizedFlexPanel panel = new SizedFlexPanel(property.panelCaptionVertical);

        if (!captionLast)
            sizedLabel.add(panel, panelCaptionAlignment);

        panel.transparentResize = true;
        valuePanel.add(panel, panelValueAlignment, 1, true);

        if (captionLast)
            sizedLabel.add(panel, panelCaptionAlignment);

        // mostly it is needed to handle margins / paddings / layouting but we do it ourselves
//        cellRenderer.renderPanelContainer(panel);

        return new SizedWidget(panel);
    }

    @Override
    public SizedWidget getSizedWidget() {
        return sizedView;
    }

    @Override
    protected Widget getTooltipWidget() {
        return label != null ? label : super.getTooltipWidget();
    }

    protected void setLabelText(String text) {
        if(label == null) {
            assert text == null || text.isEmpty();
            return;
        }

        BaseImage.updateText(label, text, property.panelCaptionVertical);
    }
}
