package lsfusion.gwt.client.form.property.panel.view;

import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.base.GwtSharedUtils;
import lsfusion.gwt.client.base.TooltipManager;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.event.GInputEvent;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.property.GPropertyDraw;

import static lsfusion.gwt.client.base.GwtSharedUtils.nullEquals;

public abstract class PanelRenderer {

    protected final GFormController form;

    protected final GPropertyDraw property;
    protected final GGroupObjectValue columnKey;

    protected final ActionOrPropertyPanelValue value;

    private String caption;
    private String tooltip;

    public PanelRenderer(GFormController form, GPropertyDraw property, GGroupObjectValue columnKey, String styleName) {
        this.form = form;
        this.property = property;
        this.columnKey = columnKey;

        value = new ActionOrPropertyPanelValue(property, columnKey, form);
        value.addStyleName(styleName);
    }

    protected void finalizeInit() {
        setCaption(property.getCaptionOrEmpty());

        Widget label = getTooltipWidget();
        TooltipManager.registerWidget(label, new TooltipManager.TooltipHelper() {
            public String getTooltip() {
                return tooltip;
            }

            public boolean stillShowTooltip() {
                return label.isAttached() && label.isVisible();
            }
        });
        if (this.property.captionFont != null) {
            this.property.captionFont.apply(label.getElement().getStyle());
        }
    }

    public abstract Widget getComponent();

    public void updateValue(Object value) {
        this.value.updateValue(value);
    }

    public void setReadOnly(boolean readOnly) {
        value.setReadOnly(readOnly);
    }

    public void setCaption(String caption) {
        if (!GwtSharedUtils.nullEquals(this.caption, caption)) {
            this.caption = caption;
            tooltip = property.getTooltipText(caption);

            setLabelText(property.getEditCaption(caption));
        }
    }

    protected abstract Widget getTooltipWidget();

    protected abstract void setLabelText(String text);

    private Object background;
    public void updateCellBackgroundValue(Object value) {
        if (!nullEquals(background, value)) {
            background = value;

            this.value.setBackground(value != null ? value.toString() : null);
        }
    }

    public void onBinding(GInputEvent bindingEvent, Event event) {
        value.onBinding(bindingEvent, event);
    }

    private Object foreground;
    public void updateCellForegroundValue(Object value) {
        if (!nullEquals(foreground, value)) {
            foreground = value;

            this.value.setForeground(value != null ? value.toString() : null);
        }
    }

    public void focus() {
        value.setFocus(true);
    }
}
