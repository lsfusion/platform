package lsfusion.gwt.client.form.property.panel.view;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.InputElement;
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
import lsfusion.gwt.client.form.property.cell.classes.view.SimpleTextBasedCellRenderer;

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

        boolean isAlignCaption = property.isAlignCaption() && captionContainer != null;
        boolean verticalDiffers = property.panelCaptionVertical != property.panelCommentVertical;

        SizedWidget commentWidget = null;
        if(property.comment != null) {
            comment = GFormLayout.createLabelCaptionWidget();
            comment.getElement().setInnerText(property.comment);
            commentWidget = new SizedWidget(comment, property.getCaptionWidth(), property.getCaptionHeight());
            if (isAlignCaption || verticalDiffers) {
                SizedFlexPanel valueCommentPanel = new SizedFlexPanel(property.panelCommentVertical);

                if (commentFirst)
                    commentWidget.add(valueCommentPanel, panelCommentAlignment);

                valuePanel.addFill(valueCommentPanel);

                if (!commentFirst)
                    commentWidget.add(valueCommentPanel, panelCommentAlignment);

                valuePanel = new SizedWidget(valueCommentPanel);
            }
        }

        if (isAlignCaption) { // align caption
            if(!panelCaptionAlignment.equals(GFlexAlignment.END))
                captionLast = false; // it's odd having caption last for alignments other than END

            captionContainer.set(new CaptionWidget(captionLast ? valuePanel : sizedLabel, GFlexAlignment.START, panelCaptionAlignment, panelValueAlignment));
            return captionLast ? sizedLabel : valuePanel;
        }

        SizedFlexPanel panel = new SizedFlexPanel(property.panelCaptionVertical);
        panel.addStyleName("panel-container");

        if (!captionLast)
            sizedLabel.add(panel, panelCaptionAlignment);

        if (commentWidget != null && !verticalDiffers && commentFirst)
            commentWidget.add(panel, panelCommentAlignment);

        panel.transparentResize = true;
        valuePanel.add(panel, panelValueAlignment, 1, true);

        if (commentWidget != null && !verticalDiffers && !commentFirst)
            commentWidget.add(panel, panelCommentAlignment);

        if (captionLast)
            sizedLabel.add(panel, panelCaptionAlignment);

        // mostly it is needed to handle margins / paddings / layouting but we do it ourselves
//
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

    protected void setLabelClasses(String classes) {
        // there can be no caption
        if(label == null) {
            return;
        }

        BaseImage.updateClasses(label, classes);
    }

    protected void setCommentText(String text) {
        if(comment == null) {
            assert text == null || text.isEmpty();
            return;
        }

        comment.getElement().setInnerText(text);
    }

    protected void setCommentClasses(String classes) {
        comment.getElement().addClassName(classes);
    }

    protected void setPlaceholderText(String placeholder) {
        InputElement inputElement = SimpleTextBasedCellRenderer.getInputElement(value.getElement());
        if (placeholder != null)
            inputElement.setAttribute("placeholder", placeholder);
        else
            inputElement.removeAttribute("placeholder");
    }
}
