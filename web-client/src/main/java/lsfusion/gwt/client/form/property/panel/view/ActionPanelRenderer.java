package lsfusion.gwt.client.form.property.panel.view;

import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.GwtSharedUtils;
import lsfusion.gwt.client.base.Pair;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.design.view.flex.LinearCaptionContainer;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.cell.classes.view.ActionCellRenderer;

import static lsfusion.gwt.client.form.property.cell.classes.view.ActionCellRenderer.ASYNCIMAGE;

public class ActionPanelRenderer extends PanelRenderer {

//    private final GFormController form;

    private Label label;
    public ActionPanelRenderer(final GFormController form, ActionOrPropertyValueController controller, final GPropertyDraw property, GGroupObjectValue columnKey, LinearCaptionContainer captionContainer) {
        super(form, controller, property, columnKey, captionContainer);

        // we don't need to wrap value in any container (which is important for LinearContainerView since it can override set baseSizes)
        // because any panel renderer is wrapped in renderersPanel (see getComponent usage)
        // however addFill there will override value sizes set in setDynamic, but it doesn't matter now
        Pair<Integer, Integer> valueSizes = value.setDynamic(false);
        assert !property.isAutoDynamicHeight();
        if(captionContainer != null) {
            // creating virtual value component with the same size as value and return it as a value
            label = new Label();
            captionContainer.put(value, label, valueSizes, property.getPanelCaptionAlignment());
        }

        finalizeInit();
    }

    @Override
    public Widget getComponent() {
        return label != null ? label : value;
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
    public void setLoadingImage(String iconPath) {
        Element renderElement = value.getRenderElement();
        if(iconPath == null) {
            ActionCellRenderer.setImage(renderElement, renderElement.getPropertyString(ASYNCIMAGE), false);
        } else
            GwtClientUtils.setThemeImage(iconPath, imageUrl -> ActionCellRenderer.setImage(renderElement, imageUrl, false));
    }
}
