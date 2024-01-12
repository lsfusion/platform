package lsfusion.gwt.client.form.property.panel.view;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.base.AppBaseImage;
import lsfusion.gwt.client.base.FocusUtils;
import lsfusion.gwt.client.base.GwtSharedUtils;
import lsfusion.gwt.client.base.TooltipManager;
import lsfusion.gwt.client.base.view.SizedWidget;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.PValue;

import java.util.ArrayList;

import static lsfusion.gwt.client.base.GwtClientUtils.nvl;

public abstract class PanelRenderer {

    protected final GFormController form;

    protected final GPropertyDraw property;
    protected final GGroupObjectValue columnKey;

    protected final ActionOrPropertyPanelValue value;

    public ArrayList<Integer> bindingEventIndices;

    public PanelRenderer(GFormController form, ActionOrPropertyValueController controller, GPropertyDraw property, GGroupObjectValue columnKey, boolean globalCaptionIsDrawn) {
        this.form = form;
        this.property = property;
        this.columnKey = columnKey;

        value = new ActionOrPropertyPanelValue(property, columnKey, form, globalCaptionIsDrawn, controller); // captionContainer != null, now we use different approach when button is renderered as caption
    }

    private JavaScriptObject tippy = null;
    private TooltipManager.TooltipHelper tooltipHelper = null;

    protected void finalizeInit() {
        setCaption(property.caption);
        setCaptionElementClass(property.captionElementClass);
        setComment(property.comment);
        setCommentElementClass(property.commentElementClass);

        Element label = getTooltipWidget().getElement();
        tippy = TooltipManager.initTooltip(label, tooltipHelper = new TooltipManager.TooltipHelper() {
            public String getTooltip(String dynamicTooltip) {
                if(value.isEditing)
                    return null;
                return property.getTooltip(nvl(dynamicTooltip, caption));
            }

            public String getPath() {
                return property.path;
            }

            public String getCreationPath() {
                return property.creationPath;
            }

            @Override
            public String getFormPath() {
                return property.formPath;
            }
        });
        if (this.property.captionFont != null)
            GFormController.setFont(label, this.property.captionFont);
    }

    public abstract SizedWidget getSizedWidget();
    public Widget getComponent() {
        return getSizedWidget().widget;
    }

    public void update(PValue value, boolean loading, AppBaseImage image, String valueElementClass,
                       String background, String foreground, Boolean readOnly, String placeholder, String pattern,
                       String regexp, String regexpMessage, String valueTooltip) {
        this.value.update(value, loading, image, valueElementClass, background, foreground, readOnly, placeholder, pattern,
                regexp, regexpMessage, valueTooltip);
    }

    private String caption;
    public void setCaption(String caption) {
        if (!GwtSharedUtils.nullEquals(this.caption, caption)) {
            this.caption = caption;
            setLabelText(property.getPanelCaption(caption));
        }
    }
    private String captionElementClass;
    public void setCaptionElementClass(String classes) {
        if (!GwtSharedUtils.nullEquals(this.captionElementClass, classes)) {
            this.captionElementClass = classes;
            setLabelClasses(classes);
        }
    }
    private String comment;
    public void setComment(String comment) {
        if (!GwtSharedUtils.nullEquals(this.comment, comment)) {
            this.comment = comment;
            setCommentText(comment);
        }
    }
    private String commentElementClass;
    public void setCommentElementClass(String classes) {
        if (!GwtSharedUtils.nullEquals(this.commentElementClass, classes)) {
            this.commentElementClass = classes;
            setCommentClasses(classes);
        }
    }

    private String tooltip;
    public void setTooltip(String tooltip) {
        if (!GwtSharedUtils.nullEquals(this.tooltip, tooltip)) {
            this.tooltip = tooltip;
            Element label = getTooltipWidget().getElement();
            TooltipManager.updateContent(tippy, tooltipHelper, tooltip);
        }
    }

    protected Widget getTooltipWidget() {
        return getComponent();
    }

    protected abstract void setLabelText(String text);
    protected abstract void setLabelClasses(String classes);
    protected abstract void setCommentText(String text);
    protected abstract void setCommentClasses(String classes);

    public void onBinding(Event event) {
        value.onBinding(event);
    }

    public void focus(FocusUtils.Reason reason) {
        value.focus(reason);
    }

    public PValue setLoadingValue(PValue value) {
        return this.value.setLoadingValue(value);
    }
}
