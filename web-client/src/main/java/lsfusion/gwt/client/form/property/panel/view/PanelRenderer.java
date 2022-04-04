package lsfusion.gwt.client.form.property.panel.view;

import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.base.GwtSharedUtils;
import lsfusion.gwt.client.base.TooltipManager;
import lsfusion.gwt.client.base.view.SizedWidget;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.design.view.flex.LinearCaptionContainer;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.object.table.view.GGridPropertyTable;
import lsfusion.gwt.client.form.property.GPropertyDraw;

import java.util.ArrayList;
import java.util.Optional;

import static lsfusion.gwt.client.base.GwtSharedUtils.nullEquals;

public abstract class PanelRenderer {

    protected final GFormController form;

    protected final GPropertyDraw property;
    protected final GGroupObjectValue columnKey;

    protected final ActionOrPropertyPanelValue value;

    public ArrayList<Integer> bindingEventIndices;

    public PanelRenderer(GFormController form, ActionOrPropertyValueController controller, GPropertyDraw property, GGroupObjectValue columnKey, LinearCaptionContainer captionContainer) {
        this.form = form;
        this.property = property;
        this.columnKey = columnKey;

        value = new ActionOrPropertyPanelValue(property, columnKey, form, false, controller); // captionContainer != null, now we use different approach when button is renderered as caption
    }

    protected void finalizeInit() {
        setCaption(GGridPropertyTable.getPropertyCaption(property));

        Widget label = getTooltipWidget();
        TooltipManager.registerWidget(label, new TooltipManager.TooltipHelper() {
            public String getTooltip() {
                if(value.isEditing)
                    return null;
                return tooltip;
            }

            public boolean stillShowTooltip() {
                return label.isAttached() && label.isVisible();
            }

            public String getPath() {
                return property.path;
            }

            public String getCreationPath() {
                return property.creationPath;
            }
        });
        if (this.property.captionFont != null) {
            this.property.captionFont.apply(label.getElement().getStyle());
        }
    }

    public abstract SizedWidget getSizedWidget();
    public Widget getComponent() {
        return getSizedWidget().widget;
    }

    public void update(Object value, boolean loading, Object image, Object background, Object foreground, boolean readOnly) {
        this.value.update(value, loading, image, background, foreground, readOnly);
    }

    private String caption;
    private String tooltip;
    public void setCaption(String caption) {
        if (!GwtSharedUtils.nullEquals(this.caption, caption)) {
            this.caption = caption;
            setLabelText(property.getPanelCaption(caption));

            tooltip = property.getTooltipText(caption);
        }
    }

    protected Widget getTooltipWidget() {
        return getComponent();
    }

    protected abstract void setLabelText(String text);

    public void onBinding(Event event) {
        value.onBinding(event);
    }

    public void focus() {
        value.setFocus(true);
    }

    public Object setLoadingValue(Object value) {
        Object oldValue = this.value.getValue();
        this.value.setLoadingValue(value);
        return oldValue;
    }
}
