package lsfusion.gwt.client.form.property.panel.view;

import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.GwtSharedUtils;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.object.panel.controller.GPropertyPanelController;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.cell.classes.view.ActionCellRenderer;

import static lsfusion.gwt.client.base.GwtClientUtils.stopPropagation;

public class ActionPanelRenderer extends PanelRenderer {

//    private final GFormController form;

    public ActionPanelRenderer(final GFormController form, ActionOrPropertyValueController controller, final GPropertyDraw property, GGroupObjectValue columnKey) {
        super(form, controller, property, columnKey);

        value.setDynamic(false);

        finalizeInit();
    }

    @Override
    public Widget getComponent() {
        return value;
    }

    @Override
    protected Widget getTooltipWidget() {
        return value;
    }

    // hack, assert that render element is rendered with ActionCellRenderer
    @Override
    protected void setLabelText(String text) {
        ActionCellRenderer.setLabelText(value.getRenderElement(), text);
    }

    private Object image;
    public void setDynamicImage(Object image) {
        if (!GwtSharedUtils.nullEquals(this.image, image)) {
            this.image = image;

            assert property.hasDynamicImage();
            GFormController.setDynamicImage(value.getRenderElement(), image);
        }
    }

    // interface for refresh button
    private String nullImage = null;
    public void setLoadingImage(String iconPath) {
        Element renderElement = value.getRenderElement();
        if(iconPath == null) {
            if(nullImage != null) {
                ActionCellRenderer.setImage(renderElement, nullImage, null, false);
                nullImage = null;
            }
        } else
            GwtClientUtils.setThemeImage(iconPath, imageUrl -> ActionCellRenderer.setImage(renderElement, imageUrl, nullImage == null ? s -> { nullImage = s; } : null, false));
    }
}
