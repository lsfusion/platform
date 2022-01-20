package lsfusion.gwt.client.form.property.panel.view;

import com.google.gwt.dom.client.Element;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.GwtSharedUtils;
import lsfusion.gwt.client.base.view.SizedWidget;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.design.view.flex.LinearCaptionContainer;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.cell.classes.view.ActionCellRenderer;

import static lsfusion.gwt.client.form.property.cell.classes.view.ActionCellRenderer.ASYNCIMAGE;

public class ActionPanelRenderer extends PanelRenderer {

//    private final GFormController form;

    private final SizedWidget sizedView;

    public ActionPanelRenderer(final GFormController form, ActionOrPropertyValueController controller, final GPropertyDraw property, GGroupObjectValue columnKey, LinearCaptionContainer captionContainer) {
        super(form, controller, property, columnKey, captionContainer);

        sizedView = value.setSized(true, false, null);

        finalizeInit();
    }

    @Override
    public SizedWidget getSizedWidget() {
        return sizedView;
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
    public void setLoadingImage(String iconPath) {
        Element renderElement = value.getRenderElement();
        if(iconPath == null) {
            ActionCellRenderer.setImage(renderElement, renderElement.getPropertyString(ASYNCIMAGE), false);
        } else
            GwtClientUtils.setThemeImage(iconPath, imageUrl -> ActionCellRenderer.setImage(renderElement, imageUrl, false));
    }
}
