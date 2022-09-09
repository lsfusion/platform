package lsfusion.gwt.client.form.property.panel.view;

import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.Result;
import lsfusion.gwt.client.base.view.GFlexAlignment;
import lsfusion.gwt.client.base.view.LabelWidget;
import lsfusion.gwt.client.base.view.SizedFlexPanel;
import lsfusion.gwt.client.base.view.SizedWidget;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.design.view.CaptionWidget;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.property.GPropertyDraw;

public class PropertyPanelRenderer extends PanelRenderer {

    private SizedWidget sizedView;

    private LabelWidget label;

    public PropertyPanelRenderer(final GFormController form, ActionOrPropertyValueController controller, GPropertyDraw property, GGroupObjectValue columnKey, Result<CaptionWidget> captionContainer) {
        super(form, controller, property, columnKey);

//        value.getElement().setAttribute("id", property.sID);
        value.getElement().setId(property.propertyFormName);

        SizedWidget valueWidget = value.getSizedWidget();

        boolean editableNotNull = property.isEditableNotNull();
        if (editableNotNull || property.hasChangeAction)
            appendCorners(editableNotNull, valueWidget.widget.getElement());

        sizedView = initCaption(valueWidget, property, captionContainer);

        finalizeInit();
    }

    private SizedWidget initCaption(SizedWidget valuePanel, GPropertyDraw property, Result<CaptionWidget> captionContainer) {
        if(property.caption == null) // if there is no (empty) static caption and no dynamic caption
            return valuePanel;

        label = new LabelWidget();
        label.addStyleName("panel-label");
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

    public static void appendCorners(boolean notNull, Element panelElement) {
        if(notNull)
            panelElement.addClassName("notNullCornerTriangleHolder");
        else
            panelElement.addClassName("changeActionCornerTriangleHolder");
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

        GwtClientUtils.setInnerContent(label.getElement(), text);

        boolean notEmptyText = text != null && !text.isEmpty();
        if(this.notEmptyText != notEmptyText) {
            if(notEmptyText)
                label.addStyleName("panel-not-empty-label");
            else
                label.removeStyleName("panel-not-empty-label");
            this.notEmptyText = notEmptyText;
        }
    }
}
