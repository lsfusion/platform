package lsfusion.gwt.client.form.property.panel.view;

import com.google.gwt.dom.client.Element;
import com.google.gwt.event.dom.client.MouseDownEvent;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.base.AppBaseImage;
import lsfusion.gwt.client.base.BaseImage;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.Result;
import lsfusion.gwt.client.base.view.GFlexAlignment;
import lsfusion.gwt.client.base.view.SizedFlexPanel;
import lsfusion.gwt.client.base.view.SizedWidget;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.design.view.CaptionWidget;
import lsfusion.gwt.client.form.design.view.GFormLayout;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.PValue;

public class PropertyPanelRenderer extends PanelRenderer {

    private SizedWidget sizedView;

    private Widget label;

    private Widget comment;

    public PropertyPanelRenderer(final GFormController form, ActionOrPropertyValueController controller, GPropertyDraw property, GGroupObjectValue columnKey, Result<CaptionWidget> captionContainer) {
        super(form, controller, property, columnKey, property.isAction()); // assert if is Action that property has alignCaption() true and captionContainer != null

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
    public void update(PValue value, boolean loading, AppBaseImage image, String valueElementClass, String background, String foreground, boolean readOnly) {
        if(property.hasDynamicImage() && !property.isAction()) {
            BaseImage.updateImage(image, label, property.panelCaptionVertical);
            image = null;
        }

        // we don't need image in value
        super.update(value, loading, image, valueElementClass, background, foreground, readOnly);
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

        // id and for we need to support editing when clicking on the label
        // however only CLICK and CHANGE (for boolean props) are propagated to the input, and not MOUSEDOWN
        // but since we use MOUSEDOWN as a change event (so it starts editing) we need to propagate MOUSEDOWN manually
        // we need id to be global (otherwise everything stops working if the same form is opened twice)
        String globalID = form.globalID + "->" + property.propertyFormName;
        value.getElement().setId(globalID);
        label.getElement().setAttribute("for", globalID);
        label.addDomHandler(event -> {
            GwtClientUtils.fireOnMouseDown(value.getElement());
        }, MouseDownEvent.getType());

        if (this.property.captionFont != null)
            this.property.captionFont.apply(label.getElement().getStyle());

        // mostly it is needed to handle margins / paddings / layouting but we do it ourselves
//        CellRenderer cellRenderer = property.getCellRenderer();
//        cellRenderer.renderPanelLabel(label);

        GFlexAlignment panelCaptionAlignment = property.getPanelCaptionAlignment(); // vertical alignment
        GFlexAlignment panelValueAlignment = property.getPanelValueAlignment(); // vertical alignment
        GFlexAlignment panelCommentAlignment = property.getPanelCommentAlignment(); // vertical alignment
        boolean captionLast = property.isPanelCaptionLast();
        boolean commentFirst = property.isPanelCommentFirst();
        SizedWidget sizedLabel = new SizedWidget(label, property.getCaptionWidth(), property.getCaptionHeight());

        SizedWidget commentWidget = null;
        if(property.comment != null) {
            comment = GFormLayout.createLabelCaptionWidget();
            BaseImage.initImageText(comment, property.comment, property.appImage, property.panelCommentVertical);
            comment.addStyleName("panel-label");
            commentWidget = new SizedWidget(comment, property.getCaptionWidth(), property.getCaptionHeight());
        }

        if(property.isAlignCaption() && captionContainer != null) { // align caption
            if(!panelCaptionAlignment.equals(GFlexAlignment.END))
                captionLast = false; // it's odd having caption last for alignments other than END

            SizedWidget valueCommentWidget;
            if(commentWidget != null) {
                valueCommentWidget = createValueCommentWidget(valuePanel, panelValueAlignment, commentWidget, panelCommentAlignment, commentFirst);
            } else {
                valueCommentWidget = valuePanel;
            }

            captionContainer.set(new CaptionWidget(captionLast ? valueCommentWidget : sizedLabel, GFlexAlignment.START, panelCaptionAlignment, panelValueAlignment));
            return captionLast ? sizedLabel : valueCommentWidget;
        }

        SizedFlexPanel panel = new SizedFlexPanel(property.panelCaptionVertical);
        panel.addStyleName("panel-container");

        if(property.panelCaptionVertical != property.panelCommentVertical) {

            SizedWidget valueCommentWidget = createValueCommentWidget(valuePanel, panelValueAlignment, commentWidget, panelCommentAlignment, commentFirst);

            if (!captionLast)
                sizedLabel.add(panel, panelCaptionAlignment);

            panel.transparentResize = true;
            valueCommentWidget.add(panel, panelValueAlignment, 1, true);

            if (captionLast)
                sizedLabel.add(panel, panelCaptionAlignment);

        } else {

            if (commentFirst)
                commentWidget.add(panel, panelCommentAlignment);

            if (!captionLast)
                sizedLabel.add(panel, panelCaptionAlignment);

            panel.transparentResize = true;
            valuePanel.add(panel, panelValueAlignment, 1, true);

            if (captionLast)
                sizedLabel.add(panel, panelCaptionAlignment);

            if (!commentFirst)
                commentWidget.add(panel, panelCommentAlignment);
        }

        // mostly it is needed to handle margins / paddings / layouting but we do it ourselves
//        cellRenderer.renderPanelContainer(panel);

        return new SizedWidget(panel);
    }

    private SizedWidget createValueCommentWidget(SizedWidget valuePanel, GFlexAlignment panelValueAlignment,
                                                 SizedWidget commentWidget, GFlexAlignment panelCommentAlignment, boolean commentFirst) {
        SizedFlexPanel valueCommentPanel = new SizedFlexPanel(property.panelCommentVertical);

        if (commentFirst)
            commentWidget.add(valueCommentPanel, panelCommentAlignment);

        valuePanel.add(valueCommentPanel, panelValueAlignment);

        if (!commentFirst)
            commentWidget.add(valueCommentPanel, panelCommentAlignment);

        return new SizedWidget(valueCommentPanel);
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

    protected void setLabelClasses(String classes) {
//        if(label == null) {
//            return;
//        }

        BaseImage.updateClasses(label, classes);
    }
}
