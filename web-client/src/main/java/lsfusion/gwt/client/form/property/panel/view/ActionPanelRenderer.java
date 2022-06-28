package lsfusion.gwt.client.form.property.panel.view;

import com.google.gwt.dom.client.Element;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.view.SizedWidget;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.cell.classes.view.ActionCellRenderer;

import static lsfusion.gwt.client.form.property.cell.classes.view.ActionCellRenderer.ASYNCIMAGE;

public class ActionPanelRenderer extends PanelRenderer {

//    private final GFormController form;

    private final SizedWidget sizedView;

    public ActionPanelRenderer(final GFormController form, ActionOrPropertyValueController controller, final GPropertyDraw property, GGroupObjectValue columnKey) {
        super(form, controller, property, columnKey);

        sizedView = value.setSized();

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

    // interface for refresh button
    public void setLoadingImage(String iconPath) {
        Element renderElement = value.getRenderElement();
        if(iconPath == null) {
            ActionCellRenderer.setImage(renderElement, renderElement.getPropertyString(ASYNCIMAGE), false);
        } else
            GwtClientUtils.setThemeImage(iconPath, imageUrl -> ActionCellRenderer.setImage(renderElement, imageUrl, false));
    }
}
