package lsfusion.gwt.client.form.property.panel.view;

import com.google.gwt.dom.client.Element;
import com.google.gwt.event.dom.client.ContextMenuEvent;
import com.google.gwt.event.dom.client.MouseDownEvent;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.base.*;
import lsfusion.gwt.client.base.view.GFlexAlignment;
import lsfusion.gwt.client.base.view.ResizableComplexPanel;
import lsfusion.gwt.client.base.view.SizedFlexPanel;
import lsfusion.gwt.client.base.view.SizedWidget;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.design.GFont;
import lsfusion.gwt.client.form.design.view.CaptionWidget;
import lsfusion.gwt.client.form.design.view.ComponentViewWidget;
import lsfusion.gwt.client.form.design.view.GFormLayout;
import lsfusion.gwt.client.form.design.view.InlineComponentViewWidget;
import lsfusion.gwt.client.form.event.GMouseStroke;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.PValue;
import lsfusion.gwt.client.form.property.cell.view.RenderContext;

public class PropertyPanelRenderer extends PanelRenderer {

    private final ComponentViewWidget sizedView;

    private Widget label;

    private Widget comment;

    public PropertyPanelRenderer(final GFormController form, ActionOrPropertyValueController controller, GPropertyDraw property, GGroupObjectValue columnKey, Result<CaptionWidget> captionContainer) {
        super(form, controller, property, columnKey, property.isAction()); // assert if is Action that property has alignCaption() true and captionContainer != null

        SizedWidget valueWidget = value.getSizedWidget(false);

        setStyles(valueWidget.widget.getElement(), property.isEditableNotNull((RenderContext) value), property.hasChangeAction);

        sizedView = initCaption(valueWidget, property, captionContainer);

        finalizeInit();
    }

    public static void setStyles(Element panelElement, boolean notNull, boolean hasChangeAction) {
        if (notNull)
            GwtClientUtils.addClassName(panelElement, "property-not-null");
        else if(hasChangeAction)
            GwtClientUtils.addClassName(panelElement, "property-has-change");
    }

    @Override
    public void update(PValue value, boolean loading, AppBaseImage image, String valueElementClass,
                       GFont font, String background, String foreground, Boolean readOnly, String placeholder, String pattern,
                       String regexp, String regexpMessage, String valueTooltip, PValue propertyCustomOption) {
        if(property.hasDynamicImage() && !property.isAction()) {
            BaseImage.updateImage(image, label);
            image = null;
        }

        // we don't need image in value
        super.update(value, loading, image, valueElementClass, font, background, foreground, readOnly, placeholder, pattern,
                regexp, regexpMessage, valueTooltip, propertyCustomOption);
    }

    private ComponentViewWidget initCaption(SizedWidget valuePanel, GPropertyDraw property, Result<CaptionWidget> captionContainer) {
        if(property.caption == null && property.comment == null) // if there is no (empty) static caption and no dynamic caption
            return valuePanel.view;

        // id and for we need to support editing when clicking on the label
        // however only CLICK and CHANGE (for boolean props) are propagated to the input, and not MOUSEDOWN
        // but since we use MOUSEDOWN as a change event (so it starts editing) we need to propagate MOUSEDOWN manually
        // we need id to be global (otherwise everything stops working if the same form is opened twice)
        String globalID = form.globalID + "->" + property.propertyFormName;
        value.getElement().setId(globalID);

        SizedWidget sizedLabel = null;
        if(property.caption != null) {
            label = GFormLayout.createLabelCaptionWidget();
            BaseImage.initImageText(label, null, property.appImage, property.getCaptionHtmlOrTextType());
            GwtClientUtils.addClassName(label, "panel-property-label");

            label.getElement().setAttribute("for", globalID);
            Element valueElement = value.getElement();

            // display context menu when right-clicking on a label item. It is to allow to show context menu when checkboxes are displayed by buttons
            label.addDomHandler(event -> {
                GwtClientUtils.fireOnContextmenu(valueElement);
                GwtClientUtils.stopPropagation(event.getNativeEvent());
            }, ContextMenuEvent.getType());

            label.addDomHandler(event -> {
                if (GMouseStroke.isChangeEvent(event.getNativeEvent())) {// check that this is the left mouse button, because the top ContextMenuEvent should trigger on the right button.
                    GwtClientUtils.fireOnMouseDown(valueElement);
                    GwtClientUtils.stopPropagation(event.getNativeEvent()); // need this because otherwise default handler will lead to the blur event
                }
            }, MouseDownEvent.getType());

            // mostly it is needed to handle margins / paddings / layouting but we do it ourselves
//        CellRenderer cellRenderer = property.getCellRenderer();
//        cellRenderer.renderPanelLabel(label);

            sizedLabel = new SizedWidget(label, property.getCaptionWidth(), property.getCaptionHeight());
        }

        GFlexAlignment captionAlignmentHorz = property.getCaptionAlignmentHorz(); // vertical alignment
        GFlexAlignment captionAlignmentVert = property.getCaptionAlignmentVert(); // vertical alignment
        boolean captionLast = property.isCaptionLast();

        GFlexAlignment panelValueAlignment = property.getPanelValueAlignment(); // vertical alignment

        GFlexAlignment panelCommentAlignment = property.getPanelCommentAlignment(); // vertical alignment
        boolean commentFirst = property.isPanelCommentFirst();

        boolean isAlignCaption = property.isAlignCaption() && captionContainer != null;
        boolean inline = !isAlignCaption && property.isInline();
        boolean verticalDiffers = property.caption != null && property.comment != null && !inline && property.captionVertical != property.panelCommentVertical;
        boolean panelVertical = property.caption != null ? property.captionVertical : property.panelCommentVertical;

        SizedWidget sizedComment = null;
        if(property.comment != null) {
            comment = GFormLayout.createLabelCaptionWidget();
            GwtClientUtils.initCaptionHtmlOrText(comment.getElement(), property.panelCommentVertical ? CaptionHtmlOrTextType.COMMENT_VERT : CaptionHtmlOrTextType.COMMENT_HORZ);
            GwtClientUtils.addClassName(comment, "panel-comment");

            //            setCommentText(property.comment);
            sizedComment = new SizedWidget(comment, property.getCaptionWidth(), property.getCaptionHeight());
            if (isAlignCaption || verticalDiffers) {
                SizedFlexPanel valueCommentPanel = new SizedFlexPanel(property.panelCommentVertical);

                if (commentFirst)
                    sizedComment.add(valueCommentPanel, panelCommentAlignment);

                valuePanel.addFill(valueCommentPanel);

                if (!commentFirst)
                    sizedComment.add(valueCommentPanel, panelCommentAlignment);

                valuePanel = new SizedWidget(valueCommentPanel);
            }
        }

        if (isAlignCaption) { // align caption
            if(captionLast && property.isPanelBoolean()) // the main problem here is that in the boolean default caption last we don't know if it's gonna be aligned, so we'll use that hack for now (until we'll move isAlignCaptions to the server)
                captionLast = false;

            captionContainer.set(new CaptionWidget(captionLast ? valuePanel : sizedLabel, captionAlignmentHorz, captionAlignmentVert, panelValueAlignment));
            return (captionLast ? sizedLabel : valuePanel).view;
        }

        InlineComponentViewWidget componentViewWidget = new InlineComponentViewWidget(panelVertical);

        if (sizedLabel != null && !captionLast)
            componentViewWidget.add(sizedLabel, panelVertical ? captionAlignmentHorz : captionAlignmentVert, false, "caption");

        if (sizedComment != null && commentFirst && !verticalDiffers)
            componentViewWidget.add(sizedComment, panelCommentAlignment, false, "comment");

        componentViewWidget.add(valuePanel, panelValueAlignment, true, "");

        if (sizedComment != null && !verticalDiffers && !commentFirst)
            componentViewWidget.add(sizedComment, panelCommentAlignment, false, "comment");

        if (sizedLabel != null && captionLast)
            componentViewWidget.add(sizedLabel, panelVertical ? captionAlignmentHorz : captionAlignmentVert, false, "caption");

        if(inline)
            return componentViewWidget;

        Widget widget;
        if (property.panelCustom) {
            ResizableComplexPanel panel = new ResizableComplexPanel();
            componentViewWidget.add(panel, 0);
            GwtClientUtils.addClassName(panel, "panel-custom");
            widget = panel;
        } else {
            SizedFlexPanel panel = new SizedFlexPanel(panelVertical);
            panel.transparentResize = true;
            GwtClientUtils.addClassName(panel, "panel-container");
            componentViewWidget.add(panel, 0);
            // mostly it is needed to handle margins / paddings / layouting but we do it ourselves
//          cellRenderer.renderPanelContainer(panel);
            widget = panel;
        }

        return new SizedWidget(widget).view;
    }

    @Override
    public ComponentViewWidget getComponentViewWidget() {
        return sizedView;
    }

    @Override
    protected Widget getLabelWidget() {
        return label;
    }

    @Override
    protected Widget getTooltipWidget() {
        return label != null ? label : (comment != null ? comment : super.getTooltipWidget());
    }

    protected void setLabelText(String text) {
        if(label == null) {
            assert text == null || text.isEmpty();
            return;
        }

        BaseImage.updateText(label, text);
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

        GwtClientUtils.setCaptionHtmlOrText(comment.getElement(), text);
    }

    protected void setCommentClasses(String classes) {
        if(comment == null) {
            return;
        }

        GwtClientUtils.addClassName(comment.getElement(), classes);
    }
}
