package lsfusion.gwt.client.form.property.panel.view;

import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.GwtSharedUtils;
import lsfusion.gwt.client.base.Pair;
import lsfusion.gwt.client.base.view.FlexPanel;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.object.panel.controller.GPropertyPanelController;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.cell.classes.view.ActionCellRenderer;

import static lsfusion.gwt.client.base.GwtClientUtils.stopPropagation;

public class ActionPanelRenderer extends PanelRenderer {

//    private final GFormController form;

    private Label label;
    public ActionPanelRenderer(final GFormController form, ActionOrPropertyValueController controller, final GPropertyDraw property, GGroupObjectValue columnKey, GPropertyPanelController.CaptionContainer captionContainer) {
        super(form, controller, property, columnKey, captionContainer);

        // we don't need to wrap value in any container (which is important for LinearContainerView since it can override set baseSizes)
        // because any panel renderer is wrapped in renderersPanel (see getComponent usage)
        Pair<Integer, Integer> valueSizes = value.setDynamic(false);
        assert !property.isAutoDynamicHeight();
        if(captionContainer != null) {
            // creating virtual value component with the same size as value and return it as a value
            label = new Label();

            boolean vertical = true;
            Integer baseSize = vertical ? valueSizes.second : valueSizes.first;
            FlexPanel.setBaseSize(label, vertical, baseSize);  // oppositeAndFixed - false, since we're setting the size for the main direction

            captionContainer.put(value, valueSizes, property.getPanelCaptionAlignment());
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
