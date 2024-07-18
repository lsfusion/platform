package lsfusion.gwt.client.form.property.panel.view;

import lsfusion.gwt.client.base.BaseImage;
import lsfusion.gwt.client.base.view.SizedWidget;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.design.view.ComponentViewWidget;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.property.GPropertyDraw;

public class ActionPanelRenderer extends PanelRenderer {

    private final SizedWidget sizedView;

    public ActionPanelRenderer(final GFormController form, ActionOrPropertyValueController controller, final GPropertyDraw property, GGroupObjectValue columnKey) {
        super(form, controller, property, columnKey, false);

        sizedView = value.getSizedWidget(false);

        if (property.drawAsync)
            form.setAsyncView(this);

        finalizeInit();
    }

    @Override
    public ComponentViewWidget getComponentViewWidget() {
        return sizedView.view;
    }

    // hack, assert that render element is rendered with ActionCellRenderer
    @Override
    protected void setLabelText(String text) {
        BaseImage.updateText(value.getRenderElement(), text);
    }

    @Override
    public void stopEditing() { // after editing someone should restore the text (because caption is rendered inside the element)
        updateCaption();
    }

    @Override
    protected void setLabelClasses(String classes) {
//        BaseImage.updateClasses(value.getRenderElement());
    }

    @Override
    protected void setCommentText(String text) {
    }

    @Override
    protected void setCommentClasses(String classes) {
    }

    // interface for refresh button
    public void setForceLoading(boolean set) {
        value.setForceLoading(set);
    }
}
